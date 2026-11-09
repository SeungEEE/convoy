package com.car.convoy.domain

import com.car.convoy.domain.port.ConvoyServer

/** SPEC R11-15. 그룹 상태. */
enum class GroupStatus { ACTIVE, FINISHED }

/** SPEC R11-5. 그룹 단위 효과. */
enum class GroupEffect { FINISH_GROUP }

/** SPEC R11-15. 그룹 판단 결과: 다음 그룹 상태와 효과(없으면 null). */
data class GroupTransition(val status: GroupStatus, val effect: GroupEffect?)

/** 멤버 이벤트 하나를 적용한 결과: 그 멤버의 효과와 그룹 효과(없으면 각각 null). */
data class MemberEventResult(val memberEffect: MemberEffect?, val groupEffect: GroupEffect?)

/** SPEC R11-7: ARRIVED는 ARRIVED, 나머지는 모두 DEPARTED로 바꿔 R7의 isGroupFinished에 넘긴다. */
fun GroupMemberState.toArrivalState(): MemberState =
    if (this == GroupMemberState.ARRIVED) MemberState.ARRIVED else MemberState.DEPARTED

/** SPEC R7-4, R11-5, R11-15: ACTIVE에서 전원 도착이면 FINISH_GROUP을 한 번 내고 FINISHED가 된다. */
fun nextGroupStatus(status: GroupStatus, memberStates: List<GroupMemberState>): GroupTransition =
    if (status == GroupStatus.ACTIVE && isGroupFinished(memberStates.map { it.toArrivalState() })) {
        GroupTransition(GroupStatus.FINISHED, GroupEffect.FINISH_GROUP)
    } else {
        GroupTransition(status, null)
    }

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

    /**
     * SPEC R11, R11-5, R11-15: 멤버 이벤트를 표대로 적용하고 그룹 종료를 판단한다.
     * FINISHED면 모든 이벤트를 무시한다. 목적지 변경은 [setDestination]으로만 한다.
     */
    fun onMemberEvent(memberId: String, event: GroupEvent): MemberEventResult {
        require(event != GroupEvent.LEADER_SET_DESTINATION) { "목적지 변경은 setDestination으로 한다" }
        if (status == GroupStatus.FINISHED) return MemberEventResult(null, null)

        val transition = nextMemberState(states.getValue(memberId), event)
        states[memberId] = transition.state
        val group = nextGroupStatus(status, states.values.toList())
        status = group.status
        return MemberEventResult(transition.effect, group.effect)
    }

    private fun syncResult(requesterId: String, newDestination: Destination): DestinationSyncResult {
        val isLeader = requesterId == leaderId
        val current = destination
        // 첫 목적지는 거리 비교 없이 SYNC다(R11-12). 리더가 아니면 첫 목적지라도 NOT_LEADER다.
        if (current == null) return if (isLeader) DestinationSyncResult.SYNC else DestinationSyncResult.NOT_LEADER
        return shouldSyncDestination(isLeader, distanceMeters(current, newDestination))
    }
}
