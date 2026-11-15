package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R2-5: [따라가기 요청]을 누를 수 있는가 = canRequestFollow OK + R10 정차 중 + 진행 중인 따라가기 없음. */
class FollowRequestGateTest {

    @Test fun active_parked_none_in_progress_allows() =
        assertEquals(true, canStartFollowRequest(UserStatus.ACTIVE, Gear.PARK, followInProgress = false))

    @Test fun busy_forbids() =
        assertEquals(false, canStartFollowRequest(UserStatus.BUSY, Gear.PARK, followInProgress = false))

    @Test fun offline_forbids() =
        assertEquals(false, canStartFollowRequest(UserStatus.OFFLINE, Gear.PARK, followInProgress = false))

    @Test fun driving_forbids() =
        assertEquals(false, canStartFollowRequest(UserStatus.ACTIVE, Gear.DRIVE, followInProgress = false))

    @Test fun unknown_gear_forbids() =
        assertEquals(false, canStartFollowRequest(UserStatus.ACTIVE, Gear.UNKNOWN, followInProgress = false))

    @Test fun another_follow_in_progress_forbids() =
        assertEquals(false, canStartFollowRequest(UserStatus.ACTIVE, Gear.PARK, followInProgress = true))
}
