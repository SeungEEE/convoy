package com.car.convoy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import com.car.convoy.data.GroupController
import com.car.convoy.data.MainThreadScheduler
import com.car.convoy.data.navi.NaviHelperAdapter
import com.car.convoy.data.vehicle.VehicleGearSource
import com.car.convoy.domain.Destination
import com.car.convoy.ui.GroupScreen
import com.car.convoy.ui.GroupScreenModel
import com.car.convoy.ui.PRESET_DESTINATIONS
import com.car.convoy.ui.groupScreenModel

private const val TAG = "Convoy"
private const val ME = "me"
private val MEMBERS = listOf(ME, "m1", "m2")
private val NAMES = mapOf(ME to "나 (리더)", "m1" to "일행 1", "m2" to "일행 2")

/**
 * 작업 15 그룹 주행 화면(R11-20). 나는 리더이고 실제 SDK를 쓰며, 일행 1·2는 가상 멤버다(R11-18).
 * adb 시연 명령도 그대로 받는다(녹화·자동 확인용).
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo dest:서울역
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo depart:m1
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo arrive:m1
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo retry
 */
class MainActivity : ComponentActivity() {

    private val scheduler = MainThreadScheduler()
    private val model = mutableStateOf<GroupScreenModel?>(null)

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
            onGear = { g -> scheduler.post { controller.onGearChanged(g); refresh() } },
            log = ::log,
        )

        setContent {
            MaterialTheme {
                model.value?.let {
                    GroupScreen(
                        model = it,
                        onChooseDestination = ::chooseDestination,
                        onRetry = { controller.retryMyRoute(); refresh() },
                        onDemoDepart = { id -> controller.server.depart(id); refresh() },
                        onDemoArrive = { id -> controller.server.arrive(id); refresh() },
                    )
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

    private fun chooseDestination(destination: Destination) {
        log("목적지 선택 ${destination.name}")
        controller.setDestination(destination)
        refresh()
    }

    private fun handleDemo(intent: Intent?) {
        val demo = intent?.getStringExtra("demo") ?: return
        intent.removeExtra("demo")
        log("== demo $demo")
        val (command, arg) = demo.split(":", limit = 2).let { it[0] to it.getOrNull(1) }
        runCatching {
            when (command) {
                "dest" -> chooseDestination(PRESET_DESTINATIONS.first { it.name == arg })
                "depart" -> controller.server.depart(arg!!)
                "arrive" -> controller.server.arrive(arg!!)
                "retry" -> controller.retryMyRoute()
                else -> log("모르는 명령: $demo")
            }
        }.onFailure { log("명령 실패: $it") }
        refresh()
    }

    private fun refresh() {
        model.value = groupScreenModel(
            session = controller.session,
            myId = ME,
            memberIds = MEMBERS,
            names = NAMES,
            isLeader = true,
            gear = controller.currentGear,
            canRetryMyRoute = controller.canRetryMyRoute,
        )
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }
}
