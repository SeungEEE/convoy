package com.car.convoy.ui

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

/** SPEC R2-5: 친구 목록 화면에 보일 값. */
class FriendScreenModelTest {

    private val friends = listOf(
        Friend("f1", "민수", UserStatus.ACTIVE),
        Friend("f2", "지영", UserStatus.BUSY),
        Friend("f3", "현우", UserStatus.ACTIVE),
        Friend("f4", "서준", UserStatus.OFFLINE),
    )

    private fun model(
        myStatus: UserStatus = UserStatus.ACTIVE,
        follow: FollowLink? = null,
        gear: Gear = Gear.PARK,
    ) = friendScreenModel(friends, myStatus, follow, gear)

    private fun FriendScreenModel.row(id: String) = rows.first { it.id == id }

    @Test fun status_labels() {
        assertEquals("활동 중", userStatusLabel(UserStatus.ACTIVE))
        assertEquals("업무 중", userStatusLabel(UserStatus.BUSY))
        assertEquals("오프라인", userStatusLabel(UserStatus.OFFLINE))
    }

    @Test fun my_status_is_shown() {
        assertEquals(UserStatus.BUSY, model(myStatus = UserStatus.BUSY).myStatus)
    }

    @Test fun offline_friend_is_hidden_and_order_kept() {
        assertEquals(listOf("f1", "f2", "f3"), model().rows.map { it.id })
    }

    @Test fun row_has_name_status_and_label() {
        val r = model().row("f2")
        assertEquals("지영", r.name)
        assertEquals(UserStatus.BUSY, r.status)
        assertEquals("업무 중", r.statusLabel)
    }

    @Test fun active_friend_parked_can_request_without_note() {
        val r = model().row("f1")
        assertTrue(r.canRequest)
        assertNull(r.note)
        assertNull(r.followLabel)
        assertFalse(r.showCancel)
    }

    @Test fun busy_friend_cannot_request_with_note() {
        val r = model().row("f2")
        assertFalse(r.canRequest)
        assertEquals("업무 중이라 요청할 수 없어요", r.note)
    }

    @Test fun driving_disables_request_with_note() {
        val r = model(gear = Gear.DRIVE).row("f1")
        assertFalse(r.canRequest)
        assertEquals("정차 중에만 요청할 수 있어요", r.note)
    }

    @Test fun busy_note_wins_over_driving_note() {
        assertEquals("업무 중이라 요청할 수 없어요", model(gear = Gear.DRIVE).row("f2").note)
    }

    @Test fun my_status_does_not_block_request() {
        assertTrue(model(myStatus = UserStatus.OFFLINE).row("f1").canRequest)
    }

    @Test fun requested_row_shows_waiting_and_no_cancel() {
        val r = model(follow = FollowLink("f1", FollowState.REQUESTED)).row("f1")
        assertEquals("수락 기다리는 중", r.followLabel)
        assertFalse(r.canRequest)
        assertFalse(r.showCancel)
    }

    @Test fun active_row_shows_following_and_cancel() {
        val r = model(follow = FollowLink("f1", FollowState.ACTIVE)).row("f1")
        assertEquals("따라가는 중", r.followLabel)
        assertFalse(r.canRequest)
        assertTrue(r.showCancel)
    }

    @Test fun cancel_shown_while_driving() {
        assertTrue(model(follow = FollowLink("f1", FollowState.ACTIVE), gear = Gear.DRIVE).row("f1").showCancel)
    }

    @Test fun other_friends_disabled_while_one_in_progress() {
        val m = model(follow = FollowLink("f1", FollowState.REQUESTED))
        assertFalse(m.row("f3").canRequest)
        assertNull(m.row("f3").followLabel)
        assertFalse(model(follow = FollowLink("f1", FollowState.ACTIVE)).row("f3").canRequest)
    }

    @Test fun ended_link_frees_all_buttons() {
        val m = model(follow = FollowLink("f1", FollowState.ENDED))
        assertTrue(m.row("f1").canRequest)
        assertTrue(m.row("f3").canRequest)
        assertNull(m.row("f1").followLabel)
    }

    @Test fun demo_panel_only_while_requested() {
        assertNull(model().pendingRequest)
        assertEquals(PendingRequest("f1", "민수"), model(follow = FollowLink("f1", FollowState.REQUESTED)).pendingRequest)
        assertNull(model(follow = FollowLink("f1", FollowState.ACTIVE)).pendingRequest)
    }
}
