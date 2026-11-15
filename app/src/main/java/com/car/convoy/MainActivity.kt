package com.car.convoy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.car.convoy.data.AccidentController
import com.car.convoy.data.Cancellable
import com.car.convoy.data.FamilyController
import com.car.convoy.data.FollowNavigator
import com.car.convoy.data.FriendController
import com.car.convoy.data.GroupController
import com.car.convoy.data.GroupRoster
import com.car.convoy.data.MainThreadScheduler
import com.car.convoy.data.navi.NaviHelperAdapter
import com.car.convoy.data.vehicle.VehicleSource
import com.car.convoy.domain.Destination
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.FollowEvent
import com.car.convoy.domain.FollowState
import com.car.convoy.domain.ReportLocation
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.TelltaleReading
import com.car.convoy.domain.TelltaleState
import com.car.convoy.domain.TelltaleType
import com.car.convoy.domain.UserStatus
import com.car.convoy.domain.port.ConvoyServer
import com.car.convoy.ui.AccidentPrompt
import com.car.convoy.ui.ConvoyTheme
import com.car.convoy.ui.AccidentScreenModel
import com.car.convoy.ui.DestinationPickerModel
import com.car.convoy.ui.FamilyChip
import com.car.convoy.ui.FollowScreen
import com.car.convoy.ui.FollowScreenModel
import com.car.convoy.ui.followScreenModel
import com.car.convoy.ui.GroupScreen
import com.car.convoy.ui.GroupScreenModel
import com.car.convoy.ui.HomeScreen
import com.car.convoy.ui.HomeScreenModel
import com.car.convoy.ui.MY_NAME
import com.car.convoy.ui.PRESET_DESTINATIONS
import com.car.convoy.ui.SentFamilyCard
import com.car.convoy.ui.accidentScreenModel
import com.car.convoy.ui.groupScreenModel
import com.car.convoy.ui.destinationPickerModel
import com.car.convoy.ui.homeScreenModel

private const val TAG = "Convoy"
private const val ME = "me"

/** SPEC R11-21: 현대차 내비 앱 패키지(에뮬레이터). 매니페스트 <queries>에도 적는다. */
private const val NAVI_APP_PACKAGE = "ai.umos.maps.android.navigation.app"

// 작업 23(R4-5): 외부 서버가 없어 앱에 미리 정해 둔 가상 인물. 서준(오프라인)은 보이지 않는다.
private val PEOPLE = listOf(
    Friend("p1", "민수", UserStatus.ACTIVE),
    Friend("p2", "현우", UserStatus.ACTIVE),
    Friend("p3", "수진", UserStatus.ACTIVE),
    Friend("p4", "지영", UserStatus.BUSY),
    Friend("p5", "서준", UserStatus.OFFLINE),
)

private enum class Screen { HOME, DRIVE, FOLLOW }

/**
 * 작업 23 홈(R2-5, R4-5)과 그룹 주행(R11-20). 나는 리더이고 실제 SDK를 쓰며, 그룹 멤버는 가상 인물이다(R11-18).
 * adb 시연 명령(녹화·자동 확인용): adb shell am start -n com.car.convoy/.MainActivity --es demo <명령>
 *   홈: invite:p1, accept, decline(첫 수락 대기에 응답), start(목적지로 함께), disband, status:busy
 *   가족(R9): family:p1, faccept, fdecline, frelease
 *   앞차 따라가기(R12): lead:p1, laccept, ldecline, move:100, unfollow
 *   주행: dest:서울역, depart:p1, arrive:p1, retry, home, end, resume
 *   사고(R8): airbag:on|off, ok, airbaglater(8초 뒤 켜기, R8-11 확인용)
 */
class MainActivity : ComponentActivity() {

    private val scheduler = MainThreadScheduler()
    private val screen = mutableStateOf(Screen.HOME)
    private val homeModel = mutableStateOf<HomeScreenModel?>(null)
    private val groupModel = mutableStateOf<GroupScreenModel?>(null)
    private val accidentModel = mutableStateOf<AccidentScreenModel?>(null)
    private val pickerModel = mutableStateOf<DestinationPickerModel?>(null)
    private val followModel = mutableStateOf<FollowScreenModel?>(null)

    private val roster = GroupRoster(PEOPLE)
    private val family = FamilyController(PEOPLE) // 가족은 그룹과 무관하게 항상 연결(R9-1).
    private val me = FriendController(PEOPLE) // 내 상태(R2-1)와 따라가기 요청·수락·끊기(R3).
    private val names = PEOPLE.associate { it.id to it.name } + (ME to MY_NAME)

    /** SDK가 기어를 다시 보내 주지 않으므로 여기서 들고, 주행마다 새 컨트롤러에 넘긴다. */
    private var gear = Gear.UNKNOWN

    /** 진행 중인 "목적지로 함께". 없으면 주행이 "없음"(R4-5). SDK 콜백 스레드에서도 읽으므로 @Volatile. */
    @Volatile private var drive: GroupController? = null
    private var driveMembers: List<String> = emptyList()

    // SPEC R11-21: 목적지 출처. 화면이 앞으로 올 때와 그룹 주행 화면에 들어올 때 다시 묻는다.
    private var naviDestination: Destination? = null
    private var recentDestinations: List<Destination> = emptyList()
    private var bookmarkDestinations: List<Destination> = emptyList()
    private var ignoredSuggestion: Destination? = null
    private var naviReady = false

    // ---- 작업 25 앞차 따라가기(R12) ----
    private lateinit var follower: FollowNavigator
    private var myLocation: ReportLocation? = null
    private var followTicker: Cancellable? = null

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
            routeStarted = { name -> postToDrive { it.onRouteStarted(name) } },
            destinationArrived = { n, a -> postToDrive { it.onDestinationArrived(n, a) } },
            // requestRoute 예외는 호출 중 동기로 오므로 큐에 넣지 않고 바로 알린다(R11-19). 화면 갱신만 나중에 한다.
            routeFailed = {
                if (follower.active) follower.onRouteFailed() else drive?.onMyRouteFailed()
                scheduler.post { refresh() }
            },
            locationReceived = { loc -> scheduler.post { myLocation = loc; accident.onLocation(loc) } },
            log = ::log,
            naviDestination = { d -> scheduler.post { naviDestination = d; refresh() } },
            recentDestinations = { list -> scheduler.post { recentDestinations = list; refresh() } },
            bookmarks = { list -> scheduler.post { bookmarkDestinations = list; refresh() } },
        )
        follower = FollowNavigator(navi, scheduler)
        // R8-9: 시연은 가족 없이 시작한다. 가족 상태는 FamilyController에서 읽는다(R9-1). 가족이 없으면 보내지 않는다(R8-5).
        accident = AccidentController(familyServer, navi, scheduler, FamilyState.NONE, ::onAccidentChange, familySource = { family.state })
        vehicle = VehicleSource(
            applicationContext,
            onGear = { g -> scheduler.post { gear = g; drive?.onGearChanged(g); refresh() } },
            onTelltales = { r -> scheduler.post { accident.onTelltales(r) } },
            log = ::log,
        )

        setContent {
            ConvoyTheme {
                val accidentUi = accidentModel.value
                val familyChip: @Composable () -> Unit = { homeModel.value?.let { h -> FamilyChip(h.familyHeader) } }
                Box {
                    val group = groupModel.value
                    val followUi = followModel.value
                    if (screen.value == Screen.FOLLOW && followUi != null) {
                        FollowScreen(
                            model = followUi,
                            onHome = ::goHome,
                            onStop = ::stopFollow,
                            onRetry = { follower.retry(); refresh() },
                            onDemoMove = ::moveLeader,
                            headerExtra = familyChip,
                        )
                    } else if (screen.value == Screen.DRIVE && group != null) {
                        GroupScreen(
                            model = group,
                            onChooseDestination = ::chooseDestination,
                            onRetry = { drive?.retryMyRoute(); refresh() },
                            onDemoDepart = { id -> drive?.server?.depart(id); refresh() },
                            onDemoArrive = { id -> drive?.server?.arrive(id); refresh() },
                            picker = pickerModel.value ?: destinationPickerModel(null, null, null, emptyList(), emptyList()),
                            onIgnoreSuggestion = { d -> ignoredSuggestion = d; log("내비 목적지 무시 ${d.name}"); refresh() },
                            onOpenNavi = naviLaunchIntent()?.let { intent -> { openNavi(intent) } },
                            headerExtra = familyChip,
                            belowMembers = { accidentUi?.sentCard?.let { c -> SentFamilyCard(c, sentTo) } },
                            onHome = ::goHome,
                            onEnd = ::endDrive,
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
                                onDesignateFamily = ::designateFamily,
                                onReleaseFamily = ::releaseFamily,
                                onDemoFamilyAccept = { respondFamily(true) },
                                onDemoFamilyDecline = { respondFamily(false) },
                                onFollow = ::requestFollow,
                                onDemoFollowAccept = { respondFollow(true) },
                                onDemoFollowDecline = { respondFollow(false) },
                                // 시안: 홈은 가족 카드가 있어 머리줄 가족 칩을 두지 않는다.
                                belowFamily = { accidentUi?.sentCard?.let { c -> SentFamilyCard(c, sentTo) } },
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
        navi.start { scheduler.post { naviReady = true; queryDestinationSources(); handleDemo(intent) } }
    }

    override fun onResume() {
        super.onResume()
        queryDestinationSources()
    }

    override fun onPause() {
        cancelRequeries()
        super.onPause()
    }

    /** 화면이 가려지거나 주행이 끝나면 3초·6초 재조회 예약을 취소한다(Codex 리뷰). */
    private fun cancelRequeries() {
        requeries.forEach { it.cancel() }
        requeries = emptyList()
    }

    /** R11-21: 내비 목적지를 묻는다. onDestinationChanged가 오지 않아 화면에 돌아올 때마다 묻는다. */
    private fun queryDestinationSources() {
        if (!naviReady) return
        // 새 결과가 오기 전에 이전 제안 카드를 누르지 않도록 지운다(Codex 리뷰).
        naviDestination = null
        refresh()
        navi.queryDestinationSources()
        // R11-21: 내비에서 목적지를 정한 직후에는 빈 값이 오므로 3초·6초 뒤에 한 번씩 더 묻는다.
        cancelRequeries()
        requeries = listOf(3_000L, 6_000L).map { delay -> scheduler.schedule(delay) { navi.queryDestinationSources() } }
    }

    private var requeries: List<Cancellable> = emptyList()

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
        val sent = roster.invite(id, gear, driveActive = anyDrive)
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
        val done = roster.disband(gear, driveActive = anyDrive)
        log("그룹 해산 ${if (done) "됨" else "조건 안 맞아 무시"}")
        refresh()
    }

    /** R4-5 [목적지로 함께]: 그 시점의 그룹 멤버로 새 세션을 만든다(R11-22). */
    private fun startDestination() {
        val model = homeModel.value ?: return
        if (!model.canStartDestination || anyDrive) return
        driveMembers = listOf(ME) + roster.members
        drive = GroupController(ME, driveMembers, navi, scheduler, initialGear = gear) { u ->
            log("${u.memberId} ${u.event} → ${drive?.session?.memberState(u.memberId)} " +
                "효과=${u.result.memberEffect} 그룹=${u.result.groupEffect}")
            refresh()
        }
        log("목적지로 함께 시작: $driveMembers")
        screen.value = Screen.DRIVE
        queryDestinationSources()
        refresh()
    }

    private fun resume() {
        if (drive != null) screen.value = Screen.DRIVE
        if (follower.active) { screen.value = Screen.FOLLOW; startFollowTicker() }
        queryDestinationSources()
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
        drive?.close()
        drive = null
        cancelRequeries()
        driveMembers = emptyList()
        screen.value = Screen.HOME
        refresh()
    }

    // ---- 내비에서 검색하기(R11-21) ----

    /** 현대차 내비 앱의 실행 인텐트. 에뮬레이터 패키지이고 실차 패키지는 확인이 필요하다. 없으면 null(안내만 보여 준다). */
    private fun naviLaunchIntent(): Intent? = packageManager.getLaunchIntentForPackage(NAVI_APP_PACKAGE)

    private fun openNavi(intent: Intent) {
        log("내비에서 검색하기: 내비 앱 띄움")
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { log("내비 앱 띄우기 실패: $it") }
    }

    // ---- 앞차 따라가기(R12) ----

    /** 진행 중인 주행이 있는가(R4-5): 목적지로 함께, 또는 따라가기(요청 대기 포함). */
    private val anyDrive: Boolean get() = drive != null || me.follow != null

    /** R12-1: 수락한 멤버만, 주행이 없을 때만 요청한다. 업무 중·주행 중 판단은 FriendController(canStartFollowRequest). */
    private fun requestFollow(id: String) {
        val sent = id in roster.members && drive == null && me.requestFollow(id, gear)
        log("따라가기 요청 $id ${if (sent) "보냄" else "조건 안 맞아 무시"}")
        refresh()
    }

    /** 시연용 패널: 앞차가 될 멤버의 수락·거절(R3). 수락하면 따라가기 화면으로 넘어간다(R12-1). */
    private fun respondFollow(accept: Boolean) {
        val link = me.follow ?: return
        me.respond(if (accept) FollowEvent.ACCEPT else FollowEvent.DECLINE)
        log("따라가기 응답 ${link.friendId} ${if (accept) "수락" else "거절"}")
        if (me.follow?.state == FollowState.ACTIVE) {
            follower.start(names[link.friendId] ?: link.friendId)
            screen.value = Screen.FOLLOW
            startFollowTicker()
        }
        refresh()
    }

    private fun moveLeader(meters: Double) {
        follower.moveLeader(meters)
        log("앞차 ${meters.toInt()}m 이동 → 다시 요청 ${follower.rerouteCount}번")
        refresh()
    }

    /** R3-3, R12-4 [끊기]: ENDED로 바꾸고 내비 경로를 취소한 뒤 홈으로. 주행 중에도 된다. 그룹은 남는다. */
    private fun stopFollow() {
        log("따라가기 끊기")
        me.cancelFollow()
        follower.stop()
        followTicker?.cancel()
        followTicker = null
        screen.value = Screen.HOME
        refresh()
    }

    /** R12-4: 따라가기 화면이 열려 있는 동안 내 위치를 2초마다 묻고, 경과 시간 표시를 1초마다 갱신한다. */
    private fun startFollowTicker() {
        followTicker?.cancel()
        var tick = 0
        fun loop() {
            if (!follower.active || screen.value != Screen.FOLLOW) { followTicker = null; return }
            if (tick % 2 == 0) navi.requestLocation()
            tick++
            refresh()
            followTicker = scheduler.schedule(1_000) { loop() }
        }
        loop()
    }

    // ---- 가족(R9) ----

    /** 마지막으로 사고 알림을 받은 가족의 이름(R8-10 카드). 보낸 뒤 해제해도 카드에는 받은 사람이 남는다. */
    private var sentTo: String? = null

    private fun designateFamily(id: String) {
        val sent = family.designate(id, gear)
        log("가족 지정 $id ${if (sent) "요청" else "조건 안 맞아 무시"}")
        refresh()
    }

    private fun respondFamily(accept: Boolean) {
        log("가족 응답 ${family.familyId} ${if (accept) "수락" else "거절"}")
        family.respond(accept)
        refresh()
    }

    /** R9-2, R9-5 [가족 해제]·[지정 취소]. 주행 중에도 된다(R10). */
    private fun releaseFamily() {
        log("가족 해제·지정 취소 ${family.familyId}")
        family.release()
        refresh()
    }

    // ---- 그룹 주행(R11) ----

    /**
     * 내비 이벤트를 들어온 시점의 세션에만 적용한다. 큐에서 기다리는 사이 주행이 끝나거나 새로 시작됐으면 버린다
     * (끝낸 세션의 이벤트가 새 세션에 섞이지 않게, Codex 리뷰).
     */
    private fun postToDrive(action: (GroupController) -> Unit) {
        val session = drive ?: return
        scheduler.post { if (drive === session) action(session) }
    }

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
                "lead" -> requestFollow(arg!!)
                "laccept" -> respondFollow(true)
                "ldecline" -> respondFollow(false)
                "move" -> moveLeader(arg!!.toDouble())
                "unfollow" -> stopFollow()
                "family" -> designateFamily(arg!!)
                "faccept" -> respondFamily(true)
                "fdecline" -> respondFamily(false)
                "frelease" -> releaseFamily()
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
        homeModel.value = homeScreenModel(PEOPLE, me.myStatus, roster, driveActive = d != null, gear = gear, family = family, follow = me.follow)
        followModel.value = if (follower.active) {
            followScreenModel(
                leaderName = me.follow?.friendId?.let { names[it] } ?: "",
                me = myLocation,
                leader = follower.leader,
                leaderUpdatedAt = follower.leaderUpdatedAtMillis,
                now = scheduler.nowMillis(),
                reroutes = follower.rerouteCount,
                moved = follower.movedSinceLastRequest,
                failed = follower.routeFailed,
            )
        } else null
        if (followModel.value == null && screen.value == Screen.FOLLOW) screen.value = Screen.HOME
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
        pickerModel.value = d?.let {
            destinationPickerModel(naviDestination, it.session.destination, ignoredSuggestion, recentDestinations, bookmarkDestinations)
        }
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
            if (accident.lastSentReport != null) sentTo = family.familyName
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
