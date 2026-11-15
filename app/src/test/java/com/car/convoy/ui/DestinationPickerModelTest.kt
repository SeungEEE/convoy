package com.car.convoy.ui

import com.car.convoy.domain.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-20, R11-21(작업 26): 목적지 출처(내비 목적지, 최근 목적지, 즐겨찾기, 예비 목록). 거리 경계는 R11-13 예시표 좌표. */
class DestinationPickerModelTest {

    private fun d(name: String, lat: Double, lng: Double = 127.0, address: String = "$name 주소") = Destination(name, address, lat, lng)

    private val empty = Destination("", "", 0.0, 0.0)
    private val here = d("현대백화점 판교점", 37.5, address = "분당구 백현동")         // 내비 목적지(주소가 짧다)
    private val near = d("다른 이름", 37.50008)                                      // 8.8956m: 같은 곳
    private val far = d("다른 곳", 37.50009)                                          // 10.0075m: 다른 곳

    private fun model(
        navi: Destination? = null,
        group: Destination? = null,
        ignored: Destination? = null,
        recent: List<Destination> = emptyList(),
        bookmarks: List<Destination> = emptyList(),
    ) = destinationPickerModel(navi, group, ignored, recent, bookmarks)

    // ---- 내비에서 정한 목적지 ----
    @Test fun no_navi_info_no_suggestion() = assertNull(model().suggestion)

    @Test fun empty_navi_destination_no_suggestion() = assertNull(model(navi = empty).suggestion)

    @Test fun navi_destination_suggested_when_no_group_destination() =
        assertEquals("현대백화점 판교점", model(navi = here).suggestion?.name)

    @Test fun same_place_within_10m_not_suggested() = assertNull(model(navi = here, group = near).suggestion)

    @Test fun different_place_10m_or_more_suggested() = assertEquals(here.name, model(navi = here, group = far).suggestion?.name)

    @Test fun same_name_but_10m_away_is_different() =
        assertEquals(here.name, model(navi = here, group = here.copy(latitude = 37.50009)).suggestion?.name)

    @Test fun ignored_place_not_suggested() = assertNull(model(navi = here, ignored = near).suggestion)

    @Test fun suggested_again_after_navi_moves_10m_from_ignored() =
        assertEquals(here.name, model(navi = here, ignored = far).suggestion?.name)

    @Test fun address_taken_from_recent_with_same_name_within_10m() {
        val full = here.copy(address = "경기도 성남시 분당구 판교역로146번길 20", latitude = 37.50008)
        assertEquals("경기도 성남시 분당구 판교역로146번길 20", model(navi = here, recent = listOf(full)).suggestion?.address)
    }

    @Test fun address_not_taken_from_different_name() {
        val other = near.copy(address = "다른 주소")
        assertEquals("분당구 백현동", model(navi = here, recent = listOf(other)).suggestion?.address)
    }

    @Test fun address_not_taken_from_same_name_far_away() {
        // SPEC R11-21: 이름이 같아도 300m 이상 떨어지면 다른 곳이다(37.5 → 37.503은 약 334m).
        val farSame = here.copy(address = "먼 주소", latitude = 37.503)
        assertEquals("분당구 백현동", model(navi = here, recent = listOf(farSame)).suggestion?.address)
    }

    // ---- 최근 목적지 ----
    @Test fun recent_skips_blank_name_and_zero_coords_keeps_order_max_5() {
        val list = listOf(empty, d("A", 37.1), d("", 37.2), d("B", 0.0, 0.0), d("C", 37.3), d("D", 37.4), d("E", 37.5), d("F", 37.6), d("G", 37.7))
        assertEquals(listOf("A", "C", "D", "E", "F"), model(recent = list).recent.map { it.destination.name })
    }

    @Test fun recent_marks_current_group_destination_within_10m() {
        val m = model(group = near, recent = listOf(here, d("먼 곳", 37.6)))
        assertTrue(m.recent[0].current)
        assertFalse(m.recent[1].current)
    }

    // ---- 즐겨찾기 ----
    @Test fun bookmarks_skip_empty_slots() {
        val home = d("집", 37.3)
        assertEquals(listOf("집"), model(bookmarks = listOf(empty, home, empty)).bookmarks.map { it.destination.name })
    }

    // ---- 예비 목록 ----
    @Test fun fallback_presets_when_recent_and_bookmarks_empty() {
        val m = model(bookmarks = listOf(empty))
        assertTrue(m.usingFallback)
        assertEquals(PRESET_DESTINATIONS.map { it.name }, m.recent.map { it.destination.name })
    }

    @Test fun no_fallback_when_recent_exists() {
        val m = model(recent = listOf(here))
        assertFalse(m.usingFallback)
        assertEquals(listOf(here.name), m.recent.map { it.destination.name })
    }

    @Test fun fallback_marks_current() {
        val seoul = PRESET_DESTINATIONS[0]
        assertTrue(model(group = seoul).recent.first { it.destination.name == "서울역" }.current)
    }
}
