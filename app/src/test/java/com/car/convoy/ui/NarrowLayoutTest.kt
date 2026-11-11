package com.car.convoy.ui

import com.car.convoy.domain.FollowLink
import com.car.convoy.domain.FollowState
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-20, R2-5: 좁은 창(1,200dp 미만)은 1열, 친구 화면 1열 순서. */
class NarrowLayoutTest {

    @Test fun width_rule() {
        assertTrue(isSingleColumn(850f))     // 1:1 화면 나누기
        assertTrue(isSingleColumn(1133f))    // 2:1 넓은 쪽
        assertTrue(isSingleColumn(1199.9f))
        assertFalse(isSingleColumn(1200f))   // 경계는 2열
        assertFalse(isSingleColumn(1707f))   // 앱 하나
    }

    private val friends = listOf(
        Friend("f1", "민수", UserStatus.ACTIVE),
        Friend("f2", "지영", UserStatus.BUSY),
        Friend("f3", "현우", UserStatus.ACTIVE),
        Friend("f4", "서준", UserStatus.OFFLINE),
    )

    private fun order(follow: FollowLink?) =
        friendSingleColumnOrder(friendScreenModel(friends, UserStatus.ACTIVE, follow, Gear.PARK)).map {
            when (it) {
                is FriendSlot.FriendItem -> it.row.id
                FriendSlot.DemoPanel -> "demo"
                FriendSlot.MyStatus -> "status"
            }
        }

    @Test fun no_follow_status_then_friends() =
        assertEquals(listOf("status", "f1", "f2", "f3"), order(null))

    @Test fun requested_friend_then_demo_then_status_then_rest() =
        assertEquals(listOf("f3", "demo", "status", "f1", "f2"), order(FollowLink("f3", FollowState.REQUESTED)))

    @Test fun active_friend_then_status_then_rest() =
        assertEquals(listOf("f3", "status", "f1", "f2"), order(FollowLink("f3", FollowState.ACTIVE)))
}
