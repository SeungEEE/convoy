package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R10 "주행 끝내기", "그룹 해산" 행(R4-5, R11-20, 작업 23). PARK이 아닌 기어는 DRIVE와 같다. */
class DrivingRestrictionHomeTest {

    @Test fun end_drive_allowed_always() {
        Gear.entries.forEach { assertEquals("$it", true, canPerform(DriverAction.END_DRIVE, it)) }
    }

    @Test fun disband_only_parked() {
        assertEquals(true, canPerform(DriverAction.DISBAND_GROUP, Gear.PARK))
        listOf(Gear.DRIVE, Gear.NEUTRAL, Gear.REVERSE, Gear.UNKNOWN).forEach {
            assertEquals("$it", false, canPerform(DriverAction.DISBAND_GROUP, it))
        }
    }
}
