package com.car.convoy.domain

import com.car.convoy.domain.GroupEvent.*
import com.car.convoy.domain.GroupMemberState.*
import com.car.convoy.domain.MemberEffect.*
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R11 nextMemberState 표 10행. */
class GroupMemberTest {

    private fun check(
        state: GroupMemberState,
        event: GroupEvent,
        expectedState: GroupMemberState,
        expectedEffect: MemberEffect?,
    ) = assertEquals(MemberTransition(expectedState, expectedEffect), nextMemberState(state, event))

    @Test fun idle_leader_sets_destination() = check(IDLE, LEADER_SET_DESTINATION, REROUTING, REQUEST_ROUTE)
    @Test fun rerouting_route_started() = check(REROUTING, ROUTE_STARTED, DRIVING, null)
    @Test fun idle_gear_departed() = check(IDLE, GEAR_DEPARTED, DRIVING, SHARE_DEPARTED)
    @Test fun driving_navi_arrived() = check(DRIVING, NAVI_ARRIVED, ARRIVED, SHARE_ARRIVED)
    @Test fun driving_gear_arrived() = check(DRIVING, GEAR_ARRIVED, ARRIVED, SHARE_ARRIVED)
    @Test fun arrived_leader_sets_destination() = check(ARRIVED, LEADER_SET_DESTINATION, REROUTING, REQUEST_ROUTE)
    @Test fun driving_leader_sets_destination() = check(DRIVING, LEADER_SET_DESTINATION, REROUTING, REQUEST_ROUTE)
    @Test fun idle_navi_arrived_is_ignored() = check(IDLE, NAVI_ARRIVED, IDLE, null)
    @Test fun rerouting_gear_departed_shares_but_keeps_state() = check(REROUTING, GEAR_DEPARTED, REROUTING, SHARE_DEPARTED)
    @Test fun rerouting_navi_arrived_is_ignored() = check(REROUTING, NAVI_ARRIVED, REROUTING, null)
}
