package com.car.convoy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.car.convoy.data.AccidentController
import com.car.convoy.data.FriendController
import com.car.convoy.data.GroupController
import com.car.convoy.data.MainThreadScheduler
import com.car.convoy.data.navi.NaviHelperAdapter
import com.car.convoy.data.vehicle.VehicleSource
import com.car.convoy.domain.Destination
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.FollowEvent
import com.car.convoy.domain.Friend
import com.car.convoy.domain.TelltaleReading
import com.car.convoy.domain.TelltaleState
import com.car.convoy.domain.TelltaleType
import com.car.convoy.domain.UserStatus
import com.car.convoy.ui.AccidentPrompt
import com.car.convoy.ui.AccidentScreenModel
import com.car.convoy.ui.AppTab
import com.car.convoy.ui.AppTabs
import com.car.convoy.ui.FamilyChip
import com.car.convoy.ui.FriendScreen
import com.car.convoy.ui.FriendScreenModel
import com.car.convoy.ui.friendScreenModel
import com.car.convoy.ui.GroupScreen
import com.car.convoy.ui.SentFamilyCard
import com.car.convoy.ui.accidentScreenModel
import com.car.convoy.ui.GroupScreenModel
import com.car.convoy.ui.PRESET_DESTINATIONS
import com.car.convoy.ui.groupScreenModel

private const val TAG = "Convoy"
private const val ME = "me"
private val MEMBERS = listOf(ME, "m1", "m2")
private val NAMES = mapOf(ME to "나", "m1" to "일행 1", "m2" to "일행 2")

// 작업 22(R2-5): 외부 서버가 없어 앱에 미리 정해 둔 가상 친구. 서준(오프라인)은 목록에 나오지 않는다.
private val FRIENDS = listOf(
    Friend("f1", "민수", UserStatus.ACTIVE),
    Friend("f2", "지영", UserStatus.BUSY),
    Friend("f3", "현우", UserStatus.ACTIVE),
    Friend("f4", "서준", UserStatus.OFFLINE),
)

/**
 * 작업 15 그룹 주행 화면(R11-20). 나는 리더이고 실제 SDK를 쓰며, 일행 1·2는 가상 멤버다(R11-18).
 * adb 시연 명령도 그대로 받는다(녹화·자동 확인용).
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo dest:서울역
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo depart:m1
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo arrive:m1
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo retry
 * 작업 20 사고 의심(R8): 경고등은 Virtual Console에서 켜는 것이 기본이고, 콘솔을 쓸 수 없을 때 시연 명령으로 넣는다.
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo airbag:on   (airbag:off)
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo ok          ([괜찮아요])
 *   adb shell am start -n com.car.convoy/.MainActivity --es demo airbaglater (8초 뒤 켜기, R8-11 확인용)
 * 작업 22 친구 목록(R2-5):
 *   --es demo tab:friends (tab:group)   --es demo follow:f1   --es demo accept (decline)   --es demo cancel
 *   --es demo status:busy (active, offline)
 */
class MainActivity : ComponentActivity() {

    private val scheduler = MainThreadScheduler()
    private val model = mutableStateOf<GroupScreenModel?>(null)
    private val accidentModel = mutableStateOf<AccidentScreenModel?>(null)
    private val friendModel = mutableStateOf<FriendScreenModel?>(null)
    private val tab = mutableStateOf(AppTab.GROUP)
    private val friends = FriendController(FRIENDS)

    private lateinit var navi: NaviHelperAdapter
    private lateinit var vehicle: VehicleSource
    private lateinit var controller: GroupController
    private lateinit var accident: AccidentController
    private var wasChecking = false
    private var countdown: com.car.convoy.data.Cancellable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navi = NaviHelperAdapter(
            applicationContext,
            routeStarted = { name -> scheduler.post { controller.onRouteStarted(name) } },
            destinationArrived = { n, a -> scheduler.post { controller.onDestinationArrived(n, a) } },
            // requestRoute 예외는 호출 중 동기로 오므로 큐에 넣지 않고 바로 알린다(R11-19). 화면 갱신만 나중에 한다.
            routeFailed = { controller.onMyRouteFailed(); scheduler.post { refresh() } },
            locationReceived = { loc -> scheduler.post { accident.onLocation(loc) } },
            log = ::log,
        )
        controller = GroupController(ME, MEMBERS, navi, scheduler) { u ->
            log("${u.memberId} ${u.event} → ${controller.session.memberState(u.memberId)} " +
                "효과=${u.result.memberEffect} 그룹=${u.result.groupEffect}")
            refresh()
        }
        // R8-9: 시연에서는 가족 1명이 이미 등록된 상태로 시작한다(시연 설정).
        accident = AccidentController(controller.server, navi, scheduler, FamilyState.ACTIVE, ::onAccidentChange)
        vehicle = VehicleSource(
            applicationContext,
            onGear = { g -> scheduler.post { controller.onGearChanged(g); refresh() } },
            onTelltales = { r -> scheduler.post { accident.onTelltales(r) } },
            log = ::log,
        )

        setContent {
            MaterialTheme {
                val accidentUi = accidentModel.value
                Box {
                    val tabs: @Composable () -> Unit = { AppTabs(tab.value) { tab.value = it } }
                    when (tab.value) {
                        AppTab.GROUP -> model.value?.let {
                            GroupScreen(
                                model = it,
                                onChooseDestination = ::chooseDestination,
                                onRetry = { controller.retryMyRoute(); refresh() },
                                onDemoDepart = { id -> controller.server.depart(id); refresh() },
                                onDemoArrive = { id -> controller.server.arrive(id); refresh() },
                                headerExtra = { accidentUi?.let { a -> FamilyChip(a.familyLabel) } },
                                belowMembers = { accidentUi?.sentCard?.let { c -> SentFamilyCard(c) } },
                                tabs = tabs,
                            )
                        }
                        AppTab.FRIENDS -> friendModel.value?.let {
                            FriendScreen(
                                model = it,
                                onMyStatus = { s -> friends.setMyStatus(s); refresh() },
                                onRequestFollow = ::requestFollow,
                                onCancelFollow = { friends.cancelFollow(); refresh() },
                                onDemoAccept = { friends.respond(FollowEvent.ACCEPT); refresh() },
                                onDemoDecline = { friends.respond(FollowEvent.DECLINE); refresh() },
                                headerExtra = { accidentUi?.let { a -> FamilyChip(a.familyLabel) } },
                                tabs = tabs,
                            )
                        }
                    }
                    // 탭은 각 화면 머리줄에 놓는다(R2-5). 작업 21: 사고 확인 중이면 어느 탭 위에도 "괜찮으세요?"를 덮는다(R2-5).
                    if (accidentUi?.showPrompt == true) AccidentPrompt(accidentUi) { accident.respondOk() }
                }
            }
        }

        refresh()
        runCatching { vehicle.start() }.onFailure { log("Vehicle 시작 실패: $it") }
        // 초기화 성공 콜백의 스레드는 보장되지 않으므로 메인 스레드로 옮긴다(GroupController는 한 스레드에서만 부른다).
        navi.start { scheduler.post { handleDemo(intent) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDemo(intent)
    }

    override fun onDestroy() {
        vehicle.stop()
        navi.stop()
        super.onDestroy()
    }

    private fun chooseDestination(destination: Destination) {
        log("목적지 선택 ${destination.name}")
        controller.setDestination(destination)
        refresh()
    }

    private fun requestFollow(friendId: String) {
        val sent = friends.requestFollow(friendId, controller.currentGear)
        log("따라가기 요청 $friendId ${if (sent) "보냄" else "조건 안 맞아 무시"}")
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
                "airbag" -> accident.onTelltales(
                    listOf(TelltaleReading(TelltaleType.AIRBAG, if (arg == "on") TelltaleState.ON else TelltaleState.OFF)),
                )
                "ok" -> accident.respondOk()
                // R8-11 확인용: 8초 뒤 경고등을 넣는다(그사이 내비를 앞으로 띄워 둔다).
                "airbaglater" -> scheduler.schedule(8_000) {
                    accident.onTelltales(listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON)))
                }
                "tab" -> tab.value = if (arg == "friends") AppTab.FRIENDS else AppTab.GROUP
                "follow" -> requestFollow(arg!!)
                "accept" -> friends.respond(FollowEvent.ACCEPT)
                "decline" -> friends.respond(FollowEvent.DECLINE)
                "cancel" -> friends.cancelFollow()
                "status" -> friends.setMyStatus(UserStatus.valueOf(arg!!.uppercase()))
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
        friendModel.value = friendScreenModel(FRIENDS, friends.myStatus, friends.follow, controller.currentGear)
        accidentModel.value = accidentScreenModel(
            checking = accident.checking,
            remainingSeconds = accident.remainingSeconds,
            family = accident.familyState,
            lastSentReport = accident.lastSentReport,
            zone = java.time.ZoneId.systemDefault(),
        )
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    /** 작업 20: 사고 확인 상태가 바뀔 때. R8-11: 확인이 시작되면 Convoy 화면을 앞으로 가져온다. */
    private fun onAccidentChange() {
        if (accident.checking && !wasChecking) {
            log("사고 의심: 괜찮으세요? (${accident.remainingSeconds}초)")
            runCatching {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                )
            }.onFailure { log("화면 앞으로 가져오기 실패: $it") }
        }
        if (!accident.checking && wasChecking) {
            log(accident.lastSentReport?.let { "가족에게 보냄: ${it.telltales} 위치=${it.location ?: "위치 확인 불가"}" } ?: "확인 끝: 보내지 않음")
        }
        if (accident.checking && !wasChecking) tickCountdown()
        if (!accident.checking) {
            // 확인이 끝나면 카운트다운 예약을 취소한다(곧바로 새 확인이 시작돼도 루프가 겹치지 않게).
            countdown?.cancel()
            countdown = null
        }
        wasChecking = accident.checking
        refresh()
    }

    /** 작업 21: 확인 중에는 남은 초를 보여 주려고 0.5초마다 화면을 갱신한다. */
    private fun tickCountdown() {
        countdown?.cancel()
        if (!accident.checking) return
        refresh()
        countdown = scheduler.schedule(500) { tickCountdown() }
    }
}
