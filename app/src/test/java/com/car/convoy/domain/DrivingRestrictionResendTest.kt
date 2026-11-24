package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R10 "경유지 다시 보내기" 2행(R13-7, 작업 39, AI 작성). PARK이 아닌 기어는 DRIVE와 같다. */
class DrivingRestrictionResendTest {

    @Test fun resend_waypoint_parked_true() {
        assertEquals(true, canPerform(DriverAction.RESEND_WAYPOINT, Gear.PARK))
    }

    @Test fun resend_waypoint_driving_false() {
        listOf(Gear.DRIVE, Gear.NEUTRAL, Gear.REVERSE, Gear.UNKNOWN).forEach {
            assertEquals("$it", false, canPerform(DriverAction.RESEND_WAYPOINT, it))
        }
    }
}
