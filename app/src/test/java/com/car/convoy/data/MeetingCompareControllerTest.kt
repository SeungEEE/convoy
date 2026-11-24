package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.RouteResult
import com.car.convoy.domain.port.RoutePort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R14-20(작업 37, AI 작성): 합류 비교의 길찾기 세 가지, 늦은 응답 버림, 같은 후보 다시 누름, 카메라 완료 조건. */
class MeetingCompareControllerTest {

    private data class Call(val from: GeoPoint, val to: GeoPoint, val via: List<GeoPoint>, val onResult: (RouteResult?) -> Unit)

    private class FakeRoutes : RoutePort {
        val calls = mutableListOf<Call>()
        override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) {
            calls += Call(from, to, via, onResult)
        }
    }

    private val routes = FakeRoutes()
    private var changes = 0
    private val c = MeetingCompareController(routes) { changes++ }

    private val me = GeoPoint(37.41, 127.09)
    private val minsu = GeoPoint(37.42, 127.10)
    private val dest = GeoPoint(37.55, 126.97)
    private val wp = GeoPoint(37.30, 127.20)
    private val rest = Destination("덕평휴게소", "경기 이천시 마장면", 37.24, 127.348)
    private val pangyo = Destination("판교역", "경기 성남시 분당구 판교역로 160", 37.3948, 127.1112)
    private val restPoint = GeoPoint(rest.latitude, rest.longitude)

    private fun result(seconds: Int) = RouteResult(listOf(me, restPoint), seconds, 1000)

    private fun select(candidate: Destination = rest, members: List<MeetingMember> = listOf(MeetingMember("me", me)), destination: GeoPoint? = dest, waypoints: List<GeoPoint> = emptyList()) =
        c.select(candidate, members, destination, waypoints, nowMillis = 1_000L)

    @Test fun with_destination_calls_to_candidate_direct_and_via() {
        select(waypoints = listOf(wp))
        assertEquals(3, routes.calls.size)
        val (toCandidate, direct, via) = routes.calls
        assertEquals(Triple(me, restPoint, listOf(wp)), Triple(toCandidate.from, toCandidate.to, toCandidate.via))
        assertEquals(Triple(me, dest, listOf(wp)), Triple(direct.from, direct.to, direct.via))
        assertEquals(Triple(me, dest, listOf(wp, restPoint)), Triple(via.from, via.to, via.via))
    }

    @Test fun without_destination_calls_only_to_candidate() {
        select(destination = null)
        assertEquals(1, routes.calls.size)
        assertEquals(restPoint, routes.calls.single().to)
        assertFalse(c.hasDestination)
    }

    @Test fun member_without_location_is_not_called() {
        select(members = listOf(MeetingMember("me", me), MeetingMember("p1", null)))
        assertEquals(3, routes.calls.size)
        val row = c.rows.single { it.id == "p1" }
        assertFalse(row.located)
        assertFalse(row.loading)
    }

    @Test fun rows_keep_member_order() {
        select(members = listOf(MeetingMember("me", me), MeetingMember("p1", minsu)))
        assertEquals(listOf("me", "p1"), c.rows.map { it.id })
    }

    @Test fun loading_until_all_three_return() {
        select()
        routes.calls[0].onResult(result(1200))
        routes.calls[1].onResult(result(900))
        assertTrue(c.rows.single().loading)
        routes.calls[2].onResult(result(1080))
        val row = c.rows.single()
        assertFalse(row.loading)
        assertEquals(1200, row.toCandidate?.durationSeconds)
        assertEquals(180, row.detourSeconds)
    }

    @Test fun failed_direct_leaves_detour_empty() {
        select()
        routes.calls[0].onResult(result(1200))
        routes.calls[1].onResult(null)
        routes.calls[2].onResult(result(1080))
        assertEquals(1200, c.rows.single().toCandidate?.durationSeconds)
        assertNull(c.rows.single().detourSeconds)
    }

    @Test fun late_response_for_previous_candidate_is_ignored() {
        select(candidate = rest)
        val old = routes.calls.toList()
        select(candidate = pangyo)
        old.forEach { it.onResult(result(1200)) }
        assertTrue(c.rows.single().loading)
        assertEquals(pangyo, c.candidate)
    }

    @Test fun same_candidate_while_loading_is_ignored() {
        assertTrue(select())
        assertFalse(select())
        assertEquals(3, routes.calls.size)
    }

    @Test fun same_candidate_after_done_calls_again() {
        select()
        routes.calls.toList().forEach { it.onResult(result(1200)) }
        assertTrue(select())
        assertEquals(6, routes.calls.size)
    }

    @Test fun other_candidate_while_loading_restarts() {
        select(candidate = rest)
        assertTrue(select(candidate = pangyo))
        assertEquals(6, routes.calls.size)
    }

    @Test fun camera_waits_only_for_to_candidate_of_located_members() {
        select(members = listOf(MeetingMember("me", me), MeetingMember("p1", minsu), MeetingMember("p2", null)))
        assertFalse(c.cameraReady)
        routes.calls[0].onResult(result(1200)) // 나: 후보까지
        assertFalse(c.cameraReady)
        routes.calls[3].onResult(null) // 민수: 후보까지(실패도 끝난 것)
        assertTrue(c.cameraReady)
        assertTrue(c.rows.first().loading) // 곧장·경유는 아직
    }

    @Test fun no_located_member_is_camera_ready_at_once() {
        select(members = listOf(MeetingMember("p1", null)))
        assertTrue(routes.calls.isEmpty())
        assertTrue(c.cameraReady)
    }

    @Test fun selection_remembers_time_and_bumps_version() {
        val before = c.selectVersion
        select()
        assertEquals(1_000L, c.selectedAt)
        assertEquals(before + 1, c.selectVersion)
        assertTrue(changes > 0)
    }

    @Test fun nothing_selected_at_start() {
        assertNull(c.candidate)
        assertTrue(c.rows.isEmpty())
        assertFalse(c.cameraReady)
    }
}
