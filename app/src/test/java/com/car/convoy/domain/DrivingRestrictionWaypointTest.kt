package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R10 경유지 3행(R13-11, 작업 30, AI 작성). PARK이 아닌 기어는 DRIVE와 같다. */
class DrivingRestrictionWaypointTest {

    @Test fun propose_waypoint_parked_true() {
        assertEquals(true, canPerform(DriverAction.PROPOSE_WAYPOINT, Gear.PARK))
    }

    @Test fun propose_waypoint_driving_false() {
        listOf(Gear.DRIVE, Gear.NEUTRAL, Gear.REVERSE, Gear.UNKNOWN).forEach {
            assertEquals("$it", false, canPerform(DriverAction.PROPOSE_WAYPOINT, it))
        }
    }

    @Test fun respond_proposal_parked_true() {
        assertEquals(true, canPerform(DriverAction.RESPOND_WAYPOINT_PROPOSAL, Gear.PARK))
    }

    @Test fun respond_proposal_driving_true() {
        assertEquals(true, canPerform(DriverAction.RESPOND_WAYPOINT_PROPOSAL, Gear.DRIVE))
    }

    @Test fun cancel_proposal_parked_true() {
        assertEquals(true, canPerform(DriverAction.CANCEL_WAYPOINT_PROPOSAL, Gear.PARK))
    }

    @Test fun cancel_proposal_driving_true() {
        assertEquals(true, canPerform(DriverAction.CANCEL_WAYPOINT_PROPOSAL, Gear.DRIVE))
    }
}
