package com.car.convoy.ui

import com.car.convoy.data.GroupRoster
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

/** SPEC R12-1, R4-5(작업 25): 홈의 [앞차 따라가기] 선택. 수락한 멤버만, 업무 중이면 고를 수 없다. 요청 대기도 진행 중인 주행이다. */
class HomeFollowModelTest {

    private val people = listOf(
        Friend("p1", "민수", UserStatus.ACTIVE),
        Friend("p2", "현우", UserStatus.ACTIVE),
        Friend("p4", "지영", UserStatus.BUSY),
    )

    private fun roster(vararg members: String, pending: List<String> = emptyList()) = GroupRoster(people).apply {
        members.forEach { invite(it, Gear.PARK, false); respond(it, accept = true) }
        pending.forEach { invite(it, Gear.PARK, false) }
    }

    private fun model(r: GroupRoster, follow: FollowLink? = null, gear: Gear = Gear.PARK, driveActive: Boolean = false) =
        homeScreenModel(people, UserStatus.ACTIVE, r, driveActive = driveActive, gear = gear, follow = follow)

    @Test fun choices_are_accepted_members_only() =
        assertEquals(listOf("p1", "p4"), model(roster("p1", "p4", pending = listOf("p2"))).followChoices.map { it.id })

    @Test fun active_member_can_be_followed() {
        val c = model(roster("p1")).followChoices.single()
        assertTrue(c.canFollow)
        assertNull(c.note)
    }

    @Test fun busy_member_cannot_be_followed() {
        val c = model(roster("p4")).followChoices.single()
        assertFalse(c.canFollow)
        assertEquals("업무 중이라 따라갈 수 없어요", c.note)
    }

    @Test fun driving_or_drive_active_blocks_follow() {
        assertFalse(model(roster("p1"), gear = Gear.DRIVE).followChoices.single().canFollow)
        assertFalse(model(roster("p1"), driveActive = true).followChoices.single().canFollow)
    }

    @Test fun requested_shows_waiting_and_blocks_other_starts() {
        val m = model(roster("p1", "p2"), follow = FollowLink("p1", FollowState.REQUESTED))
        assertEquals("민수님 수락 대기", m.followWaiting)
        assertFalse(m.canStartDestination)
        assertTrue(m.followChoices.none { it.canFollow })
        assertEquals(PendingInvite("p1", "민수"), m.pendingFollow)
        assertFalse(m.showResume)
    }

    @Test fun active_follow_shows_resume() {
        val m = model(roster("p1"), follow = FollowLink("p1", FollowState.ACTIVE))
        assertTrue(m.showResume)
        assertEquals("민수 차를 따라가는 중이에요", m.resumeLabel)
        assertNull(m.pendingFollow)
    }

    @Test fun destination_drive_resume_label() =
        assertEquals("목적지로 함께 가는 중이에요", model(roster("p1"), driveActive = true).resumeLabel)

    @Test fun pending_follow_makes_demo_slot() =
        assertEquals(
            listOf(HomeSlot.DEPARTURE, HomeSlot.DEMO, HomeSlot.PEOPLE),
            homeSingleColumnOrder(model(roster("p1"), follow = FollowLink("p1", FollowState.REQUESTED))),
        )
}
