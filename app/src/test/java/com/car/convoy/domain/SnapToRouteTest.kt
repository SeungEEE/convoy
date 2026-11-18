package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R14-4, 작업 34(AI 작성): snapToRoute 4행 + 점 하나·빈 경로 + 같은 거리. 허용 오차 ±1m. */
class SnapToRouteTest {

    private val route = listOf(GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.502, 127.0))

    private fun check(point: GeoPoint, progress: Double, off: Double, path: List<GeoPoint> = route) {
        val snap = snapToRoute(point, path)!!
        assertEquals("progress", progress, snap.progressMeters, 1.0)
        assertEquals("off", off, snap.offRouteMeters, 1.0)
    }

    @Test fun on_middle_point() = check(GeoPoint(37.5010, 127.0), 111.2, 0.0)

    @Test fun middle_of_second_segment_east() = check(GeoPoint(37.5015, 127.0005), 166.8, 44.1)

    @Test fun before_start_clamps_to_start() = check(GeoPoint(37.4990, 127.0), 0.0, 111.2)

    @Test fun after_end_clamps_to_end() = check(GeoPoint(37.5030, 127.0), 222.4, 111.2)

    @Test fun single_point_route() = check(GeoPoint(37.501, 127.0), 0.0, 111.2, listOf(GeoPoint(37.500, 127.0)))

    @Test fun empty_route_is_null() = assertNull(snapToRoute(GeoPoint(37.5, 127.0), emptyList()))

    @Test fun tie_picks_smaller_progress() {
        // 같은 곳을 갔다가 돌아오는 경로: 시작점은 진행 0과 222.4 두 곳에 똑같이 가깝다.
        val back = listOf(GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.500, 127.0))
        check(GeoPoint(37.500, 127.0), 0.0, 0.0, back)
    }

    @Test fun nonzero_tie_picks_smaller_progress() {
        // V자 경로: 꼭짓점 아래(남쪽) 점은 두 선분에 같은 거리(0이 아님)다. 앞 선분(진행 거리가 작은 쪽)을 고른다(Codex 리뷰).
        val v = listOf(GeoPoint(37.500, 127.000), GeoPoint(37.501, 127.001), GeoPoint(37.500, 127.002))
        val snap = snapToRoute(GeoPoint(37.5005, 127.001), v)!!
        val firstSegment = distanceMeters(v[0], v[1])
        assertTrue("앞 선분 위", snap.progressMeters < firstSegment)
        assertTrue("0이 아닌 거리", snap.offRouteMeters > 1.0)
    }

    @Test fun route_length_accumulates_segments() {
        assertEquals(222.4, routeLengthMeters(route), 1.0)
    }
}
