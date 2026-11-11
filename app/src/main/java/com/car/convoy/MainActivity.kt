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
import com.car.convoy.data.Cancellable
import com.car.convoy.data.FriendController
import com.car.convoy.data.GroupController
import com.car.convoy.data.GroupRoster
import com.car.convoy.data.MainThreadScheduler
import com.car.convoy.data.navi.NaviHelperAdapter
import com.car.convoy.data.vehicle.VehicleSource
import com.car.convoy.domain.Destination
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.TelltaleReading
import com.car.convoy.domain.TelltaleState
import com.car.convoy.domain.TelltaleType
import com.car.convoy.domain.UserStatus
import com.car.convoy.domain.port.ConvoyServer
import com.car.convoy.ui.AccidentPrompt
import com.car.convoy.ui.AccidentScreenModel
import com.car.convoy.ui.DriveNavButtons
import com.car.convoy.ui.FamilyChip
import com.car.convoy.ui.GroupScreen
import com.car.convoy.ui.GroupScreenModel
import com.car.convoy.ui.HomeScreen
import com.car.convoy.ui.HomeScreenModel
import com.car.convoy.ui.MY_NAME
import com.car.convoy.ui.PRESET_DESTINATIONS
import com.car.convoy.ui.SentFamilyCard
import com.car.convoy.ui.accidentScreenModel
import com.car.convoy.ui.groupScreenModel
import com.car.convoy.ui.homeScreenModel

private const val TAG = "Convoy"
private const val ME = "me"

// 작업 23(R4-5): 외부 서버가 없어 앱에 미리 정해 둔 가상 인물. 서준(오프라인)은 보이지 않는다.
private val PEOPLE = listOf(
    Friend("p1", "민수", UserStatus.ACTIVE),
    Friend("p2", "현우", UserStatus.ACTIVE),
    Friend("p3", "수진", UserStatus.ACTIVE),
    Friend("p4", "지영", UserStatus.BUSY),
    Friend("p5", "서준", UserStatus.OFFLINE),
)

private enum class Screen { HOME, DRIVE }

/**
 * 작업 23 홈(R2-5, R4-5)과 그룹 주행(R11-20). 나는 리더이고 실제 SDK를 쓰며, 그룹 멤버는 가상 인물이다(R11-18).
 * adb 시연 명령(녹화·자동 확인용): adb shell am start -n com.car.convoy/.MainActivity --es demo <명령>
 *   홈: invite:p1, accept, decline(첫 수락 대기에 응답), start(목적지로 함께), disband, status:busy
 *   주행: dest:서울역, depart:p1, arrive:p1, retry, home, end, resume
 *   사고(R8): airbag:on|off, ok, airbaglater(8초 뒤 켜기, R8-11 확인용)
 */
class MainActivity : ComponentActivity() {

    private val scheduler = MainThreadScheduler()
    private val screen = mutableStateOf(Screen.HOME)
    private val homeModel = mutableStateOf<HomeScreenModel?>(null)
    private val groupModel = mutableStateOf<GroupScreenModel?>(null)
    private val accidentModel = mutableStateOf<AccidentScreenModel?>(null)

    private val roster = GroupRoster(PEOPLE)
    private val me = FriendController(PEOPLE) // 내 상태(R2-1). 따라가기는 작업 25에서 쓴다.
    private val names = PEOPLE.associate { it.id to it.name } + (ME to MY_NAME)

    /** SDK가 기어를 다시 보내 주지 않으므로 여기서 들고, 주행마다 새 컨트롤러에 넘긴다. */
    private var gear = Gear.UNKNOWN

    /** 진행 중인 "목적지로 함께". 없으면 주행이 "없음"(R4-5). */
    private var drive: GroupController? = null
    private var driveMembers: List<String> = emptyList()

    private lateinit var navi: NaviHelperAdapter
    private lateinit var vehicle: VehicleSource
    private lateinit var accident: AccidentController
    private var wasChecking = false
    private var countdown: Cancellable? = null

    // 가족 알림은 그룹 주행과 무관하다(R9-1). 받는 차가 없어 보낸 내용은 화면 카드로만 보여 준다(R8-10).
    private val familyServer = object : ConvoyServer {
        override fun sendDestination(memberId: String, destination: Destination) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navi = NaviHelperAdapter(
            applicationContext,
            routeStarted = { name -> scheduler.post { drive?.onRouteStarted(name) } },
            destinationArrived = { n, a -> scheduler.post { drive?.onDestinationArrived(n, a) } },
            // requestRoute 예외는 호출 중 동기로 오므로 큐에 넣지 않고 바로 알린다(R11-19). 화면 갱신만 나중에 한다.
            routeFailed = { drive?.onMyRouteFailed(); scheduler.post { refresh() } },
            locationReceived = { loc -> scheduler.post { accident.onLocation(loc) } },
            log = ::log,
        )
        // R8-9의 가족 상태는 작업 24에서 "가족 없이 시작"으로 바뀐다.
        accident = AccidentController(familyServer, navi, scheduler, FamilyState.ACTIVE, ::onAccidentChange)
        vehicle = VehicleSource(
            applicationContext,
            onGear = { g -> scheduler.post { gear = g; drive?.onGearChanged(g); refresh() } },
            onTelltales = { r -> scheduler.post { accident.onTelltales(r) } },
            log = ::log,
        )

        setContent {
            MaterialTheme {
                val accidentUi = accidentModel.value
                val familyChip: @Composable () -> Unit = { accidentUi?.let { a -> FamilyChip(a.familyLabel) } }
                Box {
                    val group = groupModel.value
                    if (screen.value == Screen.DRIVE && group != null) {
                        GroupScreen(
                            model = group,
                            onChooseDestination = ::chooseDestination,
                            onRetry = { drive?.retryMyRoute(); refresh() },
                            onDemoDepart = { id -> drive?.server?.depart(id); refresh() },
                            onDemoArrive = { id -> drive?.server?.arrive(id); refresh() },
                            headerExtra = familyChip,
                            belowMembers = { accidentUi?.sentCard?.let { c -> SentFamilyCard(c) } },
                            nav = { DriveNavButtons(onHome = ::goHome, onEnd = ::endDrive) },
                        )
                    } else {
                        homeModel.value?.let {
                            HomeScreen(
                                model = it,
                                onMyStatus = { s -> me.setMyStatus(s); refresh() },
                                onInvite = ::invite,
                                onStartDestination = ::startDestination,
                                onResume = ::resume,
                                onDisband = ::disband,
                                onDemoAccept = { respondInvite(true) },
                                onDemoDecline = { respondInvite(false) },
                                headerExtra = familyChip,
                            )
                        }
                    }
                    // R2-5: 사고 확인 중이면 어느 화면 위에도 "괜찮으세요?"를 덮는다(작업 21).
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

    // ---- 홈(R4-5) ----

    private fun invite(id: String) {
        val sent = roster.invite(id, gear, driveActive = drive != null)
        log("그룹 초대 $id ${if (sent) "보냄" else "조건 안 맞아 무시"}")
        refresh()
    }

    /** 시연용 패널: 첫 수락 대기의 응답(R4-4). */
    private fun respondInvite(accept: Boolean) {
        roster.firstPendingInvite?.let { id ->
            roster.respond(id, accept)
            log("초대 응답 $id ${if (accept) "수락" else "거절"}")
        }
        refresh()
    }

    private fun disband() {
        val done = roster.disband(gear, driveActive = drive != null)
        log("그룹 해산 ${if (done) "됨" else "조건 안 맞아 무시"}")
        refresh()
    }

    /** R4-5 [목적지로 함께]: 그 시점의 그룹 멤버로 새 세션을 만든다(R11-22). */
    private fun startDestination() {
        val model = homeModel.value ?: return
        if (!model.canStartDestination) return
        driveMembers = listOf(ME) + roster.members
        drive = GroupController(ME, driveMembers, navi, scheduler, initialGear = gear) { u ->
            log("${u.memberId} ${u.event} → ${drive?.session?.memberState(u.memberId)} " +
                "효과=${u.result.memberEffect} 그룹=${u.result.groupEffect}")
            refresh()
        }
        log("목적지로 함께 시작: $driveMembers")
        screen.value = Screen.DRIVE
        refresh()
    }

    private fun resume() {
        if (drive != null) screen.value = Screen.DRIVE
        refresh()
    }

    /** R11-20 [홈으로]: 주행은 이어 둔다. */
    private fun goHome() {
        screen.value = Screen.HOME
        refresh()
    }

    /** R11-20, R11-22 [주행 끝내기]: 세션을 버리고 주행 "없음"으로. 그룹은 남는다. 주행 중에도 된다(R10). */
    private fun endDrive() {
        log("주행 끝내기")
        drive = null
        driveMembers = emptyList()
        screen.value = Screen.HOME
        refresh()
    }

    // ---- 그룹 주행(R11) ----

    private fun chooseDestination(destination: Destination) {
        log("목적지 선택 ${destination.name}")
        drive?.setDestination(destination)
        refresh()
    }

    private fun handleDemo(intent: Intent?) {
        val demo = intent?.getStringExtra("demo") ?: return
        intent.removeExtra("demo")
        log("== demo $demo")
        val (command, arg) = demo.split(":", limit = 2).let { it[0] to it.getOrNull(1) }
        runCatching {
            when (command) {
                "invite" -> invite(arg!!)
                "accept" -> respondInvite(true)
                "decline" -> respondInvite(false)
                "start" -> startDestination()
                "disband" -> disband()
                "status" -> me.setMyStatus(UserStatus.valueOf(arg!!.uppercase()))
                "home" -> goHome()
                "resume" -> resume()
                "end" -> endDrive()
                "dest" -> chooseDestination(PRESET_DESTINATIONS.first { it.name == arg })
                "depart" -> drive?.server?.depart(arg!!)
                "arrive" -> drive?.server?.arrive(arg!!)
                "retry" -> drive?.retryMyRoute()
                "airbag" -> accident.onTelltales(
                    listOf(TelltaleReading(TelltaleType.AIRBAG, if (arg == "on") TelltaleState.ON else TelltaleState.OFF)),
                )
                "ok" -> accident.respondOk()
                // R8-11 확인용: 8초 뒤 경고등을 넣는다(그사이 내비를 앞으로 띄워 둔다).
                "airbaglater" -> scheduler.schedule(8_000) {
                    accident.onTelltales(listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON)))
                }
                else -> log("모르는 명령: $demo")
            }
        }.onFailure { log("명령 실패: $it") }
        refresh()
    }

    private fun refresh() {
        val d = drive
        homeModel.value = homeScreenModel(PEOPLE, me.myStatus, roster, driveActive = d != null, gear = gear)
        groupModel.value = d?.let {
            groupScreenModel(
                session = it.session,
                myId = ME,
                memberIds = driveMembers,
                names = names,
                isLeader = true,
                gear = gear,
                canRetryMyRoute = it.canRetryMyRoute,
            )
        }
        if (d == null && screen.value == Screen.DRIVE) screen.value = Screen.HOME
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
