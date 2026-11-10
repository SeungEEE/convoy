package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R10 "그룹 목적지 변경" 행(SET_GROUP_DESTINATION): 정차 중에만. PARK이 아닌 기어는 DRIVE와 같다. */
class DrivingRestrictionGroupDestinationTest {

    private fun check(gear: Gear, expected: Boolean) =
        assertEquals(expected, canPerform(DriverAction.SET_GROUP_DESTINATION, gear))

    @Test fun park_allows() = check(Gear.PARK, true)
    @Test fun drive_forbids() = check(Gear.DRIVE, false)
    @Test fun neutral_forbids() = check(Gear.NEUTRAL, false)
    @Test fun reverse_forbids() = check(Gear.REVERSE, false)
    @Test fun unknown_forbids() = check(Gear.UNKNOWN, false)
}
