package com.car.convoy.data

import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R4-5: 이번 주행 그룹(초대·수락·해산). 주행이 "없음"이고 정차 중일 때만 초대·해산한다. */
class GroupRosterTest {

    private val people = listOf(
        Friend("p1", "민수", UserStatus.ACTIVE),
        Friend("p2", "현우", UserStatus.ACTIVE),
        Friend("p3", "수진", UserStatus.ACTIVE),
        Friend("p4", "지영", UserStatus.BUSY),
        Friend("p5", "서준", UserStatus.OFFLINE),
        Friend("p6", "도윤", UserStatus.ACTIVE),
    )

    private fun roster() = GroupRoster(people)
    private fun GroupRoster.inv(id: String) = invite(id, Gear.PARK, driveActive = false)

    @Test fun starts_empty_with_only_me() {
        val r = roster()
        assertEquals(1, r.headCount)
        assertTrue(r.members.isEmpty())
        assertNull(r.firstPendingInvite)
    }

    @Test fun invite_makes_pending() {
        val r = roster()
        assertTrue(r.inv("p1"))
        assertEquals(RosterState.INVITED, r.stateOf("p1"))
        assertEquals(2, r.headCount)
        assertEquals("p1", r.firstPendingInvite)
    }

    @Test fun busy_person_can_be_invited() = assertTrue(roster().inv("p4"))

    @Test fun offline_person_cannot_be_invited() {
        val r = roster()
        assertFalse(r.inv("p5"))
        assertNull(r.stateOf("p5"))
    }

    @Test fun unknown_person_cannot_be_invited() = assertFalse(roster().inv("nobody"))

    @Test fun cannot_invite_twice() {
        val r = roster()
        r.inv("p1")
        assertFalse(r.inv("p1"))
    }

    @Test fun cannot_invite_while_driving() = assertFalse(roster().invite("p1", Gear.DRIVE, driveActive = false))

    @Test fun cannot_invite_with_unknown_gear() = assertFalse(roster().invite("p1", Gear.UNKNOWN, driveActive = false))

    @Test fun cannot_invite_during_a_drive() = assertFalse(roster().invite("p1", Gear.PARK, driveActive = true))

    @Test fun pending_invites_count_toward_capacity() {
        val r = roster()
        listOf("p1", "p2", "p3", "p4").forEach { assertTrue(r.inv(it)) }   // 나 + 4 = 5
        assertFalse(r.inv("p6"))                                           // GROUP_FULL
        assertEquals(5, r.headCount)
    }

    @Test fun accept_makes_member_in_invite_order() {
        val r = roster()
        r.inv("p2"); r.inv("p1")
        r.respond("p1", accept = true)
        r.respond("p2", accept = true)
        assertEquals(listOf("p2", "p1"), r.members)
        assertEquals(RosterState.MEMBER, r.stateOf("p1"))
    }

    @Test fun decline_removes_and_can_invite_again() {
        val r = roster()
        r.inv("p1")
        r.respond("p1", accept = false)
        assertNull(r.stateOf("p1"))
        assertEquals(1, r.headCount)
        assertTrue(r.inv("p1"))
    }

    @Test fun respond_to_not_invited_is_ignored() {
        val r = roster()
        r.respond("p1", accept = true)
        assertNull(r.stateOf("p1"))
    }

    @Test fun respond_to_member_is_ignored() {
        val r = roster()
        r.inv("p1"); r.respond("p1", accept = true)
        r.respond("p1", accept = false)
        assertEquals(RosterState.MEMBER, r.stateOf("p1"))
    }

    @Test fun first_pending_skips_members() {
        val r = roster()
        r.inv("p1"); r.inv("p2"); r.respond("p1", accept = true)
        assertEquals("p2", r.firstPendingInvite)
    }

    @Test fun disband_clears_when_parked_and_no_drive() {
        val r = roster()
        r.inv("p1"); r.respond("p1", accept = true); r.inv("p2")
        assertTrue(r.disband(Gear.PARK, driveActive = false))
        assertEquals(1, r.headCount)
        assertTrue(r.members.isEmpty())
    }

    @Test fun cannot_disband_while_driving_or_during_drive() {
        val r = roster()
        r.inv("p1"); r.respond("p1", accept = true)
        assertFalse(r.disband(Gear.DRIVE, driveActive = false))
        assertFalse(r.disband(Gear.PARK, driveActive = true))
        assertEquals(listOf("p1"), r.members)
    }

    @Test fun cannot_disband_empty_group() = assertFalse(roster().disband(Gear.PARK, driveActive = false))
}
