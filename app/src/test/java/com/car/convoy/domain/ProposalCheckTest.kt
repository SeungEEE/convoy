package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SPEC R13-1~4 `checkProposal` 예시표 11행(AI 작성).
 * 판단 순서: NOT_ACTIVE → NO_DESTINATION → PENDING_EXISTS → APPLYING → FULL → SAME_AS_DESTINATION → ALREADY_WAYPOINT → OK.
 */
class ProposalCheckTest {

    private fun check(
        group: GroupStatus = GroupStatus.ACTIVE,
        hasDestination: Boolean = true,
        waypoints: Int = 0,
        toDestination: Double? = 500.0,
        toNearestWaypoint: Double? = null,
        pending: Boolean = false,
        applying: Boolean = false,
    ) = checkProposal(group, hasDestination, waypoints, toDestination, toNearestWaypoint, pending, applying)

    @Test
    fun no_waypoint_new_place_ok() {
        assertEquals(ProposalCheck.OK, check())
    }

    @Test
    fun one_waypoint_new_place_ok() {
        assertEquals(ProposalCheck.OK, check(waypoints = 1, toNearestWaypoint = 500.0))
    }

    @Test
    fun two_waypoints_full() {
        assertEquals(ProposalCheck.FULL, check(waypoints = 2, toNearestWaypoint = 500.0))
    }

    @Test
    fun no_destination() {
        assertEquals(ProposalCheck.NO_DESTINATION, check(hasDestination = false, toDestination = null))
    }

    @Test
    fun finished_group_not_active() {
        assertEquals(ProposalCheck.NOT_ACTIVE, check(group = GroupStatus.FINISHED))
    }

    @Test
    fun near_destination_same() {
        assertEquals(ProposalCheck.SAME_AS_DESTINATION, check(toDestination = 8.9))
    }

    @Test
    fun exactly_10m_from_destination_ok() {
        assertEquals(ProposalCheck.OK, check(toDestination = 10.0))
    }

    @Test
    fun near_waypoint_already() {
        assertEquals(ProposalCheck.ALREADY_WAYPOINT, check(waypoints = 1, toNearestWaypoint = 8.9))
    }

    @Test
    fun pending_exists() {
        assertEquals(ProposalCheck.PENDING_EXISTS, check(pending = true))
    }

    @Test
    fun pending_checked_before_full() {
        assertEquals(ProposalCheck.PENDING_EXISTS, check(waypoints = 2, toNearestWaypoint = 500.0, pending = true))
    }

    @Test
    fun applying_blocks() {
        assertEquals(ProposalCheck.APPLYING, check(waypoints = 1, toNearestWaypoint = 500.0, applying = true))
    }
}
