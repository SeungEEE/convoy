package com.car.convoy.domain

import com.car.convoy.domain.GroupEvent.*
import com.car.convoy.domain.GroupMemberState.*
import com.car.convoy.domain.MemberEffect.*
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R11 nextMemberState 표 11행: 재탐색 중에 목적지가 다시 바뀌면 새 목적지로 다시 요청한다. */
class GroupMemberRerouteTest {

    @Test fun rerouting_leader_sets_destination_again_requests_route() =
        assertEquals(
            MemberTransition(REROUTING, REQUEST_ROUTE),
            nextMemberState(REROUTING, LEADER_SET_DESTINATION),
        )
}
