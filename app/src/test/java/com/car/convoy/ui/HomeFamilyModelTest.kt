package com.car.convoy.ui

import com.car.convoy.data.FamilyController
import com.car.convoy.data.GroupRoster
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R9-5(작업 24): 홈의 가족 카드, 사람 줄의 [가족으로 설정], 머리줄 가족 표시, 시연 패널의 가족 응답. */
class HomeFamilyModelTest {

    private val people = listOf(
        Friend("p1", "민수", UserStatus.ACTIVE),
        Friend("p2", "현우", UserStatus.ACTIVE),
        Friend("p4", "지영", UserStatus.BUSY),
        Friend("p5", "서준", UserStatus.OFFLINE),
    )

    private fun model(family: FamilyController = FamilyController(people), gear: Gear = Gear.PARK) =
        homeScreenModel(people, UserStatus.ACTIVE, GroupRoster(people), driveActive = false, gear = gear, family = family)

    private fun HomeScreenModel.row(id: String) = this.people.first { it.id == id }

    private fun pending() = FamilyController(people).apply { designate("p1", Gear.PARK) }
    private fun active() = pending().apply { respond(accept = true) }

    @Test fun no_family_card() {
        val c = model().familyCard
        assertEquals(FamilyCardState.NONE, c.state)
        assertNull(c.name)
        assertEquals("가족을 정하면 사고가 의심될 때 알려요", c.message)
        assertFalse(c.showCancel)
        assertFalse(c.showRelease)
    }

    @Test fun pending_card() {
        val c = model(pending()).familyCard
        assertEquals(FamilyCardState.PENDING, c.state)
        assertEquals("민수", c.name)
        assertEquals("민수님 수락 대기", c.message)
        assertTrue(c.showCancel)
        assertFalse(c.showRelease)
    }

    @Test fun active_card() {
        val c = model(active()).familyCard
        assertEquals(FamilyCardState.ACTIVE, c.state)
        assertEquals("민수 · 사고 알림 받는 중", c.message)
        assertTrue(c.showRelease)
        assertFalse(c.showCancel)
    }

    @Test fun header_label() {
        assertEquals("가족 없음", model().familyHeader)
        assertEquals("가족 없음", model(pending()).familyHeader)
        assertEquals("가족 민수", model(active()).familyHeader)
    }

    @Test fun rows_offer_designate_when_none() {
        val m = model()
        assertTrue(m.row("p1").canDesignateFamily)
        assertTrue(m.row("p4").canDesignateFamily)
        assertNull(m.row("p1").familyTag)
    }

    @Test fun designate_disabled_while_driving() = assertFalse(model(gear = Gear.DRIVE).row("p1").canDesignateFamily)

    @Test fun others_disabled_when_someone_pending_or_active() {
        listOf(pending(), active()).forEach { f ->
            val m = model(f)
            assertFalse(m.row("p2").canDesignateFamily)
            assertFalse(m.row("p1").canDesignateFamily)
        }
    }

    @Test fun family_tag_on_row() {
        assertEquals("가족 수락 대기", model(pending()).row("p1").familyTag)
        assertEquals("가족", model(active()).row("p1").familyTag)
        assertNull(model(active()).row("p2").familyTag)
    }

    @Test fun demo_panel_family_request() {
        assertNull(model().pendingFamily)
        assertEquals(PendingInvite("p1", "민수"), model(pending()).pendingFamily)
        assertNull(model(active()).pendingFamily)
    }
}
