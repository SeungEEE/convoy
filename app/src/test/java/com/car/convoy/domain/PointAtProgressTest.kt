package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC R14-3, 작업 35(AI 작성): 경로를 따라 진행 거리만큼 간 지점(가상 멤버 위치). */
class PointAtProgressTest {

    private val route = listOf(GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.502, 127.0))

    private fun near(expected: GeoPoint, actual: GeoPoint?) {
        assertEquals(expected.latitude, actual!!.latitude, 1e-6)
        assertEquals(expected.longitude, actual.longitude, 1e-6)
    }

    @Test fun start() = near(GeoPoint(37.500, 127.0), pointAtProgress(route, 0.0))
    @Test fun half_of_first_segment() = near(GeoPoint(37.5005, 127.0), pointAtProgress(route, routeLengthMeters(route) / 4))
    @Test fun middle_point() = near(GeoPoint(37.501, 127.0), pointAtProgress(route, routeLengthMeters(route) / 2))
    @Test fun before_start_clamps() = near(GeoPoint(37.500, 127.0), pointAtProgress(route, -50.0))
    @Test fun after_end_clamps() = near(GeoPoint(37.502, 127.0), pointAtProgress(route, 10_000.0))
    @Test fun single_point() = near(GeoPoint(37.5, 127.0), pointAtProgress(listOf(GeoPoint(37.5, 127.0)), 100.0))
    @Test fun empty_is_null() = assertNull(pointAtProgress(emptyList(), 0.0))

    @Test fun snap_of_point_at_progress_round_trips() {
        val p = pointAtProgress(route, 150.0)!!
        assertEquals(150.0, snapToRoute(p, route)!!.progressMeters, 0.5)
    }
}
