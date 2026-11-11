package com.car.convoy.ui

import com.car.convoy.domain.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-21: 주소가 비어 있는 최근 목적지(앞차 따라가기로 요청한 "<이름> 차" 등)는 보여 주지 않는다. */
class DestinationRecentAddressTest {

    private val followTarget = Destination("민수 차", "", 37.4217, 127.0947)
    private val real = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)

    @Test fun blank_address_excluded() =
        assertEquals(listOf("서울역"), destinationPickerModel(null, null, null, listOf(followTarget, real), emptyList()).recent.map { it.destination.name })

    @Test fun only_blank_address_falls_back_to_presets() =
        assertTrue(destinationPickerModel(null, null, null, listOf(followTarget), emptyList()).usingFallback)
}
