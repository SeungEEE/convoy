package com.car.convoy.ui

import com.car.convoy.domain.Destination
import org.junit.Assert.assertEquals
import org.junit.Test

/** 녹화 중 발견: Convoy가 경로를 요청하면 내비가 같은 곳을 최근 목적지에 한 번 더 남긴다. 같은 곳은 한 번만 보여 준다. */
class DestinationRecentDedupTest {

    private val poi = Destination("현대백화점 판교점", "경기도 성남시 분당구 판교역로146번길 20", 37.39283611, 127.11199166)
    private val routePoint = Destination("현대백화점 판교점", "경기도 성남시 분당구 판교역로146번길 20", 37.39192222, 127.11171666)
    private val seoul = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)

    @Test fun same_place_shown_once_keeping_first() =
        assertEquals(listOf("현대백화점 판교점", "서울역"),
            destinationPickerModel(null, null, null, listOf(poi, routePoint, seoul), emptyList()).recent.map { it.destination.name })

    @Test fun dedup_happens_before_the_5_limit() {
        val list = listOf(poi, routePoint) + (1..5).map { Destination("곳$it", "주소$it", 37.0 + it, 127.0) }
        assertEquals(5, destinationPickerModel(null, null, null, list, emptyList()).recent.size)
        assertEquals("곳4", destinationPickerModel(null, null, null, list, emptyList()).recent.last().destination.name)
    }
}
