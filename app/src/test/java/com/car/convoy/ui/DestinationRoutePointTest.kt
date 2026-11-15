package com.car.convoy.ui

import com.car.convoy.domain.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC R11-21(에뮬레이터 실측): 내비 목적지 좌표는 경로 도착점이라 같은 장소의 최근 목적지와 약 100m 다르다.
 * 주소 보완과 "지금 목적지" 표시는 이름이 같고 300m 미만이면 같은 곳으로 본다.
 */
class DestinationRoutePointTest {

    // 에뮬레이터에서 받은 실제 값.
    private val navi = Destination("현대백화점 판교점", "분당구 백현동", 37.39192222, 127.11171666)
    private val recentPoi = Destination("현대백화점 판교점", "경기도 성남시 분당구 판교역로146번길 20", 37.39283611, 127.11199166)

    @Test fun recent_address_used_for_route_point_100m_away() =
        assertEquals(recentPoi.address, destinationPickerModel(navi, null, null, listOf(recentPoi), emptyList()).suggestion?.address)

    @Test fun recent_poi_marked_current_when_group_is_route_point() =
        assertTrue(destinationPickerModel(null, navi, null, listOf(recentPoi), emptyList()).recent.single().current)

    @Test fun different_name_100m_away_not_current() {
        val other = recentPoi.copy(name = "판교 다른 곳")
        assertFalse(destinationPickerModel(null, navi, null, listOf(other), emptyList()).recent.single().current)
    }

    @Test fun same_name_300m_or_more_not_current() {
        val far = recentPoi.copy(latitude = 37.39192222 + 0.0028) // 약 311m
        assertFalse(destinationPickerModel(null, navi, null, listOf(far), emptyList()).recent.single().current)
    }
}
