package com.car.convoy.ui

import com.car.convoy.data.WaypointProposal
import com.car.convoy.domain.ApplyState
import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.ProposalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R13-1·4·7·11, 작업 32(AI 작성): 경유지 패널 화면 값. 나(me)가 리더다. */
class WaypointPanelModelTest {

    private val names = mapOf("me" to "준희", "p1" to "민수", "p3" to "수진")
    private val rest = Destination("덕평휴게소", "경기 이천시 마장면", 37.24, 127.348)
    private val pangyo = Destination("판교역", "경기 성남시 분당구", 37.3948, 127.1112)

    private fun model(
        proposal: WaypointProposal? = null,
        waypoints: List<Destination> = emptyList(),
        apply: Map<String, ApplyState> = emptyMap(),
        gear: Gear = Gear.PARK,
        active: Boolean = true,
        hasDestination: Boolean = true,
    ) = waypointPanelModel("me", proposal, waypoints, apply, names, gear, active, hasDestination)

    @Test fun nothing_yet() {
        val m = model()
        assertTrue(m.waypointNames.isEmpty())
        assertNull(m.request)
        assertNull(m.applyText)
        assertFalse(m.showResend)
        assertTrue(m.canPropose)
    }

    @Test fun cannot_propose_while_driving() {
        assertFalse(model(gear = Gear.DRIVE).canPropose)
    }

    @Test fun cannot_propose_without_destination_or_after_finish() {
        assertFalse(model(hasDestination = false).canPropose)
        assertFalse(model(active = false).canPropose)
    }

    @Test fun member_request_shows_leader_panel_even_while_driving() {
        val m = model(proposal = WaypointProposal("p3", rest, ProposalState.PENDING), gear = Gear.DRIVE)
        assertEquals(WaypointRequestUi("수진님의 경유지 요청", "덕평휴게소", "경기 이천시 마장면", canRespond = true), m.request)
    }

    @Test fun my_own_pending_is_not_a_request_to_me() {
        // 리더(나)의 제안은 바로 보내므로 PENDING이 없지만, 있더라도 나에게 묻지 않는다.
        assertNull(model(proposal = WaypointProposal("me", rest, ProposalState.PENDING)).request)
    }

    @Test fun waypoint_names_numbered() {
        assertEquals(listOf("경유지 1 · 덕평휴게소", "경유지 2 · 판교역"), model(waypoints = listOf(rest, pangyo)).waypointNames)
    }

    @Test fun all_applied_text() {
        val m = model(waypoints = listOf(rest), apply = mapOf("me" to ApplyState.APPLIED, "p1" to ApplyState.APPLIED, "p3" to ApplyState.APPLIED))
        assertEquals("3/3 반영 · 모두 반영했어요", m.applyText)
        assertTrue(m.applyComplete)
        assertFalse(m.showResend)
    }

    @Test fun waiting_member_named_as_processing() {
        val m = model(waypoints = listOf(rest), apply = mapOf("me" to ApplyState.APPLIED, "p1" to ApplyState.APPLIED, "p3" to ApplyState.SENT))
        assertEquals("2/3 반영 · 수진 처리 중", m.applyText)
        assertFalse(m.applyComplete)
        assertFalse(m.showResend)
    }

    @Test fun failed_member_named_and_resend_shown() {
        val m = model(waypoints = listOf(rest), apply = mapOf("me" to ApplyState.APPLIED, "p1" to ApplyState.FAILED, "p3" to ApplyState.APPLIED))
        assertEquals("2/3 반영 · 민수 실패", m.applyText)
        assertTrue(m.showResend)
    }

    @Test fun waiting_and_failed_both_shown() {
        val m = model(waypoints = listOf(rest), apply = mapOf("me" to ApplyState.SENT, "p1" to ApplyState.FAILED, "p3" to ApplyState.APPLIED))
        assertEquals("1/3 반영 · 준희 처리 중 · 민수 실패", m.applyText)
    }

    @Test fun earlier_failure_stays_on_waypoint_line() {
        val m = waypointPanelModel(
            "me", null, listOf(rest, pangyo),
            mapOf("me" to ApplyState.APPLIED, "p1" to ApplyState.APPLIED, "p3" to ApplyState.APPLIED),
            names, Gear.PARK, groupActive = true, hasDestination = true,
            earlierFailures = mapOf(0 to setOf("p3")),
        )
        assertEquals(listOf("경유지 1 · 덕평휴게소 · 수진 미반영", "경유지 2 · 판교역"), m.waypointNames)
    }

    @Test fun ended_proposal_notices() {
        assertEquals("수진님의 요청을 거절했어요", model(proposal = WaypointProposal("p3", rest, ProposalState.REJECTED)).notice)
        assertEquals("응답이 없어 수진님의 요청이 만료됐어요", model(proposal = WaypointProposal("p3", rest, ProposalState.EXPIRED)).notice)
        assertEquals("수진님의 요청이 취소됐어요", model(proposal = WaypointProposal("p3", rest, ProposalState.CANCELLED)).notice)
        assertNull(model(proposal = WaypointProposal("p3", rest, ProposalState.APPROVED)).notice)
        assertNull(model(proposal = WaypointProposal("p3", rest, ProposalState.PENDING)).notice)
    }
}
