package com.car.convoy.data

import com.car.convoy.domain.ApplyState
import com.car.convoy.domain.ApplySummary
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.GroupSession
import com.car.convoy.domain.ProposalCheck
import com.car.convoy.domain.ProposalState
import com.car.convoy.domain.port.ConvoyServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SPEC R13-1~10, R13-14, 작업 31(AI 작성). 나(me)가 리더이고 민수(p1), 수진(p3)이 멤버다. */
class WaypointControllerTest {

    private class RecordingServer : ConvoyServer {
        val waypoints = mutableListOf<Pair<String, String>>()
        override fun sendDestination(memberId: String, destination: Destination) {}
        override fun sendWaypoint(memberId: String, waypoint: Destination) {
            waypoints += memberId to waypoint.name
        }
    }

    private val seoul = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)
    private val restArea = Destination("덕평휴게소", "경기 이천시 마장면", 37.2400, 127.3480)
    private val gasStation = Destination("만남주유소", "경기 용인시", 37.3000, 127.2000)
    private val third = Destination("판교역", "경기 성남시 분당구", 37.3948, 127.1112)

    private lateinit var scheduler: FakeScheduler
    private lateinit var server: RecordingServer
    private lateinit var session: GroupSession
    private lateinit var c: WaypointController

    @Before fun setUp() {
        scheduler = FakeScheduler()
        server = RecordingServer()
        session = GroupSession("me", listOf("me", "p1", "p3"), server)
        session.setDestination("me", seoul)
        c = WaypointController("me", session, server, scheduler)
    }

    private fun applyAll() = listOf("me", "p1", "p3").forEach { c.onWaypointAdded(it) }

    @Test fun member_proposal_waits_for_leader() {
        assertEquals(ProposalCheck.OK, c.propose("p3", restArea))
        assertEquals(ProposalState.PENDING, c.proposal!!.state)
        assertTrue(server.waypoints.isEmpty())
    }

    @Test fun approve_sends_to_every_member_including_leader() {
        c.propose("p3", restArea)
        c.approve()
        assertEquals(ProposalState.APPROVED, c.proposal!!.state)
        assertEquals(listOf(restArea), c.waypoints)
        assertEquals(setOf("me", "p1", "p3"), server.waypoints.map { it.first }.toSet())
        assertEquals(mapOf("me" to ApplyState.SENT, "p1" to ApplyState.SENT, "p3" to ApplyState.SENT), c.applyStates)
    }

    @Test fun all_added_is_complete() {
        c.propose("p3", restArea); c.approve()
        applyAll()
        assertEquals(ApplySummary(3, 3, true, 0, 0), c.summary)
    }

    @Test fun no_add_within_15s_fails_only_that_member() {
        c.propose("p3", restArea); c.approve()
        c.onWaypointAdded("me"); c.onWaypointAdded("p1")
        scheduler.advanceBy(14_999)
        assertEquals(ApplyState.SENT, c.applyStates["p3"])
        scheduler.advanceBy(1)
        assertEquals(ApplyState.FAILED, c.applyStates["p3"])
        assertEquals(ApplySummary(2, 3, false, 0, 1), c.summary)
    }

    @Test fun add_failed_exception_fails_immediately() {
        c.propose("p3", restArea); c.approve()
        c.onAddFailed("me")
        assertEquals(ApplyState.FAILED, c.applyStates["me"])
    }

    @Test fun resend_only_failed_members() {
        c.propose("p3", restArea); c.approve()
        c.onWaypointAdded("me"); c.onWaypointAdded("p1")
        scheduler.advanceBy(15_000)
        server.waypoints.clear()
        c.resend()
        assertEquals(listOf("p3" to "덕평휴게소"), server.waypoints)
        assertEquals(ApplyState.SENT, c.applyStates["p3"])
        assertEquals(ApplyState.APPLIED, c.applyStates["me"])
    }

    @Test fun pending_expires_after_60s_and_late_approve_is_ignored() {
        c.propose("p3", restArea)
        scheduler.advanceBy(59_999)
        assertEquals(ProposalState.PENDING, c.proposal!!.state)
        scheduler.advanceBy(1)
        assertEquals(ProposalState.EXPIRED, c.proposal!!.state)
        c.approve()
        assertTrue(server.waypoints.isEmpty())
        assertTrue(c.waypoints.isEmpty())
    }

    @Test fun reject_sends_nothing() {
        c.propose("p3", restArea)
        c.reject()
        assertEquals(ProposalState.REJECTED, c.proposal!!.state)
        assertTrue(server.waypoints.isEmpty())
    }

    @Test fun proposer_cancel() {
        c.propose("p3", restArea)
        c.cancel()
        assertEquals(ProposalState.CANCELLED, c.proposal!!.state)
    }

    @Test fun leader_proposal_is_sent_without_approval() {
        assertEquals(ProposalCheck.OK, c.propose("me", restArea))
        assertEquals(ProposalState.APPROVED, c.proposal!!.state)
        assertEquals(3, server.waypoints.size)
    }

    @Test fun second_proposal_blocked_while_pending() {
        c.propose("p3", restArea)
        assertEquals(ProposalCheck.PENDING_EXISTS, c.propose("p1", gasStation))
    }

    @Test fun next_proposal_blocked_while_applying_then_allowed_after_failure() {
        c.propose("p3", restArea); c.approve()
        assertEquals(ProposalCheck.APPLYING, c.propose("p1", gasStation))
        c.onWaypointAdded("me"); c.onWaypointAdded("p1")
        scheduler.advanceBy(15_000) // p3 FAILED만 남음
        assertEquals(ProposalCheck.OK, c.propose("p1", gasStation))
    }

    @Test fun same_place_as_waypoint_rejected() {
        c.propose("p3", restArea); c.approve(); applyAll()
        assertEquals(ProposalCheck.ALREADY_WAYPOINT, c.propose("p1", restArea))
    }

    @Test fun same_place_as_destination_rejected() {
        assertEquals(ProposalCheck.SAME_AS_DESTINATION, c.propose("p1", seoul))
    }

    @Test fun third_waypoint_is_full() {
        c.propose("me", restArea); applyAll()
        c.propose("me", gasStation); applyAll()
        assertEquals(ProposalCheck.FULL, c.propose("p1", third))
    }

    @Test fun no_destination_cannot_propose() {
        val fresh = GroupSession("me", listOf("me", "p1"), server)
        val controller = WaypointController("me", fresh, server, scheduler)
        assertEquals(ProposalCheck.NO_DESTINATION, controller.propose("p1", restArea))
        assertNull(controller.proposal)
    }

    @Test fun arrived_member_is_not_a_target() {
        session.onMemberEvent("p1", GroupEvent.ROUTE_STARTED)
        session.onMemberEvent("p1", GroupEvent.NAVI_ARRIVED)
        c.propose("me", restArea)
        assertEquals(setOf("me", "p3"), c.applyStates.keys)
        assertFalse(server.waypoints.any { it.first == "p1" })
    }

    @Test fun close_cancels_pending_and_ignores_later_events() {
        c.propose("p3", restArea)
        c.close()
        assertEquals(ProposalState.CANCELLED, c.proposal!!.state)
        c.approve()
        assertTrue(server.waypoints.isEmpty())
    }

    @Test fun close_drops_apply_timers_and_late_adds() {
        c.propose("me", restArea)
        c.close()
        assertTrue(c.applyStates.isEmpty())
        c.onWaypointAdded("p1")
        scheduler.advanceBy(60_000)
        assertTrue(c.applyStates.isEmpty())
    }

    @Test fun route_started_name_matches_destination_or_requested_waypoint() {
        assertTrue(c.isRouteStartedName("서울역"))
        assertFalse(c.isRouteStartedName("덕평휴게소"))
        c.propose("p3", restArea); c.approve()
        // onWaypointChanged보다 onRouteStarted가 먼저 와도 받는다(R13-8).
        assertTrue(c.isRouteStartedName("덕평휴게소"))
        assertFalse(c.isRouteStartedName("강남역"))
        assertFalse(c.isRouteStartedName(null))
    }

    // ---- Codex 리뷰 반영 ----
    @Test fun earlier_failure_is_kept_when_next_waypoint_is_sent() {
        c.propose("me", restArea)
        c.onWaypointAdded("me"); c.onWaypointAdded("p1")
        scheduler.advanceBy(15_000) // p3 FAILED
        c.propose("me", gasStation)
        // 다음 경유지는 모두에게 보낸다(이전 경유지는 다시 보내지 않으니 순서는 섞이지 않는다, R13-7).
        assertEquals(setOf("me", "p1", "p3"), c.applyStates.keys)
        // 이전 경유지의 실패는 사라지지 않는다.
        assertEquals(mapOf(0 to setOf("p3")), c.earlierFailures)
    }

    @Test fun resend_success_clears_nothing_from_history_until_next_send() {
        c.propose("me", restArea)
        listOf("me", "p1", "p3").forEach { c.onWaypointAdded(it) }
        c.propose("me", gasStation)
        assertEquals(emptyMap<Int, Set<String>>(), c.earlierFailures)
    }

    @Test fun cancel_judged_at_callback_time() {
        c.propose("me", restArea) // 0ms에 내 차 addWaypoint
        scheduler.advanceBy(20_000)
        // SDK 콜백은 14.9초에 왔지만 메인 스레드 처리가 늦었다.
        assertTrue(c.isExpectedRouteCancel(at = 14_900))
        assertFalse(c.isExpectedRouteCancel(at = 15_000))
    }

    @Test fun pending_expires_exactly_at_60s_after_creation_time() {
        scheduler.advanceBy(5_000)
        c.propose("p3", restArea)
        scheduler.advanceBy(59_999)
        assertEquals(ProposalState.PENDING, c.proposal!!.state)
        scheduler.advanceBy(1)
        assertEquals(ProposalState.EXPIRED, c.proposal!!.state)
    }

    @Test fun close_right_after_approve_drops_everything() {
        c.propose("p3", restArea); c.approve()
        c.close()
        scheduler.advanceBy(15_000)
        assertTrue(c.applyStates.isEmpty())
        assertEquals(ProposalState.APPROVED, c.proposal!!.state)
    }

    @Test fun resend_restarts_15s_timer() {
        c.propose("me", restArea)
        c.onWaypointAdded("me"); c.onWaypointAdded("p1")
        scheduler.advanceBy(15_000)
        c.resend()
        scheduler.advanceBy(14_999)
        assertEquals(ApplyState.SENT, c.applyStates["p3"])
        scheduler.advanceBy(1)
        assertEquals(ApplyState.FAILED, c.applyStates["p3"])
    }

    @Test fun cancel_within_15s_of_my_add_is_expected() {
        assertFalse(c.isExpectedRouteCancel())
        c.propose("me", restArea)
        scheduler.advanceBy(14_999)
        assertTrue(c.isExpectedRouteCancel())
        scheduler.advanceBy(1)
        assertFalse(c.isExpectedRouteCancel())
    }
}
