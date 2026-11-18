package com.car.convoy.data

import com.car.convoy.data.server.FakeConvoyServer
import com.car.convoy.domain.Destination
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SPEC R13-5·12·14, 작업 31(AI 작성): 가상 서버의 경유지 전달. */
class FakeConvoyServerWaypointTest {

    private val restArea = Destination("덕평휴게소", "경기 이천시 마장면", 37.2400, 127.3480)

    private val added = mutableListOf<String>()
    private val navi = object : NaviPort {
        override fun requestRoute(destination: Destination) {}
        override fun addWaypoint(waypoint: Destination) { added += waypoint.name }
    }
    private val results = mutableListOf<Pair<String, Boolean>>()
    private lateinit var scheduler: FakeScheduler
    private lateinit var server: FakeConvoyServer

    @Before fun setUp() {
        scheduler = FakeScheduler()
        server = FakeConvoyServer("me", navi, scheduler, onVirtualWaypoint = { id, ok -> results += id to ok }) { _, _ -> }
    }

    @Test fun my_car_adds_to_my_navi() {
        server.sendWaypoint("me", restArea)
        assertEquals(listOf("덕평휴게소"), added)
        scheduler.advanceBy(10_000)
        assertTrue(results.isEmpty())
    }

    @Test fun virtual_member_applies_after_4s() {
        server.sendWaypoint("p3", restArea)
        scheduler.advanceBy(3_999)
        assertTrue(results.isEmpty())
        scheduler.advanceBy(1)
        assertEquals(listOf("p3" to true), results)
        assertTrue(added.isEmpty())
    }

    @Test fun demo_failure_applies_once() {
        server.failNextWaypoint("p3")
        server.sendWaypoint("p3", restArea)
        scheduler.advanceBy(4_000)
        server.sendWaypoint("p3", restArea)
        scheduler.advanceBy(4_000)
        assertEquals(listOf("p3" to false, "p3" to true), results)
    }

    @Test fun cancel_pending_drops_waypoint_results() {
        server.sendWaypoint("p3", restArea)
        server.cancelPending()
        scheduler.advanceBy(10_000)
        assertTrue(results.isEmpty())
    }
}
