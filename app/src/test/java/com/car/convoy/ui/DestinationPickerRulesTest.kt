package com.car.convoy.ui

import com.car.convoy.domain.Destination
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-21(Codex 리뷰로 고친 문구): 내비 목적지 유효 조건, 즐겨찾기 "지금 목적지" 기준. */
class DestinationPickerRulesTest {

    private val navi = Destination("현대백화점 판교점", "분당구 백현동", 37.39192222, 127.11171666)

    @Test fun navi_with_name_but_zero_coords_is_no_destination() =
        assertNull(destinationPickerModel(navi.copy(latitude = 0.0, longitude = 0.0), null, null, emptyList(), emptyList()).suggestion)

    @Test fun navi_with_coords_but_blank_name_is_no_destination() =
        assertNull(destinationPickerModel(navi.copy(name = ""), null, null, emptyList(), emptyList()).suggestion)

    @Test fun bookmark_marked_current_with_same_name_within_300m() {
        val bookmarkPoi = Destination("현대백화점 판교점", "경기도 성남시 분당구 판교역로146번길 20", 37.39283611, 127.11199166)
        assertTrue(destinationPickerModel(null, navi, null, emptyList(), listOf(bookmarkPoi)).bookmarks.single().current)
    }
}
