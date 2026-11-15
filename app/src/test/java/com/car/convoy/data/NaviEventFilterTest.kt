package com.car.convoy.data

import com.car.convoy.data.navi.isArrivalForRequest
import com.car.convoy.data.navi.isRouteStartedForRequest
import com.car.convoy.domain.Destination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-4, R11-17: 마지막으로 요청한 목적지와 정확히 같은 이름(도착은 이름과 주소)일 때만 이벤트를 만든다. */
class NaviEventFilterTest {

    private val requested = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)

    @Test fun route_started_same_name() = assertTrue(isRouteStartedForRequest("서울역", requested))
    @Test fun route_started_other_name_is_stale() = assertFalse(isRouteStartedForRequest("강남역", requested))
    @Test fun route_started_name_is_not_trimmed() = assertFalse(isRouteStartedForRequest("서울역 ", requested))
    @Test fun route_started_null_name() = assertFalse(isRouteStartedForRequest(null, requested))
    @Test fun route_started_without_request() = assertFalse(isRouteStartedForRequest("서울역", null))

    @Test fun arrived_same_name_and_address() =
        assertTrue(isArrivalForRequest("서울역", "서울 용산구 한강대로 405", requested))
    @Test fun arrived_other_name() =
        assertFalse(isArrivalForRequest("강남역", "서울 용산구 한강대로 405", requested))
    @Test fun arrived_other_address() =
        assertFalse(isArrivalForRequest("서울역", "서울 강남구 강남대로 396", requested))
    @Test fun arrived_without_request() =
        assertFalse(isArrivalForRequest("서울역", "서울 용산구 한강대로 405", null))
}
