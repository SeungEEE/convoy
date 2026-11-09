package com.car.convoy.domain

import com.car.convoy.domain.GroupEvent.*
import com.car.convoy.domain.GroupMemberState.*
import com.car.convoy.domain.GroupStatus.*
import com.car.convoy.domain.port.ConvoyServer
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R7-4, R11-5, R11-7, R11-9, R11-15 작업 19 예시표 6행과 세션 흐름. */
class GroupFinishTest {

    // ---- nextGroupStatus: 작업 19 표 ----
    private fun check(
        status: GroupStatus,
        states: List<GroupMemberState>,
        expectedStatus: GroupStatus,
        expectedEffect: GroupEffect?,
    ) = assertEquals(GroupTransition(expectedStatus, expectedEffect), nextGroupStatus(status, states))

    @Test fun last_member_arrives_finishes_group() =
        check(ACTIVE, listOf(ARRIVED, ARRIVED, ARRIVED), FINISHED, GroupEffect.FINISH_GROUP)

    @Test fun some_members_not_arrived_keeps_active() =
        check(ACTIVE, listOf(ARRIVED, DRIVING, ARRIVED), ACTIVE, null)

    @Test fun solo_arrived_finishes_group() =
        check(ACTIVE, listOf(ARRIVED), FINISHED, GroupEffect.FINISH_GROUP)

    @Test fun solo_rerouting_keeps_active() =
        check(ACTIVE, listOf(REROUTING), ACTIVE, null)

    @Test fun finished_does_not_finish_again() =
        check(FINISHED, listOf(ARRIVED, ARRIVED, ARRIVED), FINISHED, null)

    // ---- R11-7 변환 ----
    @Test fun r11_7_only_arrived_maps_to_arrived() {
        assertEquals(MemberState.ARRIVED, ARRIVED.toArrivalState())
        listOf(IDLE, REROUTING, DRIVING).forEach { assertEquals(MemberState.DEPARTED, it.toArrivalState()) }
    }

    // ---- GroupSession.onMemberEvent: 세션 흐름 ----
    private class RecordingServer : ConvoyServer {
        val sent = mutableListOf<Pair<String, Destination>>()
        override fun sendDestination(memberId: String, destination: Destination) { sent += memberId to destination }
    }

    private val leader = "leader"
    private val ids = listOf(leader, "m1", "m2")
    private val target = Destination("목적지", "", 37.5045, 127.0)

    /** 목적지를 정하고 모두 경로가 시작된(DRIVING) 세션. */
    private fun drivingSession(server: ConvoyServer = RecordingServer(), memberIds: List<String> = ids) =
        GroupSession(leader, memberIds, server).apply {
            setDestination(leader, target)
            memberIds.forEach { onMemberEvent(it, ROUTE_STARTED) }
        }

    @Test fun session_finishes_once_when_last_member_arrives() {
        val s = drivingSession()
        assertEquals(MemberEventResult(MemberEffect.SHARE_ARRIVED, null), s.onMemberEvent(leader, NAVI_ARRIVED))
        assertEquals(MemberEventResult(MemberEffect.SHARE_ARRIVED, null), s.onMemberEvent("m1", NAVI_ARRIVED))
        assertEquals(ACTIVE, s.status)
        assertEquals(
            MemberEventResult(MemberEffect.SHARE_ARRIVED, GroupEffect.FINISH_GROUP),
            s.onMemberEvent("m2", NAVI_ARRIVED),
        )
        assertEquals(FINISHED, s.status)
    }

    @Test fun solo_session_finishes_on_arrival() {
        val s = drivingSession(memberIds = listOf(leader))
        assertEquals(
            MemberEventResult(MemberEffect.SHARE_ARRIVED, GroupEffect.FINISH_GROUP),
            s.onMemberEvent(leader, NAVI_ARRIVED),
        )
        assertEquals(FINISHED, s.status)
    }

    @Test fun finished_session_ignores_member_events() {
        val s = drivingSession()
        ids.forEach { s.onMemberEvent(it, NAVI_ARRIVED) }
        assertEquals(MemberEventResult(null, null), s.onMemberEvent("m1", NAVI_ARRIVED))
        assertEquals(MemberEventResult(null, null), s.onMemberEvent("m1", GEAR_DEPARTED))
        assertEquals(ARRIVED, s.memberState("m1"))
        assertEquals(FINISHED, s.status)
    }

    @Test fun finished_session_ignores_destination_change() {
        val server = RecordingServer()
        val s = drivingSession(server)
        ids.forEach { s.onMemberEvent(it, NAVI_ARRIVED) }
        server.sent.clear()
        s.setDestination(leader, Destination("다른 곳", "", 37.6, 127.0))
        assertEquals(emptyList<Pair<String, Destination>>(), server.sent)
        ids.forEach { assertEquals(ARRIVED, s.memberState(it)) }
        assertEquals(target, s.destination)
    }

    // 목적지 변경은 setDestination으로만 한다(서버 전송이 빠지지 않게).
    @Test(expected = IllegalArgumentException::class)
    fun leader_set_destination_is_not_a_member_event() {
        drivingSession().onMemberEvent(leader, LEADER_SET_DESTINATION)
    }
}
