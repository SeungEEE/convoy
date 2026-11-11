package com.car.convoy.data

import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R9-1, R9-2, R9-5(작업 24): 운전자가 연결된 사람 한 명을 가족으로 지정하고, 그 사람이 수락한다. */
class FamilyControllerTest {

    private val people = listOf(
        Friend("p1", "민수", UserStatus.ACTIVE),
        Friend("p2", "현우", UserStatus.ACTIVE),
        Friend("p4", "지영", UserStatus.BUSY),
        Friend("p5", "서준", UserStatus.OFFLINE),
    )

    private fun family() = FamilyController(people)

    @Test fun starts_without_family() {
        val f = family()
        assertEquals(FamilyState.NONE, f.state)
        assertNull(f.familyId)
    }

    @Test fun designate_makes_pending() {
        val f = family()
        assertTrue(f.designate("p1", Gear.PARK))
        assertEquals(FamilyState.PENDING, f.state)
        assertEquals("p1", f.familyId)
    }

    @Test fun busy_person_can_be_designated() = assertTrue(family().designate("p4", Gear.PARK))

    @Test fun offline_or_unknown_cannot_be_designated() {
        val f = family()
        assertFalse(f.designate("p5", Gear.PARK))
        assertFalse(f.designate("nobody", Gear.PARK))
        assertEquals(FamilyState.NONE, f.state)
    }

    @Test fun designate_only_parked() {
        val f = family()
        assertFalse(f.designate("p1", Gear.DRIVE))
        assertFalse(f.designate("p1", Gear.UNKNOWN))
        assertEquals(FamilyState.NONE, f.state)
    }

    @Test fun only_one_family_at_a_time() {
        val f = family()
        f.designate("p1", Gear.PARK)
        assertFalse(f.designate("p2", Gear.PARK))
        f.respond(accept = true)
        assertFalse(f.designate("p2", Gear.PARK))
        assertEquals("p1", f.familyId)
    }

    @Test fun accept_makes_active() {
        val f = family()
        f.designate("p1", Gear.PARK)
        f.respond(accept = true)
        assertEquals(FamilyState.ACTIVE, f.state)
        assertEquals("p1", f.familyId)
    }

    @Test fun decline_clears() {
        val f = family()
        f.designate("p1", Gear.PARK)
        f.respond(accept = false)
        assertEquals(FamilyState.NONE, f.state)
        assertNull(f.familyId)
    }

    @Test fun respond_without_request_ignored() {
        val f = family()
        f.respond(accept = true)
        assertEquals(FamilyState.NONE, f.state)
    }

    @Test fun cancel_pending_even_while_driving() {
        val f = family()
        f.designate("p1", Gear.PARK)
        f.release()
        assertEquals(FamilyState.NONE, f.state)
        assertNull(f.familyId)
    }

    @Test fun release_active_clears_and_can_designate_again() {
        val f = family()
        f.designate("p1", Gear.PARK); f.respond(accept = true)
        f.release()
        assertEquals(FamilyState.NONE, f.state)
        assertTrue(f.designate("p2", Gear.PARK))
    }

    @Test fun family_name() {
        val f = family()
        assertNull(f.familyName)
        f.designate("p1", Gear.PARK)
        assertEquals("민수", f.familyName)
    }
}
