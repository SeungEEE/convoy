package com.car.convoy.data

import com.car.convoy.data.server.FakeConvoyServer
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.GroupEvent.*
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R11-14, R11-18: 내 차는 내 내비로, 가상 멤버는 6초 뒤 ROUTE_STARTED. 출발·도착은 시연자가 만든다. */
class FakeConvoyServerTest {

    private class RecordingNavi : NaviPort {
        val requested = mutableListOf<Destination>()
        override fun requestRoute(destination: Destination) { requested += destination }
    }

    private val a = Destination("A", "a", 37.5, 127.0)
    private val b = Destination("B", "b", 37.6, 127.0)

    private val navi = RecordingNavi()
    private val scheduler = FakeScheduler()
    private val events = mutableListOf<Pair<String, GroupEvent>>()
    private val server = FakeConvoyServer("me", navi, scheduler) { id, e -> events += id to e }

    @Test fun my_destination_goes_to_my_navi_without_virtual_event() {
        server.sendDestination("me", a)
        scheduler.advanceBy(10_000)
        assertEquals(listOf(a), navi.requested)
        assertEquals(emptyList<Pair<String, GroupEvent>>(), events)
    }

    @Test fun virtual_member_route_starts_after_six_seconds() {
        server.sendDestination("m1", a)
        scheduler.advanceBy(5_999)
        assertEquals(emptyList<Pair<String, GroupEvent>>(), events)
        scheduler.advanceBy(1)
        assertEquals(listOf("m1" to ROUTE_STARTED), events)
        assertEquals(emptyList<Destination>(), navi.requested)
    }

    @Test fun resend_cancels_previous_and_restarts_six_seconds() {
        server.sendDestination("m1", a)
        scheduler.advanceBy(4_000)
        server.sendDestination("m1", b)
        scheduler.advanceBy(5_999)
        assertEquals(emptyList<Pair<String, GroupEvent>>(), events)
        scheduler.advanceBy(1)
        assertEquals(listOf("m1" to ROUTE_STARTED), events)
    }

    @Test fun members_have_independent_timers() {
        server.sendDestination("m1", a)
        scheduler.advanceBy(3_000)
        server.sendDestination("m2", a)
        scheduler.advanceBy(3_000)
        assertEquals(listOf("m1" to ROUTE_STARTED), events)
        scheduler.advanceBy(3_000)
        assertEquals(listOf("m1" to ROUTE_STARTED, "m2" to ROUTE_STARTED), events)
    }

    @Test fun demo_depart_and_arrive() {
        server.depart("m1")
        server.arrive("m1")
        assertEquals(listOf("m1" to GEAR_DEPARTED, "m1" to NAVI_ARRIVED), events)
    }

    @Test(expected = IllegalArgumentException::class)
    fun demo_trigger_is_not_for_my_car() { server.arrive("me") }
}
