package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupMemberState.*
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-19: 내 차 경로 요청이 실패하면 그룹 상태는 그대로 두고, 현재 목적지를 내 내비에만 다시 요청한다. */
class GroupControllerRetryTest {

    private class RecordingNavi : NaviPort {
        val requested = mutableListOf<Destination>()
        override fun requestRoute(destination: Destination) { requested += destination }
    }

    private val dest = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)
    private val navi = RecordingNavi()
    private val scheduler = FakeScheduler()
    private val c = GroupController("me", listOf("me", "m1", "m2"), navi, scheduler)

    @Test fun failure_is_flagged_and_group_state_is_unchanged() {
        c.setDestination(dest)
        c.onMyRouteFailed()
        assertTrue(c.myRouteFailed)
        listOf("me", "m1", "m2").forEach { assertEquals(REROUTING, c.session.memberState(it)) }
        assertEquals(dest, c.session.destination)
    }

    @Test fun retry_requests_current_destination_to_my_navi_only() {
        c.setDestination(dest)
        c.onMyRouteFailed()
        navi.requested.clear()
        c.retryMyRoute()
        assertEquals(listOf(dest), navi.requested)
        assertFalse(c.myRouteFailed)
        listOf("me", "m1", "m2").forEach { assertEquals(REROUTING, c.session.memberState(it)) }
    }

    // 가상 멤버의 6초 예약을 다시 시작하지 않는다(내 내비에만 보낸다).
    @Test fun retry_does_not_restart_virtual_members() {
        c.setDestination(dest)
        scheduler.advanceBy(5_000)
        c.onMyRouteFailed()
        c.retryMyRoute()
        scheduler.advanceBy(1_000)
        assertEquals(DRIVING, c.session.memberState("m1"))
        assertEquals(DRIVING, c.session.memberState("m2"))
    }

    @Test fun retry_without_destination_does_nothing() {
        c.retryMyRoute()
        assertEquals(emptyList<Destination>(), navi.requested)
    }

    @Test fun new_destination_clears_failure() {
        c.setDestination(dest)
        c.onMyRouteFailed()
        c.setDestination(Destination("강남역", "서울 강남구 강남대로 396", 37.4979, 127.0276))
        assertFalse(c.myRouteFailed)
    }
}
