package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R14-12 `friendMarkerKind` 6행, `friendLocateEnabled` 6행 (작업 40, AI 작성). */
class FriendMapRulesTest {

    @Test fun visible_located_friend_is_gray() =
        assertEquals(FriendMarker.FRIEND, friendMarkerKind(visible = true, hasLocation = true, isGroupMember = false))

    @Test fun visible_located_member_is_navy() =
        assertEquals(FriendMarker.GROUP_MEMBER, friendMarkerKind(visible = true, hasLocation = true, isGroupMember = true))

    @Test fun friend_without_location_not_drawn() =
        assertEquals(FriendMarker.NONE, friendMarkerKind(visible = true, hasLocation = false, isGroupMember = false))

    @Test fun member_without_location_not_drawn() =
        assertEquals(FriendMarker.NONE, friendMarkerKind(visible = true, hasLocation = false, isGroupMember = true))

    @Test fun offline_friend_not_drawn() =
        assertEquals(FriendMarker.NONE, friendMarkerKind(visible = false, hasLocation = true, isGroupMember = false))

    @Test fun offline_member_not_drawn() =
        assertEquals(FriendMarker.NONE, friendMarkerKind(visible = false, hasLocation = true, isGroupMember = true))

    @Test fun locate_after_tab_opened() =
        assertTrue(friendLocateEnabled(tabOpenedAt = 100, myLocationAt = 150, mapReady = true, hasMyPoint = true))

    @Test fun locate_same_time_counts() =
        assertTrue(friendLocateEnabled(tabOpenedAt = 100, myLocationAt = 100, mapReady = true, hasMyPoint = true))

    @Test fun location_before_tab_opened_locked() =
        assertFalse(friendLocateEnabled(tabOpenedAt = 100, myLocationAt = 99, mapReady = true, hasMyPoint = true))

    @Test fun no_location_locked() =
        assertFalse(friendLocateEnabled(tabOpenedAt = 100, myLocationAt = null, mapReady = true, hasMyPoint = false))

    @Test fun map_not_ready_locked() =
        assertFalse(friendLocateEnabled(tabOpenedAt = 100, myLocationAt = 150, mapReady = false, hasMyPoint = true))

    @Test fun no_point_locked() =
        assertFalse(friendLocateEnabled(tabOpenedAt = 100, myLocationAt = 150, mapReady = true, hasMyPoint = false))
}
