package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R2 예시표 전체(작업 3). */
class FriendListTest {

    @Test
    fun active_is_visible_and_can_be_followed() {
        assertEquals(true, visibleInFriendList(UserStatus.ACTIVE))
        assertEquals(FollowRequestResult.OK, canRequestFollow(UserStatus.ACTIVE))
    }

    @Test
    fun busy_is_visible_but_rejects_follow() {
        assertEquals(true, visibleInFriendList(UserStatus.BUSY))
        assertEquals(FollowRequestResult.REJECT_BUSY, canRequestFollow(UserStatus.BUSY))
    }

    @Test
    fun offline_is_hidden_and_rejects_follow() {
        assertEquals(false, visibleInFriendList(UserStatus.OFFLINE))
        assertEquals(FollowRequestResult.REJECT_OFFLINE, canRequestFollow(UserStatus.OFFLINE))
    }
}
