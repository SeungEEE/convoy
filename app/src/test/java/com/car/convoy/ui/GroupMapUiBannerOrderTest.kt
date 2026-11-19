package com.car.convoy.ui

import com.car.convoy.data.MapSnapshot
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

/**
 * SPEC R14-2·6·16, 작업 36 Codex 리뷰 반영(AI 작성): 지도 안내 문구의 우선순위.
 * 목적지 없음 → 경로 찾는 중(요청 중) → 실패 → 경로 찾는 중(경로 없음) → 내 위치 확인 중. 조건이 겹칠 때 앞의 것이 이긴다.
 */
class GroupMapUiBannerOrderTest {

    private val route = listOf(GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0))
    private val seoul = Destination("서울역", "", 37.501, 127.0)

    private fun banner(destination: Destination?, loading: Boolean, failed: Boolean, hasRoute: Boolean, meUnknown: Boolean): String? {
        val snapshot = MapSnapshot(if (hasRoute) route else emptyList(), null, null, meUnknown, emptyList())
        return groupMapUi(snapshot, loading, failed, destination, emptyList(), emptyMap(), null, null, 0L, ZoneId.of("Asia/Seoul")).banner
    }

    @Test fun no_destination_beats_everything() =
        assertEquals("목적지를 정하면 경로가 보여요", banner(null, loading = true, failed = true, hasRoute = false, meUnknown = true))

    @Test fun loading_beats_failed() =
        assertEquals("경로 찾는 중", banner(seoul, loading = true, failed = true, hasRoute = false, meUnknown = true))

    @Test fun failed_beats_no_route_and_me_unknown() =
        assertEquals("경로를 불러오지 못했어요", banner(seoul, loading = false, failed = true, hasRoute = false, meUnknown = true))

    @Test fun no_route_beats_me_unknown() =
        assertEquals("경로 찾는 중", banner(seoul, loading = false, failed = false, hasRoute = false, meUnknown = true))

    @Test fun me_unknown_last() =
        assertEquals("내 위치 확인 중", banner(seoul, loading = false, failed = false, hasRoute = true, meUnknown = true))
}
