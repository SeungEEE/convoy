package com.car.convoy.ui

import com.car.convoy.data.GroupMapController
import com.car.convoy.data.MapGuide
import com.car.convoy.data.Scheduler
import com.car.convoy.domain.Destination
import com.car.convoy.domain.DrivingRemain
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.TurnGuide
import com.car.convoy.domain.arrivalCardText
import com.car.convoy.domain.redrawIntervalSeconds
import com.car.convoy.domain.turnCardText
import java.time.ZoneId

/** 다시 그릴 때 읽는 값. 진행 중인 그룹 주행이 없으면 [MapScreenController]의 inputs가 null을 돌려준다. */
data class MapInputs(
    val map: GroupMapController,
    val destination: Destination?,
    val waypoints: List<Destination>,
    val me: GeoPoint?,
    val meUpdatedAt: Long?,
)

/**
 * 내비 콜백을 받은 시점의 세션과 경로 세대. SDK 스레드에서 [MapScreenController.token]으로 만들고,
 * 메인 스레드에서 처리할 때 지금 값과 다르면 버린다(끝낸 세션·이전 경로의 늦은 콜백, Codex 리뷰).
 */
data class GuideToken(val session: Int, val generation: Int)

/** 예약 시계가 조금 일찍 울려도(docs/sdk-findings.md 작업 21) 그 주기로 본다. */
private const val EARLY_TOLERANCE_MILLIS = 500L

/**
 * SPEC R14-8·15·17, 작업 36: 함께 주행 지도 화면의 흐름. SDK와 무관하고 메인 스레드에서만 부른다([token] 제외).
 * - 회전 안내·도착 값은 내 차의 새 경로가 시작된 뒤([onRouteReady]: onRouteStarted 또는 경유지 반영)에만 받는다.
 *   그 전의 값(경유지를 넣는 중 오는 0m 0s 등)은 이전 경로의 값으로 보고 버린다.
 * - 그룹 목적지·경유지가 바뀌거나, 경로 요청이 실패하거나, 경로가 취소되면(경유지 변경 중 제외) 카드를 지운다.
 * - 마커는 정차 중 2초·주행 중 10초마다 다시 그린다([tick]). 카드만 바뀌면 지도 그리기 번호([drawVersion])는 그대로다.
 */
class MapScreenController(
    private val scheduler: Scheduler,
    private val names: Map<String, String>,
    private val gear: () -> Gear,
    private val inputs: () -> MapInputs?,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    /** 화면이 그릴 값. 진행 중인 그룹 주행이 없으면 null. */
    var ui: GroupMapUi? = null
        private set

    /** 선·마커·핀이 바뀔 때만 1씩 는다. 지도는 이 값이 바뀔 때만 다시 그린다(카드만 바뀌면 그대로). */
    var drawVersion = 0
        private set

    /** 다시 그린 횟수(주기 확인용). */
    var drawCount = 0
        private set

    /** R14-17: 마지막으로 그린 경로의 번호([GroupMapController.routeVersion]). 바뀌면 카메라를 한 번 맞춘다. */
    var routeVersion = 0
        private set

    private val guide = MapGuide()

    @Volatile private var session = 0
    @Volatile private var generation = 0

    /** 내 차의 새 경로가 시작됐는가. 닫혀 있으면 회전 안내·도착 값을 받지 않는다. */
    private var routeReady = false
    private var target: Pair<Destination?, List<Destination>>? = null
    private var lastDrawAt: Long? = null

    /** SDK 콜백을 받은 스레드에서 부른다. */
    fun token() = GuideToken(session, generation)

    private fun isCurrent(token: GuideToken) = token.session == session && token.generation == generation

    /** [목적지로 함께]로 새 세션을 시작한다. */
    fun startSession() {
        session++
        generation++
        resetGuide()
        target = null
        lastDrawAt = null
        redraw()
    }

    /** [주행 끝내기]. */
    fun endSession() {
        session++
        generation++
        resetGuide()
        target = null
        lastDrawAt = null
        ui = null
    }

    /** R14-15: 그룹 목적지·경유지가 바뀌면 이전 경로의 값을 지우고 새 경로가 시작될 때까지 받지 않는다. 내 위치와 무관하다. */
    fun onTargetChanged(destination: Destination?, waypoints: List<Destination>) {
        val key = destination to waypoints
        if (key == target) return
        target = key
        generation++
        resetGuide()
        refreshCards()
    }

    /** 내 차의 새 경로가 시작됐다(onRouteStarted가 거르기를 통과했거나, 내 차에 경유지가 반영됨). */
    fun onRouteReady(token: GuideToken) {
        if (isCurrent(token)) routeReady = true
    }

    /** R14-7: onTBTInfo. */
    fun onTurns(token: GuideToken, turns: List<TurnGuide>) {
        if (!isCurrent(token) || !routeReady) return
        guide.onTurns(turns)
        refreshCards()
    }

    /** R14-15: onDrivingInfo의 목적지 항목. */
    fun onRemain(token: GuideToken, remain: DrivingRemain?) {
        if (!isCurrent(token) || !routeReady) return
        guide.onDestinationRemain(remain)
        refreshCards()
    }

    /** R14-15: 경로 취소. 경유지 변경 중 취소(R13-9, [expected])는 지우지 않는다. */
    fun onRouteCancelled(token: GuideToken, expected: Boolean) {
        if (!isCurrent(token) || expected) return
        resetGuide()
        refreshCards()
    }

    /** R14-15, R11-19: 내 차의 경로 요청 실패. */
    fun onRouteFailed(token: GuideToken) {
        if (!isCurrent(token)) return
        resetGuide()
        refreshCards()
    }

    /** R14-2·17: 길찾기를 시작했거나 결과가 왔다. 경로와 안내 문구가 바뀌므로 바로 그린다. */
    fun onRouteResult() = redraw()

    /** R14-8: 위치 요청 주기(2초)마다 부른다. 정차 중 2초, 주행 중 10초가 지났으면 다시 그린다. */
    fun tick() {
        val last = lastDrawAt
        val interval = redrawIntervalSeconds(gear()) * 1_000L
        if (last == null || scheduler.nowMillis() - last >= interval - EARLY_TOLERANCE_MILLIS) redraw()
    }

    /** 지금 상태로 그릴 값을 다시 만든다. 선·마커·핀이 바뀌었을 때만 [drawVersion]을 올린다. */
    fun redraw() {
        val i = inputs()
        if (i == null) {
            ui = null
            return
        }
        val next = groupMapUi(
            snapshot = i.map.snapshot(i.me, i.meUpdatedAt),
            loading = i.map.loading,
            failed = i.map.loadFailed,
            destination = i.destination,
            waypoints = i.waypoints,
            names = names,
            turn = guide.turn,
            remain = guide.remain,
            nowMillis = wallClock(),
            zone = zone,
        )
        val previous = ui
        if (previous == null || previous.lines != next.lines || previous.markers != next.markers || previous.pins != next.pins) drawVersion++
        ui = next
        routeVersion = i.map.routeVersion
        lastDrawAt = scheduler.nowMillis()
        drawCount++
    }

    private fun resetGuide() {
        guide.clear()
        routeReady = false
    }

    /** 카드만 바꾼다. 마커는 R14-8의 주기를 지킨다. */
    private fun refreshCards() {
        val current = ui ?: return
        val hasDestination = inputs()?.destination != null
        ui = current.copy(
            turnCard = if (hasDestination) guide.turn?.let { turnCardText(it.turnTypeName, it.distanceMeters, it.description) } else null,
            arrivalCard = if (hasDestination) guide.remain?.let { arrivalCardText(wallClock(), zone, it.seconds, it.meters) } else null,
        )
    }
}
