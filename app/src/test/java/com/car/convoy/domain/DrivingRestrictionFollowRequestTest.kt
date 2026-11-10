package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R10 "따라가기 요청" 행(REQUEST_FOLLOW, R2-5): 정차 중에만. PARK이 아닌 기어는 DRIVE와 같다. */
class DrivingRestrictionFollowRequestTest {

    private fun check(gear: Gear, expected: Boolean) =
        assertEquals(expected, canPerform(DriverAction.REQUEST_FOLLOW, gear))

    @Test fun park_allows() = check(Gear.PARK, true)
    @Test fun drive_forbids() = check(Gear.DRIVE, false)
    @Test fun neutral_forbids() = check(Gear.NEUTRAL, false)
    @Test fun reverse_forbids() = check(Gear.REVERSE, false)
    @Test fun unknown_forbids() = check(Gear.UNKNOWN, false)
}
