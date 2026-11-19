package com.car.convoy.ui

import com.car.convoy.data.FakeScheduler
import com.car.convoy.data.GroupMapController
import com.car.convoy.domain.Destination
import com.car.convoy.domain.DrivingRemain
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.RouteResult
import com.car.convoy.domain.TurnGuide
import com.car.convoy.domain.port.RoutePort
import com.car.convoy.domain.routeLengthMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

/**
 * SPEC R14-8·15·17, 작업 36 Codex 리뷰 반영(AI 작성): 지도 화면 흐름.
 * 늦게 온 내비 콜백 버리기, 카드 지우는 시점, 다시 그리기 주기, 카드만 바뀔 때 지도를 다시 그리지 않음.
 */
class MapScreenControllerTest {

    private class FakeRoutes : RoutePort {
        val pending = mutableListOf<(RouteResult?) -> Unit>()
        override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) {
            pending += onResult
        }
    }

    private val line = (0..30).map { GeoPoint(37.400 + it * 0.001, 127.0) }
    private val seoul = Destination("서울역", "서울 용산구 한강대로 405", 37.430, 127.0)
    private val pangyo = Destination("판교역", "경기 성남시 분당구 판교역로 160", 37.420, 127.0)

    private val scheduler = FakeScheduler()
    private val routes = FakeRoutes()
    private val map = GroupMapController(routes, scheduler)
    private var gear = Gear.PARK
    private var destination: Destination? = null
    private var waypoints: List<Destination> = emptyList()
    private var me: GeoPoint? = GeoPoint(37.4135, 127.0)

    private val c = MapScreenController(
        scheduler = scheduler,
        names = mapOf("p1" to "민수"),
        gear = { gear },
        inputs = { MapInputs(map, destination, waypoints, me, me?.let { scheduler.nowMillis() }) },
        wallClock = { 0L },
        zone = ZoneId.of("Asia/Seoul"),
    )

    private val turn = TurnGuide("TURN_RIGHT", 700.0, "대왕판교IC 방면")
    private val remain = DrivingRemain(960, 18000)

    /** 그룹 목적지를 정하고 길찾기 결과와 내 경로 시작까지 받은 상태. */
    private fun driving() {
        c.startSession()
        destination = seoul
        c.onTargetChanged(seoul, emptyList())
        map.load(GeoPoint(37.400, 127.0), GeoPoint(37.430, 127.0), emptyList())
        routes.pending.last()(RouteResult(line, 600, routeLengthMeters(line).toInt()))
        c.onRouteResult()
        c.onRouteReady(c.token())
    }

    // ---- 늦게 온 콜백 버리기 ----

    @Test fun guide_before_my_route_starts_is_ignored() {
        c.startSession()
        destination = seoul
        c.onTargetChanged(seoul, emptyList())
        c.redraw()
        c.onTurns(c.token(), listOf(turn))
        c.onRemain(c.token(), remain)
        assertNull(c.ui?.turnCard)
        assertNull(c.ui?.arrivalCard)
    }

    @Test fun guide_after_my_route_starts_is_shown() {
        driving()
        c.onTurns(c.token(), listOf(turn))
        c.onRemain(c.token(), remain)
        assertEquals("700 m · 우회전", c.ui?.turnCard?.title)
        assertNotNull(c.ui?.arrivalCard)
    }

    @Test fun callback_from_previous_session_is_ignored() {
        driving()
        val old = c.token()
        c.endSession()
        driving()
        c.onTurns(old, listOf(turn))
        c.onRemain(old, remain)
        c.onRouteCancelled(old, expected = false)
        assertNull(c.ui?.turnCard)
        assertNull(c.ui?.arrivalCard)
        // 이전 세션의 취소가 새 세션의 준비 상태를 닫지 않았다.
        c.onTurns(c.token(), listOf(turn))
        assertNotNull(c.ui?.turnCard)
    }

    @Test fun callback_from_previous_route_is_ignored() {
        driving()
        val old = c.token()
        waypoints = listOf(pangyo)
        c.onTargetChanged(seoul, waypoints)
        c.onRouteReady(c.token())
        c.onRemain(old, DrivingRemain(0, 0))
        c.onTurns(old, emptyList())
        assertNull(c.ui?.arrivalCard)
        c.onRemain(c.token(), remain)
        assertNotNull(c.ui?.arrivalCard)
    }

    @Test fun zero_remain_while_adding_waypoint_is_not_shown() {
        // docs/sdk-findings.md 작업 36: 경유지를 넣는 사이 목적지 0m 0s가 한 번 온다. 내 차 반영(onWaypointChanged) 전이라 버린다.
        driving()
        c.onRemain(c.token(), remain)
        waypoints = listOf(pangyo)
        c.onTargetChanged(seoul, waypoints)
        c.onRouteCancelled(c.token(), expected = true)
        c.onRemain(c.token(), DrivingRemain(0, 0))
        assertNull(c.ui?.arrivalCard)
        c.onRouteReady(c.token())
        c.onRemain(c.token(), DrivingRemain(4500, 29000))
        assertNotNull(c.ui?.arrivalCard)
    }

    // ---- 카드 지우는 시점 (R14-15) ----

    @Test fun target_change_clears_cards_without_my_location() {
        driving()
        c.onTurns(c.token(), listOf(turn))
        c.onRemain(c.token(), remain)
        me = null
        c.onTargetChanged(seoul, listOf(pangyo))
        assertNull(c.ui?.turnCard)
        assertNull(c.ui?.arrivalCard)
    }

    @Test fun same_target_does_not_clear() {
        driving()
        c.onRemain(c.token(), remain)
        c.onTargetChanged(seoul, emptyList())
        assertNotNull(c.ui?.arrivalCard)
    }

    @Test fun expected_cancel_keeps_cards() {
        driving()
        c.onRemain(c.token(), remain)
        c.onRouteCancelled(c.token(), expected = true)
        assertNotNull(c.ui?.arrivalCard)
    }

    @Test fun unexpected_cancel_clears_and_waits_for_next_route() {
        driving()
        c.onRemain(c.token(), remain)
        c.onRouteCancelled(c.token(), expected = false)
        assertNull(c.ui?.arrivalCard)
        c.onRemain(c.token(), remain)
        assertNull(c.ui?.arrivalCard)
        c.onRouteReady(c.token())
        c.onRemain(c.token(), remain)
        assertNotNull(c.ui?.arrivalCard)
    }

    @Test fun route_failure_clears() {
        driving()
        c.onTurns(c.token(), listOf(turn))
        c.onRouteFailed(c.token())
        assertNull(c.ui?.turnCard)
    }

    @Test fun end_session_removes_map() {
        driving()
        c.endSession()
        assertNull(c.ui)
    }

    // ---- 다시 그리기 주기 (R14-8) ----

    /** [everyMillis]마다 tick하며 [totalMillis] 동안 그린 시각(ms)을 모은다. 시작 때 그린 것은 뺀다. */
    private fun drawTimes(everyMillis: Long, totalMillis: Long): List<Long> {
        driving()
        val times = mutableListOf<Long>()
        var t = 0L
        while (t < totalMillis) {
            scheduler.advanceBy(everyMillis)
            t += everyMillis
            val before = c.drawCount
            c.tick()
            if (c.drawCount != before) times += t
        }
        return times
    }

    @Test fun park_redraws_every_2_seconds() {
        gear = Gear.PARK
        assertEquals(listOf(2000L, 4000L, 6000L), drawTimes(2000, 6000))
    }

    @Test fun drive_redraws_every_10_seconds() {
        gear = Gear.DRIVE
        assertEquals(listOf(10000L, 20000L), drawTimes(2000, 20000))
    }

    @Test fun early_timer_still_keeps_park_interval() {
        // 예약 시계가 일찍 울려도(docs/sdk-findings.md 작업 21) 0.5초 안이면 그린다.
        gear = Gear.PARK
        assertEquals(listOf(1600L, 3200L, 4800L), drawTimes(1600, 4800))
    }

    // ---- 카드만 바뀌면 지도를 다시 그리지 않음 ----

    @Test fun card_update_does_not_change_draw_version() {
        driving()
        val version = c.drawVersion
        c.onTurns(c.token(), listOf(turn))
        c.onRemain(c.token(), remain)
        assertEquals(version, c.drawVersion)
    }

    @Test fun redraw_with_same_content_does_not_change_draw_version() {
        driving()
        val version = c.drawVersion
        c.redraw()
        assertEquals(version, c.drawVersion)
    }

    @Test fun moved_member_changes_draw_version() {
        driving()
        map.placeVirtual("p1", 300.0)
        c.redraw()
        val version = c.drawVersion
        map.moveVirtual("p1", 300.0)
        c.redraw()
        assertEquals(version + 1, c.drawVersion)
    }

    @Test fun route_version_follows_map() {
        driving()
        assertEquals(map.routeVersion, c.routeVersion)
    }
}
