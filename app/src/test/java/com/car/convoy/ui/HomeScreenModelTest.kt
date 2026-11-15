package com.car.convoy.ui

import com.car.convoy.data.GroupRoster
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R2-5, R4-5: 홈에 보일 값(작업 23). 가족 카드는 작업 24, 앞차 따라가기는 작업 25에서 붙는다. */
class HomeScreenModelTest {

    private val people = listOf(
        Friend("p1", "민수", UserStatus.ACTIVE),
        Friend("p2", "현우", UserStatus.ACTIVE),
        Friend("p3", "수진", UserStatus.ACTIVE),
        Friend("p4", "지영", UserStatus.BUSY),
        Friend("p5", "서준", UserStatus.OFFLINE),
    )

    private fun model(
        roster: GroupRoster = GroupRoster(people),
        myStatus: UserStatus = UserStatus.ACTIVE,
        driveActive: Boolean = false,
        gear: Gear = Gear.PARK,
    ) = homeScreenModel(people, myStatus, roster, driveActive, gear)

    private fun HomeScreenModel.row(id: String) = this.people.first { it.id == id }

    private fun rosterWith(vararg members: String, pending: List<String> = emptyList()) = GroupRoster(people).apply {
        members.forEach { invite(it, Gear.PARK, false); respond(it, accept = true) }
        pending.forEach { invite(it, Gear.PARK, false) }
    }

    @Test fun my_status_shown() = assertEquals(UserStatus.BUSY, model(myStatus = UserStatus.BUSY).myStatus)

    @Test fun offline_hidden_order_kept() =
        assertEquals(listOf("p1", "p2", "p3", "p4"), model().people.map { it.id })

    @Test fun person_row_name_and_status() {
        val r = model().row("p4")
        assertEquals("지영", r.name)
        assertEquals("업무 중", r.statusLabel)
        assertEquals(UserStatus.BUSY, r.status)
    }

    @Test fun outside_group_shows_invite_including_busy() {
        val m = model()
        assertNull(m.row("p1").groupLabel)
        assertTrue(m.row("p1").canInvite)
        assertTrue(m.row("p4").canInvite)
    }

    @Test fun pending_and_member_labels() {
        val m = model(rosterWith("p2", pending = listOf("p3")))
        assertEquals("그룹 멤버", m.row("p2").groupLabel)
        assertEquals("수락 대기", m.row("p3").groupLabel)
        assertFalse(m.row("p2").canInvite)
        assertFalse(m.row("p3").canInvite)
    }

    @Test fun invite_disabled_while_driving_or_during_drive() {
        assertFalse(model(gear = Gear.DRIVE).row("p1").canInvite)
        assertFalse(model(driveActive = true).row("p1").canInvite)
    }

    @Test fun invite_disabled_when_full() {
        val m = model(rosterWith("p1", "p2", pending = listOf("p3", "p4")))
        assertTrue(m.people.none { it.canInvite })
    }

    @Test fun departure_chips_me_members_then_pending() {
        val chips = model(rosterWith("p2", "p1", pending = listOf("p3"))).departure
        assertEquals(listOf("나", "현우", "민수", "수진"), chips.map { it.name })
        assertEquals(listOf(true, false, false, false), chips.map { it.isLeader })
        assertEquals(listOf(false, false, false, true), chips.map { it.pending })
    }

    @Test fun start_needs_a_member_and_park() {
        assertFalse(model().canStartDestination)
        assertFalse(model(rosterWith(pending = listOf("p1"))).canStartDestination)
        assertTrue(model(rosterWith("p1")).canStartDestination)
        assertFalse(model(rosterWith("p1"), gear = Gear.DRIVE).canStartDestination)
    }

    @Test fun drive_active_shows_resume_instead_of_start() {
        val m = model(rosterWith("p1"), driveActive = true)
        assertFalse(m.canStartDestination)
        assertTrue(m.showResume)
        assertFalse(model(rosterWith("p1")).showResume)
    }

    @Test fun disband_rules() {
        assertFalse(model().canDisband)
        assertTrue(model(rosterWith(pending = listOf("p1"))).canDisband)
        assertFalse(model(rosterWith("p1"), gear = Gear.DRIVE).canDisband)
        assertFalse(model(rosterWith("p1"), driveActive = true).canDisband)
    }

    @Test fun demo_panel_shows_first_pending_invite() {
        assertNull(model().pendingInvite)
        assertEquals(PendingInvite("p3", "수진"), model(rosterWith("p2", pending = listOf("p3", "p1"))).pendingInvite)
    }

    @Test fun single_column_order() =
        assertEquals(listOf(HomeSlot.DEPARTURE, HomeSlot.DEMO, HomeSlot.PEOPLE), homeSingleColumnOrder(model(rosterWith(pending = listOf("p1")))))

    @Test fun single_column_order_without_demo() =
        assertEquals(listOf(HomeSlot.DEPARTURE, HomeSlot.PEOPLE), homeSingleColumnOrder(model()))
}
