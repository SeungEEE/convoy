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
