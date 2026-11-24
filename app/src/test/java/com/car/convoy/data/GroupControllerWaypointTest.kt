package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupStatus
import com.car.convoy.domain.ProposalState
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R13-14: 전원 도착(FINISHED)도 세션 종료다. Codex 리뷰 반영(AI 작성). */
class GroupControllerWaypointTest {

    private class RecordingNavi : NaviPort {
        val added = mutableListOf<String>()
        override fun requestRoute(destination: Destination) {}
        override fun addWaypoint(waypoint: Destination) { added += waypoint.name }
    }

    private val dest = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)
    private val rest = Destination("덕평휴게소", "경기 이천시 마장면", 37.2400, 127.3480)
    private val navi = RecordingNavi()
    private val scheduler = FakeScheduler()
    private val c = GroupController("me", listOf("me", "m1"), navi, scheduler)

    private fun allArrive() {
        c.onRouteStarted("서울역")
        scheduler.advanceBy(6_000) // 가상 멤버 경로 시작
        c.server.depart("m1")
        c.server.arrive("m1")
        c.onDestinationArrived("서울역", "서울 용산구 한강대로 405")
    }

    @Test fun group_finish_cancels_pending_proposal() {
        c.setDestination(dest)
        c.waypoints.propose("m1", rest)
        allArrive()
        assertEquals(GroupStatus.FINISHED, c.session.status)
        assertEquals(ProposalState.CANCELLED, c.waypoints.proposal!!.state)
        c.waypoints.approve()
        assertTrue(navi.added.isEmpty())
        scheduler.advanceBy(60_000)
        assertEquals(ProposalState.CANCELLED, c.waypoints.proposal!!.state)
    }

    @Test fun group_finish_drops_virtual_waypoint_results() {
        c.setDestination(dest)
        c.onRouteStarted("서울역")
        scheduler.advanceBy(6_000)
        c.server.depart("m1")
        c.server.arrive("m1") // m1 도착, 아직 그룹은 ACTIVE(나는 주행 중)
        c.waypoints.propose("me", rest) // 나에게만 보냄(m1은 ARRIVED)
        c.onDestinationArrived("서울역", "서울 용산구 한강대로 405")
        assertEquals(GroupStatus.FINISHED, c.session.status)
        assertTrue(c.waypoints.applyStates.isEmpty())
        c.onMyWaypointAdded()
        assertTrue(c.waypoints.applyStates.isEmpty())
    }
}
