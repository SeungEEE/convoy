package com.car.convoy.data

import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.MemberRelation
import com.car.convoy.domain.RouteResult
import com.car.convoy.domain.port.RoutePort
import com.car.convoy.domain.routeLengthMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R14-2·3·5·6, 작업 35(AI 작성): 경로 불러오기, 가상 멤버 배치·이동, 앞뒤 관계. */
class GroupMapControllerTest {

    private class FakeRoutes : RoutePort {
        val calls = mutableListOf<Triple<GeoPoint, GeoPoint, List<GeoPoint>>>()
        val pending = mutableListOf<(RouteResult?) -> Unit>()
        override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) {
            calls += Triple(from, to, via); pending += onResult
        }
    }

    // 북쪽으로 곧은 경로(약 3.3km), 0.001도마다 점.
    private val line = (0..30).map { GeoPoint(37.400 + it * 0.001, 127.0) }
    private val result = RouteResult(line, 600, routeLengthMeters(line).toInt())
    private val me = GeoPoint(37.4135, 127.0) // 진행 약 1501m

    private val routes = FakeRoutes()
    private val scheduler = FakeScheduler()
    private val c = GroupMapController(routes, scheduler)

    private fun loaded() {
        c.load(GeoPoint(37.400, 127.0), GeoPoint(37.430, 127.0), listOf(me))
        routes.pending.last()(result)
    }

    @Test fun load_passes_start_destination_and_via() {
        c.load(GeoPoint(37.400, 127.0), GeoPoint(37.430, 127.0), listOf(me))
        assertEquals(Triple(GeoPoint(37.400, 127.0), GeoPoint(37.430, 127.0), listOf(me)), routes.calls.single())
        assertTrue(c.loading)
    }

    @Test fun load_success_keeps_route() {
        loaded()
        assertEquals(line, c.snapshot(me, scheduler.nowMillis()).route)
        assertFalse(c.loadFailed)
        assertFalse(c.loading)
    }

    @Test fun load_failure_draws_no_route() {
        c.load(GeoPoint(37.400, 127.0), GeoPoint(37.430, 127.0), emptyList())
        routes.pending.last()(null)
        assertTrue(c.loadFailed)
        assertTrue(c.snapshot(me, scheduler.nowMillis()).route.isEmpty())
    }

    @Test fun late_result_of_earlier_load_is_ignored() {
        c.load(GeoPoint(37.400, 127.0), GeoPoint(37.430, 127.0), emptyList())
        val first = routes.pending.last()
        loaded()
        first(RouteResult(listOf(GeoPoint(1.0, 1.0), GeoPoint(2.0, 2.0)), 1, 1))
        assertEquals(line, c.snapshot(me, scheduler.nowMillis()).route)
    }

    @Test fun virtual_members_ahead_and_behind_me() {
        loaded()
        c.placeVirtual("p1", 650.0)
        c.placeVirtual("p3", -1200.0)
        val s = c.snapshot(me, scheduler.nowMillis())
        assertEquals(1501.0, s.myProgress!!, 2.0)
        val p1 = s.members.single { it.id == "p1" }
        val p3 = s.members.single { it.id == "p3" }
        assertEquals(MemberRelation.AHEAD, p1.relation)
        assertEquals(650.0, p1.gapMeters, 2.0)
        assertEquals(MemberRelation.BEHIND, p3.relation)
        assertEquals(1200.0, p3.gapMeters, 2.0)
    }

    @Test fun move_virtual_along_route() {
        loaded()
        c.placeVirtual("p1", 650.0)
        c.snapshot(me, scheduler.nowMillis())
        c.moveVirtual("p1", 300.0)
        assertEquals(950.0, c.snapshot(me, scheduler.nowMillis()).members.single().gapMeters, 2.0)
    }

    @Test fun frozen_virtual_becomes_stale_after_30s() {
        loaded()
        c.placeVirtual("p3", -1200.0)
        c.snapshot(me, scheduler.nowMillis())
        c.freezeVirtual("p3")
        scheduler.advanceBy(30_000)
        assertEquals(MemberRelation.BEHIND, c.snapshot(me, scheduler.nowMillis()).members.single().relation)
        scheduler.advanceBy(1_000)
        val m = c.snapshot(me, scheduler.nowMillis()).members.single()
        assertEquals(MemberRelation.STALE, m.relation)
        assertEquals(31, m.ageSeconds)
    }

    @Test fun my_location_unknown() {
        loaded()
        c.placeVirtual("p1", 650.0)
        val s = c.snapshot(null, null)
        assertTrue(s.meUnknown)
        assertNull(s.myProgress)
        assertTrue(s.members.isEmpty())
    }

    @Test fun my_location_stale_hides_relations() {
        loaded()
        c.placeVirtual("p1", 650.0)
        c.snapshot(me, scheduler.nowMillis())
        val oldFix = scheduler.nowMillis()
        scheduler.advanceBy(31_000)
        val s = c.snapshot(me, oldFix)
        assertTrue(s.meUnknown)
        assertEquals(MemberRelation.ME_UNKNOWN, s.members.single().relation)
    }

    @Test fun reload_keeps_member_position_on_new_route() {
        loaded()
        c.placeVirtual("p1", 650.0)
        val before = c.snapshot(me, scheduler.nowMillis()).members.single().position
        // 경유지가 생겨 경로가 바뀐다: 같은 길 앞에 남쪽 우회 구간이 붙어 진행 거리가 모두 늘어난다.
        val detour = listOf(GeoPoint(37.395, 127.0)) + line
        c.load(GeoPoint(37.395, 127.0), GeoPoint(37.430, 127.0), listOf(me))
        routes.pending.last()(RouteResult(detour, 700, routeLengthMeters(detour).toInt()))
        val after = c.snapshot(me, scheduler.nowMillis()).members.single()
        // 멤버는 같은 자리에 있고(진행 거리만 새 경로 기준), 나와의 관계도 그대로다.
        assertEquals(before.latitude, after.position.latitude, 1e-5)
        assertEquals(MemberRelation.AHEAD, after.relation)
        assertEquals(650.0, after.gapMeters, 2.0)
    }

    // ---- Codex 리뷰 반영 ----
    @Test fun failed_reload_then_success_keeps_member_position() {
        loaded()
        c.placeVirtual("p1", 650.0)
        val before = c.snapshot(me, scheduler.nowMillis()).members.single().position
        c.load(GeoPoint(37.400, 127.0), GeoPoint(37.430, 127.0), listOf(me))
        routes.pending.last()(null) // B 실패
        val detour = listOf(GeoPoint(37.395, 127.0)) + line
        c.load(GeoPoint(37.395, 127.0), GeoPoint(37.430, 127.0), listOf(me))
        routes.pending.last()(RouteResult(detour, 700, routeLengthMeters(detour).toInt())) // C 성공
        val after = c.snapshot(me, scheduler.nowMillis()).members.single()
        assertEquals(before.latitude, after.position.latitude, 1e-5)
        assertEquals(650.0, after.gapMeters, 2.0)
    }

    @Test fun old_route_hidden_while_loading() {
        loaded()
        c.load(GeoPoint(37.400, 127.0), GeoPoint(37.430, 127.0), listOf(me))
        assertTrue(c.loading)
        assertTrue(c.snapshot(me, scheduler.nowMillis()).route.isEmpty())
    }

    @Test fun offset_not_fixed_from_unknown_my_location() {
        loaded()
        c.placeVirtual("p1", 650.0)
        // 첫 스냅샷은 31초 전의 다른 위치(진행 약 500m)라 ME_UNKNOWN: 이 위치로 자리를 정하지 않는다.
        val oldFix = scheduler.nowMillis()
        scheduler.advanceBy(31_000)
        assertTrue(c.snapshot(GeoPoint(37.4045, 127.0), oldFix).meUnknown)
        // 새 위치(진행 약 1501m)를 받으면 그 기준으로 650m 앞에 둔다.
        val m = c.snapshot(me, scheduler.nowMillis()).members.single()
        assertEquals(1501.0 + 650.0, m.progress, 3.0)
    }

    @Test fun no_route_no_members() {
        c.placeVirtual("p1", 650.0)
        assertTrue(c.snapshot(me, scheduler.nowMillis()).members.isEmpty())
    }
}
