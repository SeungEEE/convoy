package com.car.convoy.ui

import com.car.convoy.domain.Destination
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R11-21(Codex 리뷰): 같은 곳이 여러 번 있으면 "처음 것"을 남긴다. 이름만이 아니라 좌표까지 확인한다. */
class DestinationRecentKeepFirstTest {

    private val first = Destination("현대백화점 판교점", "경기도 성남시 분당구 판교역로146번길 20", 37.39283611, 127.11199166)
    private val second = Destination("현대백화점 판교점", "분당구 백현동", 37.39192222, 127.11171666)

    @Test fun keeps_the_first_entry() =
        assertEquals(first, destinationPickerModel(null, null, null, listOf(first, second), emptyList()).recent.single().destination)

    @Test fun keeps_the_first_even_when_order_is_reversed() =
        assertEquals(second, destinationPickerModel(null, null, null, listOf(second, first), emptyList()).recent.single().destination)
}
