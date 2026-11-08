package com.car.convoy.domain

import com.car.convoy.domain.GroupEvent.*
import com.car.convoy.domain.GroupMemberState.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SPEC R11 표에 없는 조합: 상태를 바꾸지 않고 효과도 없다.
 * 늦거나 중복된 이벤트가 상태를 임의로 바꾸지 않는지 고정한다.
 */
class GroupMemberOutOfTableTest {

    private fun ignored(state: GroupMemberState, event: GroupEvent) =
        assertEquals(MemberTransition(state, null), nextMemberState(state, event))

    @Test fun arrived_duplicate_arrival_does_not_share_again() = ignored(ARRIVED, NAVI_ARRIVED)
    @Test fun arrived_gear_arrived_is_ignored() = ignored(ARRIVED, GEAR_ARRIVED)
    @Test fun arrived_gear_departed_is_ignored() = ignored(ARRIVED, GEAR_DEPARTED)
    @Test fun arrived_route_started_is_ignored() = ignored(ARRIVED, ROUTE_STARTED)
    @Test fun driving_gear_departed_is_ignored() = ignored(DRIVING, GEAR_DEPARTED)
    @Test fun driving_route_started_is_ignored() = ignored(DRIVING, ROUTE_STARTED)
    @Test fun idle_gear_arrived_is_ignored() = ignored(IDLE, GEAR_ARRIVED)
    @Test fun idle_route_started_is_ignored() = ignored(IDLE, ROUTE_STARTED)
    @Test fun rerouting_gear_arrived_is_ignored() = ignored(REROUTING, GEAR_ARRIVED)
}
