package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.distanceMeters
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R6-1~2, R12-2~4(작업 25): 앞차 위치로 경로를 요청하고, 마지막 요청 지점에서 200m 이상 움직이면 다시 요청한다. */
class FollowNavigatorTest {

    private class RecordingNavi : NaviPort {
        val requests = mutableListOf<Destination>()
        var cancels = 0
        var failNext = false
        var onFail: () -> Unit = {}
        override fun requestRoute(destination: Destination) {
            requests += destination
            if (failNext) { failNext = false; onFail() }
        }
        override fun cancelRoute() { cancels++ }
    }

    private val navi = RecordingNavi()
    private val scheduler = FakeScheduler()
    private val f = FollowNavigator(navi, scheduler).also { n -> navi.onFail = { n.onRouteFailed() } }

    @Test fun start_requests_route_to_leader_named_car() {
        f.start("민수")
        assertEquals(1, navi.requests.size)
        assertEquals("민수 차", navi.requests[0].name)
        assertEquals(FOLLOW_START.latitude, navi.requests[0].latitude, 0.0)
        assertEquals(FOLLOW_START.longitude, navi.requests[0].longitude, 0.0)
        assertEquals(0, f.rerouteCount)
    }

    @Test fun spec_example_100_100_300() {
        f.start("민수")
        f.moveLeader(100.0)
        assertEquals(1, navi.requests.size)              // 100m: 다시 요청 안 함
        assertEquals(100.0, f.movedSinceLastRequest, 0.01)
        f.moveLeader(100.0)
        assertEquals(2, navi.requests.size)              // 200m: 다시 요청
        assertEquals(1, f.rerouteCount)
        assertEquals(0.0, f.movedSinceLastRequest, 0.01)
        f.moveLeader(300.0)
        assertEquals(3, navi.requests.size)              // 300m: 다시 요청
        assertEquals(2, f.rerouteCount)
        assertEquals(500.0, distanceMeters(FOLLOW_START, navi.requests[2]), 0.01)
    }

    @Test fun leader_update_time_tracked() {
        f.start("민수")
        scheduler.advanceBy(2_000)
        f.moveLeader(100.0)
        assertEquals(2_000L, f.leaderUpdatedAtMillis)
    }

    @Test fun stop_cancels_route_and_ignores_moves() {
        f.start("민수")
        f.stop()
        assertEquals(1, navi.cancels)
        assertFalse(f.active)
        f.moveLeader(300.0)
        assertEquals(1, navi.requests.size)
    }

    @Test fun failure_shows_retry_and_retry_requests_last_leader_position() {
        navi.failNext = true
        f.start("민수")
        assertTrue(f.routeFailed)
        f.moveLeader(100.0)
        f.retry()
        assertFalse(f.routeFailed)
        assertEquals(2, navi.requests.size)
        assertEquals(100.0, distanceMeters(FOLLOW_START, navi.requests[1]), 0.01)
        assertEquals(0.0, f.movedSinceLastRequest, 0.01)
    }

    @Test fun retry_without_failure_does_nothing() {
        f.start("민수")
        f.retry()
        assertEquals(1, navi.requests.size)
    }
}
