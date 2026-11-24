package com.car.convoy.ui

import com.car.convoy.data.WaypointProposal
import com.car.convoy.domain.ApplyState
import com.car.convoy.domain.Destination
import com.car.convoy.domain.DriverAction
import com.car.convoy.domain.Gear
import com.car.convoy.domain.ProposalState
import com.car.convoy.domain.applySummary
import com.car.convoy.domain.canPerform

/** SPEC R13-4·5, 시안 S13: 리더에게 보이는 경유지 요청. */
data class WaypointRequestUi(val title: String, val placeName: String, val placeAddress: String, val canRespond: Boolean)

/** SPEC R13, 작업 32: 그룹 주행 화면의 경유지 패널 값. Compose는 이 값만 그린다. */
data class WaypointPanelModel(
    /** 승인된 경유지("경유지 1 · 덕평휴게소"). */
    val waypointNames: List<String>,
    /** 다른 멤버의 대기 중 제안. 없으면 null. */
    val request: WaypointRequestUi?,
    /** "2/3 반영 · 수진 처리 중 · 민수 실패". 보낸 경유지가 없으면 null(R13-7). */
    val applyText: String?,
    val applyComplete: Boolean,
    /** [다시 보내기]: 실패가 있을 때만(R13-7). */
    val showResend: Boolean,
    /** [경유지로]: 그룹 ACTIVE, 목적지 있음, 정차 중(R13-1, R13-11). 개수·대기·반영 중 판단은 누를 때 checkProposal이 한다. */
    val canPropose: Boolean,
    /** 끝난 제안의 안내(거절·만료·취소). */
    val notice: String?,
    /** [다시 보내기]를 누를 수 있는지. 실패가 있고 정차 중일 때만(R13-7, R10 "경유지 다시 보내기"). */
    val canResend: Boolean = false,
) {
    /** SPEC R14-19 `WAYPOINT` 칸: 경유지 이름, 반영 문구, 알림, [다시 보내기] 중 하나라도 있으면 상태가 있다. 요청은 넣지 않는다. */
    val hasStatus: Boolean get() = waypointNames.isNotEmpty() || applyText != null || notice != null || showResend
}

/** SPEC R14-19 `MEMBERS`: 멤버 줄은 3줄까지 보여 주고, 넘치면 넷째 줄에 "외 n명"을 쓴다. */
fun infoCardMemberLines(labels: List<String>): List<String> =
    if (labels.size <= 3) labels else labels.take(3) + "외 ${labels.size - 3}명"

/**
 * SPEC R13-1·4·7·11. [myId]는 이 앱의 차(리더)다. [apply]는 가장 최근 경유지의 멤버별 반영 상태이고 순서대로 이름을 쓴다.
 */
fun waypointPanelModel(
    myId: String,
    proposal: WaypointProposal?,
    waypoints: List<Destination>,
    apply: Map<String, ApplyState>,
    names: Map<String, String>,
    gear: Gear,
    groupActive: Boolean,
    hasDestination: Boolean,
    /** 이전 경유지(번호 0부터)에 반영되지 못한 멤버(R13-7). */
    earlierFailures: Map<Int, Set<String>> = emptyMap(),
): WaypointPanelModel {
    fun name(id: String) = names[id] ?: id
    val summary = applySummary(apply.values.toList())
    val applyText = if (apply.isEmpty()) null else {
        val parts = mutableListOf("${summary.applied}/${summary.targets} 반영")
        if (summary.complete) parts += "모두 반영했어요"
        apply.filterValues { it == ApplyState.SENT }.keys.forEach { parts += "${name(it)} 처리 중" }
        apply.filterValues { it == ApplyState.FAILED }.keys.forEach { parts += "${name(it)} 실패" }
        parts.joinToString(" · ")
    }
    val request = proposal?.takeIf { it.state == ProposalState.PENDING && it.proposerId != myId }?.let {
        WaypointRequestUi(
            title = "${name(it.proposerId)}님의 경유지 요청",
            placeName = it.place.name,
            placeAddress = it.place.address,
            canRespond = canPerform(DriverAction.RESPOND_WAYPOINT_PROPOSAL, gear),
        )
    }
    val notice = proposal?.let {
        when (it.state) {
            ProposalState.REJECTED -> "${name(it.proposerId)}님의 요청을 거절했어요"
            ProposalState.EXPIRED -> "응답이 없어 ${name(it.proposerId)}님의 요청이 만료됐어요"
            ProposalState.CANCELLED -> "${name(it.proposerId)}님의 요청이 취소됐어요"
            ProposalState.PENDING, ProposalState.APPROVED -> null
        }
    }
    return WaypointPanelModel(
        waypointNames = waypoints.mapIndexed { i, d ->
            val missed = earlierFailures[i].orEmpty().map { name(it) }
            (listOf("경유지 ${i + 1}", d.name) + missed.map { "$it 미반영" }).joinToString(" · ")
        },
        request = request,
        applyText = applyText,
        applyComplete = summary.complete,
        showResend = summary.failed > 0,
        canPropose = groupActive && hasDestination && canPerform(DriverAction.PROPOSE_WAYPOINT, gear),
        notice = notice,
        canResend = summary.failed > 0 && canPerform(DriverAction.RESEND_WAYPOINT, gear),
    )
}
