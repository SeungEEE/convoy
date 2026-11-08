package com.car.convoy.domain

/** SPEC R11. 그룹 멤버 한 명의 진행 상태. R7의 MemberState와는 별도 타입이다(R11-7). */
enum class GroupMemberState { IDLE, REROUTING, DRIVING, ARRIVED }

/** SPEC R11. 멤버 상태를 바꾸는 이벤트. */
enum class GroupEvent { LEADER_SET_DESTINATION, ROUTE_STARTED, GEAR_DEPARTED, NAVI_ARRIVED, GEAR_ARRIVED }

/** SPEC R11. 상태 전이가 요구하는 일. 없으면 null이다. */
enum class MemberEffect { REQUEST_ROUTE, SHARE_DEPARTED, SHARE_ARRIVED }

/** SPEC R11. 전이 결과: 다음 상태와 해야 할 일(없으면 null). */
data class MemberTransition(val state: GroupMemberState, val effect: MemberEffect?)

/** SPEC R11: 표에 없는 조합은 상태를 바꾸지 않고 효과도 없다. */
fun nextMemberState(state: GroupMemberState, event: GroupEvent): MemberTransition =
    when (state to event) {
        GroupMemberState.IDLE to GroupEvent.LEADER_SET_DESTINATION,
        GroupMemberState.DRIVING to GroupEvent.LEADER_SET_DESTINATION,
        GroupMemberState.ARRIVED to GroupEvent.LEADER_SET_DESTINATION ->
            MemberTransition(GroupMemberState.REROUTING, MemberEffect.REQUEST_ROUTE)

        GroupMemberState.REROUTING to GroupEvent.ROUTE_STARTED ->
            MemberTransition(GroupMemberState.DRIVING, null)

        GroupMemberState.IDLE to GroupEvent.GEAR_DEPARTED ->
            MemberTransition(GroupMemberState.DRIVING, MemberEffect.SHARE_DEPARTED)

        // 재탐색 중 출발은 공유하되, 경로가 시작되기 전이라 상태는 그대로 둔다(R11-3).
        GroupMemberState.REROUTING to GroupEvent.GEAR_DEPARTED ->
            MemberTransition(GroupMemberState.REROUTING, MemberEffect.SHARE_DEPARTED)

        GroupMemberState.DRIVING to GroupEvent.NAVI_ARRIVED,
        GroupMemberState.DRIVING to GroupEvent.GEAR_ARRIVED ->
            MemberTransition(GroupMemberState.ARRIVED, MemberEffect.SHARE_ARRIVED)

        else -> MemberTransition(state, null)
    }
