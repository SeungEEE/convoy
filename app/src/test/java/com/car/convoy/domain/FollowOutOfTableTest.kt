package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SPEC R3 표에 없는 조합. R9처럼 표가 정하지 않은 경우는 상태를 바꾸지 않는다는 결정을 고정한다.
 * (SPEC에 이 규칙을 적기 전까지의 회귀 테스트)
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
