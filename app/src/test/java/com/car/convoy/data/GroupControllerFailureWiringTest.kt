package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupStatus
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC R11-19: 어댑터처럼 requestRoute 안에서 실패 콜백을 바로 부르는 가짜 내비로,
 * 실패 표시가 실제 호출 순서에서 남는지 확인한다.
 */
class GroupControllerFailureWiringTest {

    /** 요청을 기록하고, failing이면 NaviHelperAdapter처럼 호출 중에 실패를 알린다. */
    private class FailingNavi(private val onFailed: () -> Unit) : NaviPort {
        var failing = true
        val requested = mutableListOf<Destination>()
        override fun requestRoute(destination: Destination) {
            requested += destination
            if (failing) onFailed()
        }
    }

    private val seoul = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)
    private val gangnam = Destination("강남역", "서울 강남구 강남대로 396", 37.4979, 127.0276)

    private lateinit var c: GroupController
    private val navi = FailingNavi { c.onMyRouteFailed() }
    private val scheduler = FakeScheduler()

    init {
        c = GroupController("me", listOf("me", "m1", "m2"), navi, scheduler)
    }

    @Test fun failure_during_set_destination_is_kept() {
        c.setDestination(seoul)
        assertTrue(c.myRouteFailed)
        assertTrue(c.canRetryMyRoute)
    }

    @Test fun retry_failure_is_flagged_again_through_callback() {
        c.setDestination(seoul)
        c.retryMyRoute()
        assertTrue(c.myRouteFailed)
        assertEquals(listOf(seoul, seoul), navi.requested)
    }

    @Test fun retry_success_clears_failure() {
        c.setDestination(seoul)
        navi.failing = false
        c.retryMyRoute()
        assertFalse(c.myRouteFailed)
    }

    @Test fun new_destination_success_clears_previous_failure() {
        c.setDestination(seoul)
        navi.failing = false
        c.setDestination(gangnam)
        assertFalse(c.myRouteFailed)
    }

    @Test fun new_destination_failure_is_kept() {
        c.setDestination(seoul)
        c.setDestination(gangnam)
        assertTrue(c.myRouteFailed)
    }

    // 요청이 나가지 않은 목적지 변경(IGNORE_SAME)은 이전 실패 표시를 지우지 않는다.
    @Test fun ignored_destination_keeps_failure() {
        c.setDestination(seoul)
        navi.failing = false
        c.setDestination(seoul)
        assertTrue(c.myRouteFailed)
    }

    // 종료된 그룹은 실패 표시가 남아도 재요청할 수 없다(화면 안내도 이 값으로 숨긴다).
    @Test fun finished_group_cannot_retry() {
        c.setDestination(seoul)
        navi.failing = false
        c.retryMyRoute()
        scheduler.advanceBy(6_000)
        c.onRouteStarted("서울역")
        c.server.arrive("m1")
        c.server.arrive("m2")
        c.onDestinationArrived("서울역", "서울 용산구 한강대로 405")
        assertEquals(GroupStatus.FINISHED, c.session.status)
        c.onMyRouteFailed()
        assertFalse(c.canRetryMyRoute)
    }
}
