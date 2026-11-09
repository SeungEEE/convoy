package com.car.convoy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.convoy.data.GroupController
import com.car.convoy.data.MainThreadScheduler
import com.car.convoy.data.navi.NaviHelperAdapter
import com.car.convoy.data.vehicle.VehicleGearSource
import com.car.convoy.domain.Destination

private const val TAG = "Convoy"
private const val ME = "me"
private val MEMBERS = listOf(ME, "m1", "m2")

/** 시연·확인용 목적지. */
private val DESTINATIONS = mapOf(
    "seoul" to Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707),
    "gangnam" to Destination("강남역", "서울 강남구 강남대로 396", 37.4979, 127.0276),
)

/**
 * 작업 14 확인용 화면. 그룹 상태를 보여 주고, adb로 시연 명령을 받는다(화면은 작업 15에서 만든다).
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo dest:seoul
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo depart:m1
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo arrive:m1
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo retry
 */
class MainActivity : ComponentActivity() {

    private val scheduler = MainThreadScheduler()
    private val states = mutableStateOf("")
    private val logs = mutableStateListOf<String>()

    private lateinit var navi: NaviHelperAdapter
    private lateinit var gear: VehicleGearSource
    private lateinit var controller: GroupController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navi = NaviHelperAdapter(
            applicationContext,
            routeStarted = { name -> scheduler.post { controller.onRouteStarted(name) } },
            destinationArrived = { n, a -> scheduler.post { controller.onDestinationArrived(n, a) } },
            // requestRoute 예외는 호출 중 동기로 오므로 큐에 넣지 않고 바로 알린다(R11-19). 화면 갱신만 나중에 한다.
            routeFailed = { controller.onMyRouteFailed(); scheduler.post { refresh() } },
            log = ::log,
        )
        controller = GroupController(ME, MEMBERS, navi, scheduler) { u ->
            log("${u.memberId} ${u.event} → ${controller.session.memberState(u.memberId)} " +
                "효과=${u.result.memberEffect} 그룹=${u.result.groupEffect}")
            refresh()
        }
        gear = VehicleGearSource(
            applicationContext,
            onGear = { g -> scheduler.post { controller.onGearChanged(g) } },
            log = ::log,
        )

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Convoy 작업 14 확인", fontSize = 36.sp)
                    Text(states.value, fontSize = 28.sp)
                    logs.takeLast(12).forEach { Text(it, fontSize = 20.sp) }
                }
            }
        }

        refresh()
        runCatching { gear.start() }.onFailure { log("Vehicle 시작 실패: $it") }
        // 초기화 성공 콜백의 스레드는 보장되지 않으므로 메인 스레드로 옮긴다(GroupController는 한 스레드에서만 부른다).
        navi.start { scheduler.post { handleDemo(intent) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDemo(intent)
    }

    override fun onDestroy() {
        gear.stop()
        navi.stop()
        super.onDestroy()
    }

    private fun handleDemo(intent: Intent?) {
        val demo = intent?.getStringExtra("demo") ?: return
        intent.removeExtra("demo")
        log("== demo $demo")
        val (command, arg) = demo.split(":", limit = 2).let { it[0] to it.getOrNull(1) }
        runCatching {
            when (command) {
                "dest" -> controller.setDestination(DESTINATIONS.getValue(arg!!))
                "depart" -> controller.server.depart(arg!!)
                "arrive" -> controller.server.arrive(arg!!)
                "retry" -> controller.retryMyRoute()
                else -> log("모르는 명령: $demo")
            }
        }.onFailure { log("명령 실패: $it") }
        refresh()
    }

    private fun refresh() {
        val s = controller.session
        states.value = "그룹 ${s.status} · 목적지 ${s.destination?.name ?: "없음"}\n" +
            MEMBERS.joinToString("   ") { "$it=${s.memberState(it)}" } +
            when {
                controller.canRetryMyRoute -> "\n내 차 경로 요청 실패 · retry로 다시 요청"
                controller.myRouteFailed -> "\n내 차 경로 요청 실패"
                else -> ""
            }
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        scheduler.post { logs += message }
    }
}
