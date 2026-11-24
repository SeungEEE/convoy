package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.RouteResult
import com.car.convoy.domain.detourSeconds
import com.car.convoy.domain.port.RoutePort

/** SPEC R14-20: 비교할 멤버. [start]는 후보를 고른 순간의 위치이고, 없으면 길찾기를 부르지 않는다. */
data class MeetingMember(val id: String, val start: GeoPoint?)

/**
 * SPEC R14-20: 비교 표 한 줄의 값. [loading]은 이 멤버에게 부른 길찾기 중 아직 오지 않은 것이 있는지다.
 * [toCandidate]는 후보까지 경로(실패면 null), [detourSeconds]는 `detourSeconds(곧장, 경유)`(하나라도 실패면 null)다.
 */
data class MeetingRow(
    val id: String,
    val located: Boolean,
    val loading: Boolean,
    val toCandidate: RouteResult?,
    val detourSeconds: Int?,
)

/**
 * SPEC R14-2·20: 합류 비교. 후보를 고를 때만 길찾기를 부른다. 멤버마다 후보까지, 곧장(목적지가 있을 때), 경유(목적지가 있을 때)를 찾는다.
 * 다른 후보를 고르면 이전 결과는 지우고, 늦게 온 이전 응답은 버린다. 같은 후보가 아직 계산 중이면 다시 눌러도 무시한다(쿼터 보호).
 * 모든 메서드는 같은 스레드에서 부른다(RoutePort의 결과도 그 스레드로 온다).
 */
class MeetingCompareController(
    private val routes: RoutePort,
    /**
     * 아직 시작하지 않은 이전 길찾기 요청을 버린다(작업 37 Codex 리뷰). [seq]는 늦은 응답이 상태를 덮는 것만 막고 요청 자체는 막지 못하므로,
     * 다른 후보를 고르거나 닫을 때 대기열을 비워 쿼터와 대기 시간을 아낀다. 버려진 요청의 응답은 오지 않는다.
     */
    private val dropQueued: () -> Unit = {},
    private val onChange: () -> Unit = {},
) {
    private enum class Kind { TO_CANDIDATE, DIRECT, VIA }

    private class Entry(val id: String, val located: Boolean) {
        val pending = mutableSetOf<Kind>()
        val results = mutableMapOf<Kind, RouteResult?>()
    }

    private var seq = 0
    private var entries: List<Entry> = emptyList()

    /** 고른 후보. 고르기 전에는 null. */
    var candidate: Destination? = null
        private set

    /** 후보를 고른 시각. 표의 "도착" 시각은 이 시각으로 계산한다(R14-20). */
    var selectedAt: Long? = null
        private set

    var hasDestination: Boolean = false
        private set

    /** 받아들인 고르기마다 1씩 는다. 화면은 이 값으로 카메라를 한 번만 맞춘다(R14-17·20). */
    var selectVersion: Int = 0
        private set

    val rows: List<MeetingRow>
        get() = entries.map { e ->
            MeetingRow(
                id = e.id,
                located = e.located,
                loading = e.pending.isNotEmpty(),
                toCandidate = e.results[Kind.TO_CANDIDATE],
                detourSeconds = detourSeconds(e.results[Kind.DIRECT]?.durationSeconds, e.results[Kind.VIA]?.durationSeconds),
            )
        }

    /** 위치가 있는 모든 멤버의 "후보까지" 결과가 왔다(실패 포함). 위치가 없는 멤버는 끝난 것으로 본다(R14-20). */
    val cameraReady: Boolean
        get() = candidate != null && entries.none { Kind.TO_CANDIDATE in it.pending }

    /**
     * SPEC R14-20: [candidate]를 고른다. 경유지는 그룹이 승인한 목록([waypoints])이고, 후보는 그 뒤에 붙는다(R13-2).
     * 같은 후보가 아직 계산 중이면 무시하고 false를 돌려준다.
     */
    fun select(
        candidate: Destination,
        members: List<MeetingMember>,
        destination: GeoPoint?,
        waypoints: List<GeoPoint>,
        nowMillis: Long,
    ): Boolean {
        if (candidate == this.candidate && entries.any { it.pending.isNotEmpty() }) return false
        dropQueued()
        val mySeq = ++seq
        this.candidate = candidate
        selectedAt = nowMillis
        hasDestination = destination != null
        selectVersion++
        val target = GeoPoint(candidate.latitude, candidate.longitude)
        entries = members.map { Entry(it.id, it.start != null) }
        // 먼저 대기 목록을 다 채운 뒤 부른다. 가짜처럼 바로 응답하는 RoutePort에서도 계산 중이 끝나기 전에 비지 않게.
        val calls = mutableListOf<() -> Unit>()
        members.zip(entries).forEach { (member, entry) ->
            val start = member.start ?: return@forEach
            fun call(kind: Kind, to: GeoPoint, via: List<GeoPoint>) {
                entry.pending += kind
                calls += {
                    routes.route(start, to, via) { result ->
                        if (mySeq != seq) return@route
                        entry.results[kind] = result
                        entry.pending -= kind
                        onChange()
                    }
                }
            }
            call(Kind.TO_CANDIDATE, target, waypoints)
            if (destination != null) {
                call(Kind.DIRECT, destination, waypoints)
                call(Kind.VIA, destination, waypoints + target)
            }
        }
        onChange()
        calls.forEach { it() }
        return true
    }

    /** 비교 화면을 닫을 때. 진행 중인 응답은 버린다. */
    fun clear() {
        dropQueued()
        seq++
        candidate = null
        selectedAt = null
        hasDestination = false
        entries = emptyList()
        onChange()
    }
}
