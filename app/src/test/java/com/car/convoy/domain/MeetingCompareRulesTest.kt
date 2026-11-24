package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R14-20 `proposalNotice` 8행, `canOpenMeetingCompare` 4행 (작업 37, AI 작성). */
class MeetingCompareRulesTest {

    @Test fun ok_has_no_notice() = assertNull(proposalNotice(ProposalCheck.OK))

    @Test fun full() = assertEquals("경유지는 2개까지 넣을 수 있어요", proposalNotice(ProposalCheck.FULL))

    @Test fun pending_exists() = assertEquals("답하지 않은 요청이 있어요", proposalNotice(ProposalCheck.PENDING_EXISTS))

    @Test fun applying() = assertEquals("앞 경유지를 반영하는 중이에요", proposalNotice(ProposalCheck.APPLYING))

    @Test fun same_as_destination() = assertEquals("목적지와 같은 곳이에요", proposalNotice(ProposalCheck.SAME_AS_DESTINATION))

    @Test fun already_waypoint() = assertEquals("이미 들르는 곳이에요", proposalNotice(ProposalCheck.ALREADY_WAYPOINT))

    @Test fun no_destination() = assertEquals("지금은 경유지를 넣을 수 없어요", proposalNotice(ProposalCheck.NO_DESTINATION))

    @Test fun not_active() = assertEquals("지금은 경유지를 넣을 수 없어요", proposalNotice(ProposalCheck.NOT_ACTIVE))

    @Test fun open_when_active_and_parked() = assertTrue(canOpenMeetingCompare(GroupStatus.ACTIVE, Gear.PARK))

    @Test fun locked_while_driving() = assertFalse(canOpenMeetingCompare(GroupStatus.ACTIVE, Gear.DRIVE))

    @Test fun locked_when_finished() = assertFalse(canOpenMeetingCompare(GroupStatus.FINISHED, Gear.PARK))

    @Test fun unknown_gear_is_driving() = assertFalse(canOpenMeetingCompare(GroupStatus.ACTIVE, Gear.UNKNOWN))
}
