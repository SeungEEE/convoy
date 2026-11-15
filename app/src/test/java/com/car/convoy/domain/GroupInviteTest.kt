package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R4 예시표 전체(작업 5). */
class GroupInviteTest {

    @Test
    fun one_member_friend_is_ok() {
        assertEquals(InviteResult.OK, canInvite(memberCount = 1, isFriend = true))
    }

    @Test
    fun four_members_friend_is_ok() {
        assertEquals(InviteResult.OK, canInvite(memberCount = 4, isFriend = true))
    }

    @Test
    fun five_members_friend_is_full() {
        assertEquals(InviteResult.GROUP_FULL, canInvite(memberCount = 5, isFriend = true))
    }

    @Test
    fun one_member_not_friend_is_not_friend() {
        assertEquals(InviteResult.NOT_FRIEND, canInvite(memberCount = 1, isFriend = false))
    }

    @Test
    fun five_members_not_friend_checks_friend_first() {
        assertEquals(InviteResult.NOT_FRIEND, canInvite(memberCount = 5, isFriend = false))
    }

    @Test
    fun no_current_group_can_accept() {
        assertEquals(AcceptInviteResult.OK, canAcceptInvite(currentGroupId = null))
    }

    @Test
    fun current_group_rejects_accept() {
        assertEquals(AcceptInviteResult.ALREADY_IN_GROUP, canAcceptInvite(currentGroupId = "g1"))
    }
}
