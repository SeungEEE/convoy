package com.car.convoy.domain

import com.car.convoy.data.ConvoyServer
import com.car.convoy.data.NaviPort
import com.car.convoy.domain.GroupMemberState.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SPEC R11-11~15 작업 18 예시표 7행.
 * 멤버마다 가짜 NaviPort를 두고, 가짜 서버가 받은 목적지를 그 멤버의 NaviPort로 넘긴다(R11-14).
 */
class GroupDestinationTest {

    private class FakeNavi : NaviPort {
        val requested = mutableListOf<Destination>()
        override fun requestRoute(destination: Destination) { requested += destination }
    }

    private class FakeServer(private val cars: Map<String, NaviPort>) : ConvoyServer {
        override fun sendDestination(memberId: String, destination: Destination) =
            cars.getValue(memberId).requestRoute(destination)
    }

    private val current = Destination("현재", "", 37.5, 127.0)
    private fun target(latitude: Double) = Destination("새 목적지", "", latitude, 127.0)

    private val leader = "leader"
    private val ids = listOf(leader, "m1", "m2")
    private val navis = ids.associateWith { FakeNavi() }

    private fun session(
        memberIds: List<String> = ids,
        destination: Destination? = current,
        status: GroupStatus = GroupStatus.ACTIVE,
    ) = GroupSession(leader, memberIds, FakeServer(navis), destination, status)

    private fun assertRequestedOnce(memberIds: List<String>, destination: Destination) =
        memberIds.forEach { assertEquals(listOf(destination), navis.getValue(it).requested) }

    private fun assertNoRequest() = navis.values.forEach { assertEquals(emptyList<Destination>(), it.requested) }

    private fun assertStates(s: GroupSession, expected: GroupMemberState, memberIds: List<String> = ids) =
        memberIds.forEach { assertEquals(expected, s.memberState(it)) }

    @Test fun leader_first_destination_syncs_all() {
        val s = session(destination = null)
        s.setDestination(leader, target(37.5045))
        assertRequestedOnce(ids, target(37.5045))
        assertStates(s, REROUTING)
    }

    @Test fun leader_far_destination_syncs_all() {
        val s = session()
        s.setDestination(leader, target(37.5045))
        assertRequestedOnce(ids, target(37.5045))
        assertStates(s, REROUTING)
    }

    @Test fun leader_ten_meters_syncs_all() {
        val s = session()
        s.setDestination(leader, target(37.50009))
        assertRequestedOnce(ids, target(37.50009))
        assertStates(s, REROUTING)
    }

    @Test fun leader_under_ten_meters_is_ignored() {
        val s = session()
        s.setDestination(leader, target(37.50008))
        assertNoRequest()
        assertStates(s, IDLE)
    }

    @Test fun member_cannot_change_destination() {
        val s = session()
        s.setDestination("m1", target(37.5045))
        assertNoRequest()
        assertStates(s, IDLE)
    }

    @Test fun solo_leader_first_destination_requests_once() {
        val s = session(memberIds = listOf(leader), destination = null)
        s.setDestination(leader, target(37.5045))
        assertRequestedOnce(listOf(leader), target(37.5045))
        assertStates(s, REROUTING, listOf(leader))
    }

    @Test fun finished_group_ignores_destination_change() {
        val s = session(status = GroupStatus.FINISHED)
        s.setDestination(leader, target(37.5045))
        assertNoRequest()
        assertStates(s, IDLE)
    }
}
