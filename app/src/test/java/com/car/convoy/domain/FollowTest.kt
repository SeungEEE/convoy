package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R3 예시표 전체(작업 4). */
class FollowTest {

    @Test
    fun requested_accept_becomes_active() {
        assertEquals(FollowState.ACTIVE, nextFollowState(FollowState.REQUESTED, FollowEvent.ACCEPT))
    }

    @Test
    fun requested_decline_becomes_ended() {
        assertEquals(FollowState.ENDED, nextFollowState(FollowState.REQUESTED, FollowEvent.DECLINE))
    }

    @Test
    fun requested_cancel_becomes_ended() {
        assertEquals(FollowState.ENDED, nextFollowState(FollowState.REQUESTED, FollowEvent.CANCEL))
    }

    @Test
    fun active_cancel_becomes_ended() {
        assertEquals(FollowState.ENDED, nextFollowState(FollowState.ACTIVE, FollowEvent.CANCEL))
    }

    @Test
    fun active_accept_is_ignored() {
        assertEquals(FollowState.ACTIVE, nextFollowState(FollowState.ACTIVE, FollowEvent.ACCEPT))
    }

    @Test
    fun ended_accept_is_ignored() {
        assertEquals(FollowState.ENDED, nextFollowState(FollowState.ENDED, FollowEvent.ACCEPT))
    }
}
