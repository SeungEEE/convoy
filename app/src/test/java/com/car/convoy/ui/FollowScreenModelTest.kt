package com.car.convoy.ui

import com.car.convoy.domain.Destination
import com.car.convoy.domain.ReportLocation
import com.car.convoy.domain.moveNorth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R12-4(작업 25): 앞차 따라가기 화면에 보일 값. */
class FollowScreenModelTest {

    private val leader = Destination("민수 차", "", 37.4217, 127.0947)

    private fun model(
        me: ReportLocation? = ReportLocation(37.4217, 127.0947, null),
        leaderAt: Destination = leader,
        updatedAt: Long = 0, now: Long = 0, reroutes: Int = 0, moved: Double = 0.0, failed: Boolean = false,
    ) = followScreenModel("민수", me, leaderAt, updatedAt, now, reroutes, moved, failed)

    @Test fun title() = assertEquals("민수 차를 따라가는 중", model().title)

    @Test fun distance_unknown_without_my_location() = assertEquals("거리 확인 중", model(me = null).distanceLabel)

    @Test fun distance_in_meters_below_1km() =
        assertEquals("약 850m 앞", model(leaderAt = moveNorth(leader, 850.0)).distanceLabel)

    @Test fun distance_in_km_from_1km() =
        assertEquals("약 1.2km 앞", model(leaderAt = moveNorth(leader, 1_234.0)).distanceLabel)

    @Test fun updated_seconds_ago() = assertEquals("2초 전", model(updatedAt = 1_000, now = 3_400).updatedLabel)

    @Test fun reroutes_and_moved() {
        val m = model(reroutes = 2, moved = 80.4)
        assertEquals("2번", m.rerouteLabel)
        assertEquals("80m", m.movedLabel)
    }

    @Test fun retry_only_when_failed() {
        assertFalse(model().showRetry)
        assertTrue(model(failed = true).showRetry)
    }
}
