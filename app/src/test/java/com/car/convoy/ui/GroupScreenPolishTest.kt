package com.car.convoy.ui

import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.GroupSession
import com.car.convoy.domain.port.ConvoyServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 그룹 주행 화면 보강 값: 도착 요약, 목적지 주소, 리더 표시. 규칙(R11-20)은 바꾸지 않는다. */
class GroupScreenPolishTest {

    private object NoServer : ConvoyServer {
        override fun sendDestination(memberId: String, destination: Destination) {}
    }

    private val ids = listOf("me", "m1", "m2")
    private val dest = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)

    private fun model(s: GroupSession) = groupScreenModel(s, "me", ids, emptyMap(), true, Gear.PARK, false)

    private fun driving() = GroupSession("me", ids, NoServer).apply {
        setDestination("me", dest)
        ids.forEach { onMemberEvent(it, GroupEvent.ROUTE_STARTED) }
    }

    @Test fun arrived_summary_counts_arrived_members() {
        val s = driving()
        assertEquals("도착 0 / 3", model(s).arrivedSummary)
        s.onMemberEvent("m1", GroupEvent.NAVI_ARRIVED)
        assertEquals("도착 1 / 3", model(s).arrivedSummary)
    }

    @Test fun destination_address() {
        assertNull(model(GroupSession("me", ids, NoServer)).destinationAddress)
        assertEquals("서울 용산구 한강대로 405", model(driving()).destinationAddress)
    }

    @Test fun only_my_row_is_leader() {
        val rows = model(driving()).members
        assertTrue(rows.first { it.id == "me" }.isLeader)
        assertFalse(rows.first { it.id == "m1" }.isLeader)
    }
}
