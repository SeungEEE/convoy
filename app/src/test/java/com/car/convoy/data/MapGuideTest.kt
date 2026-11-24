package com.car.convoy.data

import com.car.convoy.domain.DrivingRemain
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.RouteResult
import com.car.convoy.domain.TurnGuide
import com.car.convoy.domain.port.RoutePort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SPEC R14-7·15·17, 작업 36(AI 작성): 회전 안내·도착 값 보관과 지우기, 카메라 맞춤용 경로 번호.
 */
class MapGuideTest {

    private val guide = MapGuide()

    @Test fun first_turn_is_kept() {
        guide.onTurns(listOf(TurnGuide("TURN_RIGHT", 700.0, "판교 방면"), TurnGuide("TURN_LEFT", 2000.0, "")))
        assertEquals(TurnGuide("TURN_RIGHT", 700.0, "판교 방면"), guide.turn)
    }

    @Test fun empty_turn_list_hides_card() {
        guide.onTurns(listOf(TurnGuide("TURN_RIGHT", 700.0, "")))
        guide.onTurns(emptyList())
        assertNull(guide.turn)
    }

    @Test fun destination_remain_is_kept_and_null_keeps_previous() {
        guide.onDestinationRemain(DrivingRemain(960, 18000))
        assertEquals(DrivingRemain(960, 18000), guide.remain)
        // 목적지 항목이 없는 onDrivingInfo는 값을 지우지 않는다(지우는 시점은 clear만, R14-15).
        guide.onDestinationRemain(null)
        assertEquals(DrivingRemain(960, 18000), guide.remain)
    }

    @Test fun clear_removes_both() {
        guide.onTurns(listOf(TurnGuide("TURN_RIGHT", 700.0, "")))
        guide.onDestinationRemain(DrivingRemain(960, 18000))
        guide.clear()
        assertNull(guide.turn)
        assertNull(guide.remain)
    }

    // ---- R14-17: 새 길찾기 결과가 올 때만 경로 번호가 바뀐다(화면은 번호가 바뀔 때 한 번 카메라를 맞춘다) ----

    private class FakeRoutes : RoutePort {
        val pending = mutableListOf<(RouteResult?) -> Unit>()
        override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) {
            pending += onResult
        }
    }

    private val line = (0..10).map { GeoPoint(37.400 + it * 0.001, 127.0) }
    private val routes = FakeRoutes()
    private val map = GroupMapController(routes, FakeScheduler())

    private fun load() = map.load(GeoPoint(37.400, 127.0), GeoPoint(37.410, 127.0), emptyList())

    @Test fun route_version_starts_at_zero() = assertEquals(0, map.routeVersion)

    @Test fun route_version_changes_on_new_result() {
        load()
        assertEquals(0, map.routeVersion)
        routes.pending.last()(RouteResult(line, 600, 1100))
        assertEquals(1, map.routeVersion)
        load()
        routes.pending.last()(RouteResult(line, 600, 1100))
        assertEquals(2, map.routeVersion)
    }

    @Test fun route_version_unchanged_on_failure_or_stale_result() {
        load()
        routes.pending.last()(null)
        assertEquals(0, map.routeVersion)
        load()
        val stale = routes.pending.last()
        load()
        stale(RouteResult(line, 600, 1100)) // 늦게 온 이전 응답은 버린다(작업 35)
        assertEquals(0, map.routeVersion)
    }

    @Test fun moving_members_does_not_change_route_version() {
        load()
        routes.pending.last()(RouteResult(line, 600, 1100))
        map.placeVirtual("p1", 100.0)
        map.snapshot(GeoPoint(37.403, 127.0), 0L)
        map.moveVirtual("p1", 300.0)
        map.snapshot(GeoPoint(37.403, 127.0), 0L)
        assertEquals(1, map.routeVersion)
    }
}
