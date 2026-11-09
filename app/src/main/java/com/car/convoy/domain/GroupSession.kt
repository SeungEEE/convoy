package com.car.convoy.domain

import com.car.convoy.data.ConvoyServer

/** SPEC R11-15. 그룹 상태. */
enum class GroupStatus { ACTIVE, FINISHED }

/**
 * SPEC R11-11~15. 그룹 하나의 목적지와 멤버 상태.
 * [memberIds]에는 리더도 들어간다(R11-11).
 */
class GroupSession(
    private val leaderId: String,
    memberIds: List<String>,
    private val server: ConvoyServer,
    destination: Destination? = null,
    status: GroupStatus = GroupStatus.ACTIVE,
) {
    init {
        require(leaderId in memberIds) { "memberIds에 리더($leaderId)가 없다 (R11-11)" }
        require(memberIds.toSet().size == memberIds.size) { "memberIds에 중복 ID가 있다: $memberIds" }
    }

    var destination: Destination? = destination
        private set

    var status: GroupStatus = status
        private set

    private val states: MutableMap<String, GroupMemberState> =
        memberIds.associateWith { GroupMemberState.IDLE }.toMutableMap()

    fun memberState(memberId: String): GroupMemberState = states.getValue(memberId)

    /**
     * SPEC R5, R11-1, R11-9, R11-12, R11-14: 리더가 목적지를 바꾸면 SYNC일 때만
     * 리더를 포함한 모든 멤버에게 서버로 목적지를 보내고, 멤버 상태를 표대로 바꾼다.
     */
    fun setDestination(requesterId: String, newDestination: Destination) {
        if (status == GroupStatus.FINISHED) return
        if (syncResult(requesterId, newDestination) != DestinationSyncResult.SYNC) return

        destination = newDestination
        for (memberId in states.keys) {
            val transition = nextMemberState(states.getValue(memberId), GroupEvent.LEADER_SET_DESTINATION)
            states[memberId] = transition.state
            if (transition.effect == MemberEffect.REQUEST_ROUTE) server.sendDestination(memberId, newDestination)
        }
    }

    private fun syncResult(requesterId: String, newDestination: Destination): DestinationSyncResult {
        val isLeader = requesterId == leaderId
        val current = destination
        // 첫 목적지는 거리 비교 없이 SYNC다(R11-12). 리더가 아니면 첫 목적지라도 NOT_LEADER다.
        if (current == null) return if (isLeader) DestinationSyncResult.SYNC else DestinationSyncResult.NOT_LEADER
        return shouldSyncDestination(isLeader, distanceMeters(current, newDestination))
    }
}
