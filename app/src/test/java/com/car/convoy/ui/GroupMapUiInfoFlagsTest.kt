package com.car.convoy.ui

import com.car.convoy.data.MapSnapshot
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * SPEC R14-16·19, 작업 39(AI 작성): 정보 카드와 [내 위치]에 넘길 값.
 * "경로가 있다"는 지도 안내 문구가 경로 때문에 뜨지 않는 때(목적지 있음, 요청 중 아님, 실패 아님, 경로 점 있음)다.
 */
class GroupMapUiInfoFlagsTest {

    private val route = listOf(GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0))
    private val seoul = Destination("서울역", "", 37.501, 127.0)
    private val me = GeoPoint(37.5002, 127.0)

    private fun ui(destination: Destination? = seoul, loading: Boolean = false, failed: Boolean = false, hasRoute: Boolean = true, meUnknown: Boolean = false, myPoint: GeoPoint? = me) =
        groupMapUi(
            MapSnapshot(if (hasRoute) route else emptyList(), myPoint, if (meUnknown) null else 20.0, meUnknown, emptyList()),
            loading, failed, destination, emptyList(), emptyMap(), null, null, 0L, ZoneId.of("Asia/Seoul"),
        )

    @Test fun route_ready() = assertTrue(ui().hasRoute)

    @Test fun no_destination_no_route() = assertFalse(ui(destination = null).hasRoute)

    @Test fun loading_no_route() = assertFalse(ui(loading = true).hasRoute)

    @Test fun failed_no_route() = assertFalse(ui(failed = true).hasRoute)

    @Test fun empty_route_no_route() = assertFalse(ui(hasRoute = false).hasRoute)

    @Test fun me_unknown_passed_through() {
        assertTrue(ui(meUnknown = true).meUnknown)
        assertFalse(ui().meUnknown)
    }

    @Test fun my_point_passed_through_even_when_unknown() {
        // R14-16: ME_UNKNOWN이어도 마지막으로 받은 위치로 옮긴다.
        assertEquals(me, ui(meUnknown = true).myPoint)
        assertNull(ui(myPoint = null).myPoint)
    }
}
