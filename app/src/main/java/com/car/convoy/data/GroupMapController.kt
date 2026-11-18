package com.car.convoy.data

import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.MemberRelation
import com.car.convoy.domain.RouteResult
import com.car.convoy.domain.pointAtProgress
import com.car.convoy.domain.port.RoutePort
import com.car.convoy.domain.relation
import com.car.convoy.domain.snapToRoute
import kotlin.math.abs

/** SPEC R14-6: 지도 위 멤버 하나. [gapMeters]는 경로를 따라 잰 나와의 거리, [progress]는 경로 위 진행 거리. */
data class MemberOnMap(
    val id: String,
    val position: GeoPoint,
    val progress: Double,
    val relation: MemberRelation,
    val gapMeters: Double,
    val ageSeconds: Int,
)

/** SPEC R14-5·6: 그릴 값. 경로가 없으면 [route]가 비고 [members]도 빈다. */
data class MapSnapshot(
    val route: List<GeoPoint>,
    val myPoint: GeoPoint?,
    val myProgress: Double?,
    val meUnknown: Boolean,
    val members: List<MemberOnMap>,
)

/**
 * 작업 35, SPEC R14-2·3·5: 그룹 경로를 불러오고(늦게 온 이전 응답은 버림), 시연용 가상 멤버를 경로 위에 두고 움직인다.
 * 위치가 바뀌어도 길찾기를 다시 부르지 않고 저장된 경로에 다시 투영한다(R14-6). 모든 메서드는 같은 스레드에서 부른다.
 */
class GroupMapController(
    private val routes: RoutePort,
    private val scheduler: Scheduler,
    private val onChange: () -> Unit = {},
) {
    private var route: RouteResult? = null
    private var requestSeq = 0

    var loading = false
        private set

    /** SPEC R14-2: 마지막 길찾기가 실패했다. 경로선을 지어내 그리지 않는다. */
    var loadFailed = false
        private set

    /** 경로 시간·거리(도착 카드가 내비 값을 못 받았을 때의 참고용). */
    val routeSummary: RouteResult?
        get() = route

    // 가상 멤버: 처음에는 내 진행 거리 기준 오프셋, 내 위치를 확인한 뒤에는 좌표로 기억한다.
    // 좌표로 두어야 경로가 바뀌거나 길찾기가 한 번 실패해도 같은 자리를 새 경로에 다시 투영할 수 있다(Codex 리뷰).
    private val pendingOffsets = linkedMapOf<String, Double>()
    private val virtualPosition = linkedMapOf<String, GeoPoint>()
    private val frozenAt = mutableMapOf<String, Long>()

    /** SPEC R14-2: 목적지·경유지가 바뀔 때만 부른다. [via]는 차례로 거칠 곳(시연에서는 내 위치 + 그룹 경유지). */
    fun load(start: GeoPoint, destination: GeoPoint, via: List<GeoPoint>) {
        val seq = ++requestSeq
        loading = true
        // 요청 중에는 옛 경로를 지금 경로처럼 그리지 않는다(Codex 리뷰). 멤버 좌표는 그대로 남는다.
        route = null
        onChange()
        routes.route(start, destination, via) { result ->
            if (seq != requestSeq) return@route
            route = result
            loadFailed = result == null
            loading = false
            onChange()
        }
    }

    /** SPEC R14-3: 시연용 가상 멤버를 내 진행 거리에서 [offsetMeters](음수면 뒤) 떨어진 경로 위에 둔다. */
    fun placeVirtual(id: String, offsetMeters: Double) {
        virtualPosition.remove(id)
        frozenAt.remove(id)
        pendingOffsets[id] = offsetMeters
    }

    /** SPEC R14-3: 시연 버튼 [<이름> 300m 앞으로]. 경로를 따라 움직인다. */
    fun moveVirtual(id: String, meters: Double) {
        val position = virtualPosition[id]
        if (position == null) {
            pendingOffsets[id]?.let { pendingOffsets[id] = it + meters }
            return
        }
        val path = route?.points.orEmpty()
        val progress = snapToRoute(position, path)?.progressMeters ?: return
        pointAtProgress(path, progress + meters)?.let { virtualPosition[id] = it }
    }

    /** 시연용: 이 가상 멤버의 위치 갱신을 멈춘다(30초 뒤 STALE, R14-5). */
    fun freezeVirtual(id: String) {
        frozenAt[id] = scheduler.nowMillis()
    }

    /** SPEC R14-4·5·6: 내 위치([me], 받은 시각 [meUpdatedAt])로 그릴 값을 만든다. */
    fun snapshot(me: GeoPoint?, meUpdatedAt: Long?): MapSnapshot {
        val path = route?.points.orEmpty()
        if (path.isEmpty() || me == null || meUpdatedAt == null) return MapSnapshot(path, me, null, true, emptyList())
        val now = scheduler.nowMillis()
        val mySnap = snapToRoute(me, path) ?: return MapSnapshot(path, me, null, true, emptyList())
        val myAge = ((now - meUpdatedAt) / 1000).toInt()
        val meKnown = relation(mySnap.progressMeters, mySnap.offRouteMeters, myAge, 0.0, 0.0, 0) != MemberRelation.ME_UNKNOWN

        // 내 위치가 확인됐을 때만 시연 자리를 확정한다. 오래되거나 경로 밖인 위치로 정하지 않는다(Codex 리뷰).
        if (meKnown) {
            pendingOffsets.forEach { (id, offset) ->
                pointAtProgress(path, (mySnap.progressMeters + offset).coerceAtLeast(0.0))?.let { virtualPosition[id] = it }
            }
            pendingOffsets.clear()
        }

        val members = virtualPosition.mapNotNull { (id, position) ->
            val snap = snapToRoute(position, path) ?: return@mapNotNull null
            val age = frozenAt[id]?.let { ((now - it) / 1000).toInt() } ?: 0
            MemberOnMap(
                id = id,
                position = position,
                progress = snap.progressMeters,
                relation = relation(mySnap.progressMeters, mySnap.offRouteMeters, myAge, snap.progressMeters, snap.offRouteMeters, age),
                gapMeters = abs(snap.progressMeters - mySnap.progressMeters),
                ageSeconds = age,
            )
        }
        return MapSnapshot(path, me, mySnap.progressMeters, !meKnown, members)
    }
}
