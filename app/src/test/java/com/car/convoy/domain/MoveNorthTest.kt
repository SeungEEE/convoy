package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R12-3: 가상 앞차는 북쪽으로 N미터 옮긴다. 옮긴 거리는 distanceMeters(R11-13)로 재도 N미터다. */
class MoveNorthTest {
    private val start = Destination("민수 차", "", 37.4217, 127.0947)

    @Test fun moves_100m() = assertEquals(100.0, distanceMeters(start, moveNorth(start, 100.0)), 0.01)
    @Test fun moves_300m() = assertEquals(300.0, distanceMeters(start, moveNorth(start, 300.0)), 0.01)
    @Test fun keeps_longitude_and_name() {
        val moved = moveNorth(start, 100.0)
        assertEquals(start.longitude, moved.longitude, 0.0)
        assertEquals("민수 차", moved.name)
    }
}
