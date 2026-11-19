package com.car.convoy

import android.content.Intent
import com.car.convoy.data.GroupMapController
import com.car.convoy.data.MapGuide
import com.car.convoy.data.route.KakaoRouteAdapter
import com.car.convoy.domain.arrivalCardText
import com.car.convoy.domain.redrawIntervalSeconds
import com.car.convoy.domain.turnCardText
import com.car.convoy.ui.GroupMap
import com.car.convoy.ui.GroupMapUi
import com.car.convoy.ui.groupMapUi
import com.kakao.vectormap.KakaoMapSdk
import androidx.compose.runtime.mutableIntStateOf
import java.time.ZoneId
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.distanceMeters
import java.util.concurrent.Executors
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
import com.car.convoy.domain.GroupStatus
import com.car.convoy.domain.ProposalCheck
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
import com.car.convoy.ui.WaypointPanelModel
import com.car.convoy.ui.waypointPanelModel
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
 *   일행 지도(R14, 작업 35): mapmove:p1(경로를 따라 300m 앞으로), mapfreeze:p3(위치 갱신 멈춤), maplog
 *   경유지(R13, 작업 31): wpropose:p3(가상 멤버 제안, 덕평휴게소) | wpropose:me,pangyo(내가 제안 = 바로 보냄),
 *     wapprove, wreject, wcancel, wfail:p3(다음 전달 실패), wresend
 */
/** SPEC R14-3: 시연용 그룹 경로 출발점(에뮬레이터 내 위치 북서쪽). 이 점 → 나 → 목적지로 찾아 "뒤" 멤버 자리를 만든다. */
private val DEMO_ROUTE_START = GeoPoint(37.4250, 127.0890)

/** 에뮬레이터의 고정 위치(docs/sdk-findings.md). 여기서 50m 안일 때만 시연 출발점을 쓴다. */
private val EMULATOR_FIXED_LOCATION = GeoPoint(37.4127, 127.0947)

class MainActivity : ComponentActivity() {

    private val scheduler = MainThreadScheduler()
    private val screen = mutableStateOf(Screen.HOME)
    private val homeModel = mutableStateOf<HomeScreenModel?>(null)
    private val groupModel = mutableStateOf<GroupScreenModel?>(null)
    private val accidentModel = mutableStateOf<AccidentScreenModel?>(null)
    private val pickerModel = mutableStateOf<DestinationPickerModel?>(null)
    private val waypointModel = mutableStateOf<WaypointPanelModel?>(null)

    /** SPEC R13-1: 내 [경유지로]가 막힌 이유. 다음 제안이 되면 지운다. */
    private var waypointMessage: String? = null
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
    private var myLocationAt: Long? = null

    // ---- SPEC R14, 작업 35: 일행 지도(그룹 경로와 앞뒤 관계) ----
    private val routeIoDelegate = lazy { Executors.newSingleThreadExecutor() }
    private val routeIo by routeIoDelegate
    private val routes by lazy {
        KakaoRouteAdapter(BuildConfig.KAKAO_REST_API_KEY, routeIo, { r -> scheduler.post { r.run() } }, ::log)
    }
    private var groupMap: GroupMapController? = null
    private var mapTicker: Cancellable? = null
    private var mapLoadedFor: Pair<Destination, List<Destination>>? = null

    // ---- SPEC R14-6~8·13~17, 작업 36: 함께 주행 지도 화면 ----
    private val mapGuide = MapGuide()
    private val mapUi = mutableStateOf<GroupMapUi?>(null)
    private val mapRouteVersion = mutableIntStateOf(0)
    private var lastMapDrawAt = Long.MIN_VALUE
    /** R14-15: 마지막으로 본 그룹 목적지·경유지. 바뀌면 회전 안내·도착 값을 지운다. */
    private var guideKey: Pair<Destination?, List<Destination>>? = null
    /**
     * 지도를 그릴 수 없는 이유. null이면 지도를 쓴다. 키가 없거나, 카카오맵 네이티브 라이브러리가 이 기기 ABI에 없으면
     * (x86_64 에뮬레이터, docs/sdk-findings.md) 지도 대신 이 문구를 보여 준다. 경로·앞뒤 계산과 카드는 그대로 동작한다.
     */
    private var mapUnavailable: String? = null
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
                // R14-15: 내 차의 경로 요청이 실패하면 회전 안내·도착 값을 지운다.
                scheduler.post { clearMapGuide("경로 요청 실패"); refresh() }
            },
            locationReceived = { loc ->
                val at = scheduler.nowMillis()
                scheduler.post { myLocation = loc; myLocationAt = at; accident.onLocation(loc); reloadMapIfNeeded() }
            },
            log = ::log,
            naviDestination = { d -> scheduler.post { naviDestination = d; refresh() } },
            recentDestinations = { list -> scheduler.post { recentDestinations = list; refresh() } },
            bookmarks = { list -> scheduler.post { bookmarkDestinations = list; refresh() } },
            waypointAdded = { postToDrive { it.onMyWaypointAdded() } },
            // addWaypoint 예외는 호출 중 동기로 온다(R13-6).
            waypointFailed = { drive?.onMyWaypointFailed() },
            // R13-9: 콜백을 받은 시각으로 판정한다(메인 스레드 지연과 무관하게).
            routeCancelled = {
                val at = scheduler.nowMillis()
                scheduler.post {
                    drive?.let {
                        val expected = it.isExpectedRouteCancel(at)
                        log("경로 취소: ${if (expected) "경유지 변경 중(무시, R13-9)" else "안내 종료"}")
                        // R14-15: 안내가 끝나면 회전 안내·도착 값을 지운다. 경유지 변경 중 취소(R13-9)는 제외한다.
                        if (!expected) clearMapGuide("경로 취소")
                    }
                }
            },
            // R14-7·15: 그룹 주행 중에만 쓴다(앞차 따라가기 화면에는 지도가 없다).
            turns = { list -> scheduler.post { if (drive != null) { mapGuide.onTurns(list); refreshMapCards() } } },
            destinationRemain = { r -> scheduler.post { if (drive != null) { mapGuide.onDestinationRemain(r); refreshMapCards() } } },
        )
        // R14-9: 카카오 키가 있을 때만 지도를 쓴다(키는 local.properties, R14-1).
        // UnsatisfiedLinkError는 Exception이 아니므로 Throwable로 받는다.
        mapUnavailable = when {
            BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank() -> "지도를 쓰려면 카카오 키가 필요해요"
            else -> try {
                KakaoMapSdk.init(applicationContext, BuildConfig.KAKAO_NATIVE_APP_KEY)
                null
            } catch (t: Throwable) {
                log("카카오맵 초기화 실패: $t")
                "이 기기에서는 지도를 그릴 수 없어요"
            }
        }
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
                            waypoint = waypointModel.value,
                            onProposeWaypoint = ::proposeWaypoint,
                            onApproveWaypoint = { drive?.waypoints?.approve(); log("경유지 승인") },
                            onRejectWaypoint = { drive?.waypoints?.reject(); log("경유지 거절") },
                            onResendWaypoint = { drive?.waypoints?.resend(); log("경유지 다시 보내기") },
                            onDemoProposeWaypoint = { id -> drive?.waypoints?.propose(id, demoWaypoint("rest"))?.let { log("경유지 제안 결과 $it") }; refresh() },
                            onDemoFailWaypoint = { id -> drive?.server?.failNextWaypoint(id); log("시연: $id 다음 경유지 반영 실패") },
                            // SPEC R14-16, 작업 36: 함께 주행 지도
                            map = { m -> GroupMap(mapUi.value, mapRouteVersion.intValue, mapUnavailable, m) },
                            memberLabels = mapUi.value?.memberLabels.orEmpty(),
                            onDemoMapMove = { id -> moveVirtualMember(id) },
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
        // 작업 35: 지도 위치 요청 주기와 길찾기 실행기를 정리한다(Codex 리뷰).
        mapTicker?.cancel()
        mapTicker = null
        if (routeIoDelegate.isInitialized()) routeIo.shutdownNow()
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
        drive = GroupController(
            ME, driveMembers, navi, scheduler, initialGear = gear,
            onUpdate = { u ->
                log("${u.memberId} ${u.event} → ${drive?.session?.memberState(u.memberId)} " +
                    "효과=${u.result.memberEffect} 그룹=${u.result.groupEffect}")
                refresh()
            },
            onWaypointChange = {
                // 상태가 바뀌면 막힌 제안 안내는 지운다(Codex 리뷰). 새 상태의 안내(만료·거절 등)가 보이게.
                waypointMessage = null
                // R14-2: 경유지 목록이 바뀌면 그룹 경로를 다시 찾는다.
                reloadMapIfNeeded()
                drive?.waypoints?.let { w ->
                    log("경유지 제안=${w.proposal?.let { "${it.proposerId}:${it.place.name}:${it.state}" }} " +
                        "경유지=${w.waypoints.map { it.name }} 반영=${w.applyStates} 요약=${w.summary}")
                }
                refresh()
            },
        )
        log("목적지로 함께 시작: $driveMembers")
        // R14-3: 시연용 가상 멤버 자리. 첫째는 내 앞 650m, 둘째는 뒤 1.2km, 나머지는 앞 1.5km.
        // R14-2·17: 길찾기를 시작하거나 결과가 오면 지도를 바로 다시 그린다(경로·안내 문구가 바뀌므로).
        groupMap = GroupMapController(routes, scheduler) { redrawMap(); refresh() }.also { map ->
            driveMembers.filter { it != ME }.forEachIndexed { i, id ->
                map.placeVirtual(id, when (i) { 0 -> 650.0; 1 -> -1200.0; else -> 1500.0 })
            }
        }
        mapLoadedFor = null
        mapGuide.clear()
        guideKey = null
        redrawMap()
        startMapTicker()
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
        waypointMessage = null
        drive?.close()
        groupMap = null
        mapTicker?.cancel()
        mapTicker = null
        mapLoadedFor = null
        // R14-15: 세션을 버리면 회전 안내·도착 값과 지도를 지운다.
        mapGuide.clear()
        guideKey = null
        mapUi.value = null
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
        reloadMapIfNeeded()
        refresh()
    }

    /**
     * SPEC R14-2·3: 그룹 목적지나 경유지가 바뀌었을 때만 길찾기를 부른다. 내 위치를 모르면 위치가 올 때 다시 본다.
     * 에뮬레이터는 내가 경로 시작점이라 "뒤" 멤버를 만들 수 없어, 시연에서는 내 뒤쪽 출발점 → 나(경유) → 목적지로 찾는다.
     */
    private fun reloadMapIfNeeded() {
        val d = drive ?: return
        val map = groupMap ?: return
        // R14-15: 그룹 목적지나 경유지가 바뀌면 이전 경로의 회전 안내·도착 값을 바로 지운다(내 위치와 무관하게).
        val currentGuideKey = d.session.destination to d.waypoints.waypoints
        if (currentGuideKey != guideKey) {
            if (guideKey != null) clearMapGuide("목적지·경유지 변경")
            guideKey = currentGuideKey
        }
        val destination = d.session.destination ?: return
        val me = myLocation ?: run { navi.requestLocation(); return }
        val key = destination to d.waypoints.waypoints
        if (key == mapLoadedFor) return
        mapLoadedFor = key
        val mePoint = GeoPoint(me.latitude, me.longitude)
        val waypoints = key.second.map { GeoPoint(it.latitude, it.longitude) }
        // 시연 출발점은 에뮬레이터 고정 위치에서만 쓴다. 실차에서는 내 위치가 출발점이다(Codex 리뷰).
        val demo = distanceMeters(mePoint, EMULATOR_FIXED_LOCATION) <= 50.0
        log("일행 지도 길찾기: ${destination.name}, 경유 ${key.second.map { it.name }}${if (demo) " (시연 출발점)" else ""}")
        if (demo) map.load(DEMO_ROUTE_START, GeoPoint(destination.latitude, destination.longitude), listOf(mePoint) + waypoints)
        else map.load(mePoint, GeoPoint(destination.latitude, destination.longitude), waypoints)
    }

    /**
     * SPEC R14-8: 그룹 주행 중 내 위치를 2초마다 묻고, 마커는 정차 중 2초·주행 중 10초마다 다시 그린다(redrawIntervalSeconds).
     * 그 사이에 들어온 위치는 다음 다시 그리기 때 한꺼번에 반영한다.
     */
    private fun startMapTicker() {
        mapTicker?.cancel()
        fun loop() {
            if (drive == null) { mapTicker = null; return }
            // 사고 확인 중에는 사고 확인이 이미 2초마다 위치를 묻는다(Codex 리뷰).
            if (!accident.checking) navi.requestLocation()
            // 예약 시계가 조금 일찍 울릴 수 있어(docs/sdk-findings.md 작업 21) 0.5초 여유를 둔다.
            val due = lastMapDrawAt == Long.MIN_VALUE ||
                scheduler.nowMillis() - lastMapDrawAt >= redrawIntervalSeconds(gear) * 1_000L - 500
            if (due) redrawMap()
            mapTicker = scheduler.schedule(2_000) { loop() }
        }
        loop()
    }

    /** SPEC R14-6·13·16: 지금 상태로 지도에 그릴 값을 다시 만든다. */
    private fun redrawMap() {
        val d = drive
        val map = groupMap
        if (d == null || map == null) { mapUi.value = null; return }
        val me = myLocation?.let { GeoPoint(it.latitude, it.longitude) }
        mapUi.value = groupMapUi(
            snapshot = map.snapshot(me, myLocationAt),
            loading = map.loading,
            failed = map.loadFailed,
            destination = d.session.destination,
            waypoints = d.waypoints.waypoints,
            names = names,
            turn = mapGuide.turn,
            remain = mapGuide.remain,
            nowMillis = System.currentTimeMillis(),
            zone = ZoneId.systemDefault(),
        )
        mapRouteVersion.intValue = map.routeVersion
        lastMapDrawAt = scheduler.nowMillis()
    }

    /** SPEC R14-7·15: 카드만 바로 바꾼다. 마커는 R14-8의 다시 그리기 간격을 지킨다. */
    private fun refreshMapCards() {
        val ui = mapUi.value ?: return
        val hasDestination = drive?.session?.destination != null
        mapUi.value = ui.copy(
            turnCard = if (hasDestination) mapGuide.turn?.let { turnCardText(it.turnTypeName, it.distanceMeters, it.description) } else null,
            arrivalCard = if (hasDestination) mapGuide.remain?.let {
                arrivalCardText(System.currentTimeMillis(), ZoneId.systemDefault(), it.seconds, it.meters)
            } else null,
        )
    }

    /** SPEC R14-15: 회전 안내·도착 값을 지운다. */
    private fun clearMapGuide(reason: String) {
        if (mapGuide.turn == null && mapGuide.remain == null) return
        log("지도 카드 지움: $reason")
        mapGuide.clear()
        refreshMapCards()
    }

    /** SPEC R14-3: 시연 버튼 [<이름> 300m 앞으로]. 지도에는 다음 다시 그리기 때 반영된다(R14-8). */
    private fun moveVirtualMember(id: String) {
        groupMap?.moveVirtual(id, 300.0)
        log("시연: ${names[id] ?: id} 300m 앞으로")
    }

    /** 작업 35 확인용: 지도 상태를 로그로 남긴다. */
    private fun logMap() {
        val map = groupMap ?: return log("일행 지도 없음")
        val me = myLocation?.let { GeoPoint(it.latitude, it.longitude) }
        val s = map.snapshot(me, myLocationAt)
        log("일행 지도 경로 ${s.route.size}점, 실패=${map.loadFailed}, 내 진행=${s.myProgress?.toInt()}m, 내 위치 확인 중=${s.meUnknown}")
        s.members.forEach { m -> log("  ${names[m.id] ?: m.id} ${m.relation} ${m.gapMeters.toInt()}m (진행 ${m.progress.toInt()}m, ${m.ageSeconds}초 전)") }
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
                // 작업 35(R14-3): 가상 멤버 이동·위치 멈춤, 지도 상태 로그
                "mapmove" -> { moveVirtualMember(arg!!); logMap() }
                "mapfreeze" -> { groupMap?.freezeVirtual(arg!!); log("시연: $arg 위치 갱신 멈춤") }
                "maplog" -> logMap()
                // 작업 31(R13): 경유지. 장소는 시연용 고정값(덕평휴게소, 판교역).
                "wpropose" -> drive?.waypoints?.propose(arg!!.substringBefore(","), demoWaypoint(arg.substringAfter(",", "rest")))?.let { log("경유지 제안 결과 $it") }
                "wapprove" -> drive?.waypoints?.approve()
                "wreject" -> drive?.waypoints?.reject()
                "wcancel" -> drive?.waypoints?.cancel()
                "wfail" -> drive?.server?.failNextWaypoint(arg!!)
                "wresend" -> drive?.waypoints?.resend()
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
        waypointModel.value = d?.let {
            val w = it.waypoints
            waypointPanelModel(
                myId = ME, proposal = w.proposal, waypoints = w.waypoints, apply = w.applyStates, names = names,
                earlierFailures = w.earlierFailures,
                gear = gear, groupActive = it.session.status == GroupStatus.ACTIVE, hasDestination = it.session.destination != null,
            ).let { m -> waypointMessage?.let { msg -> m.copy(notice = msg) } ?: m }
        }
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

    /** SPEC R13-1: 내가(리더) 목록에서 [경유지로]를 누르면 바로 보낸다. 안 되는 이유는 안내 문구로 알린다. */
    private fun proposeWaypoint(place: Destination) {
        val result = drive?.waypoints?.propose(ME, place) ?: return
        log("경유지 제안 ${place.name} → $result")
        waypointMessage = when (result) {
            ProposalCheck.OK -> null
            ProposalCheck.FULL -> "경유지는 2개까지 넣을 수 있어요"
            ProposalCheck.PENDING_EXISTS -> "답하지 않은 요청이 있어요"
            ProposalCheck.APPLYING -> "앞 경유지를 반영하는 중이에요"
            ProposalCheck.SAME_AS_DESTINATION -> "목적지와 같은 곳이에요"
            ProposalCheck.ALREADY_WAYPOINT -> "이미 들르는 곳이에요"
            ProposalCheck.NO_DESTINATION, ProposalCheck.NOT_ACTIVE -> "지금은 경유지를 넣을 수 없어요"
        }
        refresh()
    }

    /** 작업 31 시연용 경유지(R13-12). 화면(작업 32) 전까지 adb로만 쓴다. */
    private fun demoWaypoint(key: String): Destination = when (key) {
        "pangyo" -> Destination("판교역", "경기 성남시 분당구 판교역로 160", 37.3948, 127.1112)
        else -> Destination("덕평휴게소", "경기 이천시 마장면", 37.2400, 127.3480)
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
