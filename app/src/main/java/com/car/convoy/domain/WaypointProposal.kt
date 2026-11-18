package com.car.convoy.domain

/** SPEC R13-1~4: 경유지를 제안할 수 있는지. */
enum class ProposalCheck { OK, NOT_ACTIVE, NO_DESTINATION, PENDING_EXISTS, APPLYING, FULL, SAME_AS_DESTINATION, ALREADY_WAYPOINT }

/** SPEC R13-2: 그룹이 승인한 경유지는 최대 2개다(SDK 제한). */
private const val MAX_WAYPOINTS = 2

/** SPEC R13-3: R5-3과 같이 10m 미만이면 같은 곳이다. */
private const val SAME_PLACE_METERS = 10.0

/**
 * SPEC R13-1~4. 위에서부터 차례로 따진다.
 * [toDestination]과 [toNearestWaypoint]는 제안 장소와의 `distanceMeters`이고, 없으면 null이다.
 * [applying]은 직전 경유지의 반영 상태에 SENT가 남아 있는지다.
 */
fun checkProposal(
    group: GroupStatus,
    hasDestination: Boolean,
    waypointCount: Int,
    toDestination: Double?,
    toNearestWaypoint: Double?,
    hasPending: Boolean,
    applying: Boolean,
): ProposalCheck = when {
    group != GroupStatus.ACTIVE -> ProposalCheck.NOT_ACTIVE
    !hasDestination -> ProposalCheck.NO_DESTINATION
    hasPending -> ProposalCheck.PENDING_EXISTS
    applying -> ProposalCheck.APPLYING
    waypointCount >= MAX_WAYPOINTS -> ProposalCheck.FULL
    toDestination != null && toDestination < SAME_PLACE_METERS -> ProposalCheck.SAME_AS_DESTINATION
    toNearestWaypoint != null && toNearestWaypoint < SAME_PLACE_METERS -> ProposalCheck.ALREADY_WAYPOINT
    else -> ProposalCheck.OK
}

/** SPEC R13-4·5·14: 제안 상태. */
enum class ProposalState { PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED }

enum class ProposalEvent { LEADER_APPROVE, LEADER_REJECT, PROPOSER_CANCEL, TIMEOUT, SESSION_ENDED }

enum class ProposalEffect { SEND_WAYPOINT }

data class ProposalTransition(val state: ProposalState, val effect: ProposalEffect?)

/**
 * SPEC R13-4·5·14. 제안은 PENDING에서만 바뀐다. 표에 없는 조합은 상태를 바꾸지 않고 효과도 없다.
 * 리더가 직접 낸 제안(R13-1)은 이 함수를 거치지 않는다.
 */
fun nextProposalState(state: ProposalState, event: ProposalEvent): ProposalTransition {
    if (state != ProposalState.PENDING) return ProposalTransition(state, null)
    return when (event) {
        ProposalEvent.LEADER_APPROVE -> ProposalTransition(ProposalState.APPROVED, ProposalEffect.SEND_WAYPOINT)
        ProposalEvent.LEADER_REJECT -> ProposalTransition(ProposalState.REJECTED, null)
        ProposalEvent.PROPOSER_CANCEL, ProposalEvent.SESSION_ENDED -> ProposalTransition(ProposalState.CANCELLED, null)
        ProposalEvent.TIMEOUT -> ProposalTransition(ProposalState.EXPIRED, null)
    }
}

/** SPEC R13-6: 멤버별 반영 상태. */
enum class ApplyState { SENT, APPLIED, FAILED }

enum class ApplyEvent { WAYPOINT_ADDED, ADD_FAILED, TIMEOUT, RESEND }

/** SPEC R13-6·7. 표에 없는 조합은 상태를 바꾸지 않는다. */
fun nextApplyState(state: ApplyState, event: ApplyEvent): ApplyState = when {
    event == ApplyEvent.WAYPOINT_ADDED && state != ApplyState.APPLIED -> ApplyState.APPLIED
    state == ApplyState.SENT && (event == ApplyEvent.ADD_FAILED || event == ApplyEvent.TIMEOUT) -> ApplyState.FAILED
    state == ApplyState.FAILED && event == ApplyEvent.RESEND -> ApplyState.SENT
    else -> state
}

/**
 * SPEC R13-7: "반영 n/m"과 처리 중·실패 수. n은 [applied], m은 [targets](R13-5에서 경유지를 보낸 멤버 수).
 * [waiting]은 SENT(처리 중), [failed]는 FAILED 수다. 완료는 [targets]가 1 이상이고 모두 APPLIED일 때다.
 */
data class ApplySummary(val applied: Int, val targets: Int, val complete: Boolean, val waiting: Int, val failed: Int)

fun applySummary(states: List<ApplyState>): ApplySummary {
    val applied = states.count { it == ApplyState.APPLIED }
    return ApplySummary(
        applied = applied,
        targets = states.size,
        complete = states.isNotEmpty() && applied == states.size,
        waiting = states.count { it == ApplyState.SENT },
        failed = states.count { it == ApplyState.FAILED },
    )
}
