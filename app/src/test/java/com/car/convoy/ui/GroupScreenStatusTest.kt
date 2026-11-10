package com.car.convoy.ui

import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.GroupMemberState
import com.car.convoy.domain.GroupSession
import com.car.convoy.domain.port.ConvoyServer
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R11-20 "그룹 상태와 목적지": 그룹 상태 문구와 멤버 상태 값(색은 이 값으로 고른다). Codex 리뷰. */
class GroupScreenStatusTest {

    private object NoServer : ConvoyServer {
        override fun sendDestination(memberId: String, destination: Destination) {}
    }

    private val ids = listOf("me", "m1")
    private val dest = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)

    private fun model(s: GroupSession) = groupScreenModel(s, "me", ids, emptyMap(), true, Gear.PARK, false)

    @Test fun active_group_status() =
        assertEquals("그룹 주행 중", model(GroupSession("me", ids, NoServer)).groupStatusLabel)

    @Test fun finished_group_status() {
        val s = GroupSession("me", ids, NoServer)
        s.setDestination("me", dest)
        ids.forEach { s.onMemberEvent(it, GroupEvent.ROUTE_STARTED) }
        ids.forEach { s.onMemberEvent(it, GroupEvent.NAVI_ARRIVED) }
        assertEquals("모두 도착했어요", model(s).groupStatusLabel)
    }

    @Test fun member_row_carries_state() {
        val s = GroupSession("me", ids, NoServer)
        s.setDestination("me", dest)
        assertEquals(listOf(GroupMemberState.REROUTING, GroupMemberState.REROUTING), model(s).members.map { it.state })
    }
}
