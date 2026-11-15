package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SPEC R3-5: 표에 없는 조합은 상태를 바꾸지 않는다.
 */
class FollowOutOfTableTest {

    @Test
    fun active_decline_stays_active() {
        assertEquals(FollowState.ACTIVE, nextFollowState(FollowState.ACTIVE, FollowEvent.DECLINE))
    }

    @Test
    fun ended_decline_stays_ended() {
        assertEquals(FollowState.ENDED, nextFollowState(FollowState.ENDED, FollowEvent.DECLINE))
    }

    @Test
    fun ended_cancel_stays_ended() {
        assertEquals(FollowState.ENDED, nextFollowState(FollowState.ENDED, FollowEvent.CANCEL))
    }
}
