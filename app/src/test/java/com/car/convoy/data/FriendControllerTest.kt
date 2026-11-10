package com.car.convoy.data

import com.car.convoy.domain.FollowEvent
import com.car.convoy.domain.FollowLink
import com.car.convoy.domain.FollowState
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R2-5, R3: 가상 친구, 내 상태, 따라가기 하나를 들고 있는 컨트롤러. */
class FriendControllerTest {

    private val friends = listOf(
        Friend("f1", "민수", UserStatus.ACTIVE),
        Friend("f2", "지영", UserStatus.BUSY),
        Friend("f3", "현우", UserStatus.ACTIVE),
        Friend("f4", "서준", UserStatus.OFFLINE),
    )

    private fun controller() = FriendController(friends)

    @Test fun starts_active_with_no_follow() {
        val c = controller()
        assertEquals(UserStatus.ACTIVE, c.myStatus)
        assertNull(c.follow)
    }

    @Test fun my_status_changes() {
        val c = controller()
        c.setMyStatus(UserStatus.BUSY)
        assertEquals(UserStatus.BUSY, c.myStatus)
    }

    @Test fun request_active_friend_while_parked_becomes_requested() {
        val c = controller()
        assertTrue(c.requestFollow("f1", Gear.PARK))
        assertEquals(FollowLink("f1", FollowState.REQUESTED), c.follow)
    }

    @Test fun request_busy_friend_is_ignored() {
        val c = controller()
        assertFalse(c.requestFollow("f2", Gear.PARK))
        assertNull(c.follow)
    }

    @Test fun request_offline_friend_is_ignored() {
        val c = controller()
        assertFalse(c.requestFollow("f4", Gear.PARK))
        assertNull(c.follow)
    }

    @Test fun request_unknown_friend_is_ignored() {
        val c = controller()
        assertFalse(c.requestFollow("nobody", Gear.PARK))
        assertNull(c.follow)
    }

    @Test fun request_while_driving_is_ignored() {
        val c = controller()
        assertFalse(c.requestFollow("f1", Gear.DRIVE))
        assertNull(c.follow)
    }

    @Test fun my_status_does_not_block_request() {
        val c = controller()
        c.setMyStatus(UserStatus.OFFLINE)
        assertTrue(c.requestFollow("f1", Gear.PARK))
    }

    @Test fun second_request_while_one_in_progress_is_ignored() {
        val c = controller()
        c.requestFollow("f1", Gear.PARK)
        assertFalse(c.requestFollow("f3", Gear.PARK))
        assertEquals(FollowLink("f1", FollowState.REQUESTED), c.follow)
    }

    @Test fun friend_accepts_becomes_active() {
        val c = controller()
        c.requestFollow("f1", Gear.PARK)
        c.respond(FollowEvent.ACCEPT)
        assertEquals(FollowLink("f1", FollowState.ACTIVE), c.follow)
    }

    @Test fun friend_declines_ends_and_clears() {
        val c = controller()
        c.requestFollow("f1", Gear.PARK)
        c.respond(FollowEvent.DECLINE)
        assertNull(c.follow)
    }

    @Test fun cancel_active_ends_and_clears_even_while_driving() {
        val c = controller()
        c.requestFollow("f1", Gear.PARK)
        c.respond(FollowEvent.ACCEPT)
        c.cancelFollow()
        assertNull(c.follow)
    }

    @Test fun after_end_another_friend_can_be_requested() {
        val c = controller()
        c.requestFollow("f1", Gear.PARK)
        c.respond(FollowEvent.DECLINE)
        assertTrue(c.requestFollow("f3", Gear.PARK))
        assertEquals(FollowLink("f3", FollowState.REQUESTED), c.follow)
    }

    @Test fun respond_without_request_is_ignored() {
        val c = controller()
        c.respond(FollowEvent.ACCEPT)
        assertNull(c.follow)
    }

    @Test fun respond_cancel_is_not_a_friend_response() {
        val c = controller()
        c.requestFollow("f1", Gear.PARK)
        c.respond(FollowEvent.CANCEL)
        assertEquals(FollowLink("f1", FollowState.REQUESTED), c.follow)
    }

    @Test fun accept_again_while_active_stays_active() {
        val c = controller()
        c.requestFollow("f1", Gear.PARK)
        c.respond(FollowEvent.ACCEPT)
        c.respond(FollowEvent.ACCEPT)
        assertEquals(FollowLink("f1", FollowState.ACTIVE), c.follow)
    }
}
