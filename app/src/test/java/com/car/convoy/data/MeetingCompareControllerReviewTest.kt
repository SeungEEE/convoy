package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.RouteResult
import com.car.convoy.domain.port.RoutePort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC R14-2·20(작업 37 Codex 리뷰, AI 작성): 바로 응답하는 길찾기에서도 계산 중이 먼저 채워짐, 닫은 뒤 늦은 응답 버림,
 * 다른 후보를 고르거나 닫을 때 아직 시작하지 않은 이전 요청을 버림(쿼터·대기열 보호).
 */
class MeetingCompareControllerReviewTest {

    private val me = GeoPoint(37.41, 127.09)
    private val dest = GeoPoint(37.55, 126.97)
    private val rest = Destination("덕평휴게소", "경기 이천시 마장면", 37.24, 127.348)
    private val pangyo = Destination("판교역", "경기 성남시 분당구 판교역로 160", 37.3948, 127.1112)
    private fun result(seconds: Int) = RouteResult(listOf(me, dest), seconds, 1000)

    @Test fun immediate_answers_keep_row_loading_until_last() {
        lateinit var c: MeetingCompareController
        val loadingAfterEach = mutableListOf<Boolean>()
        val immediate = object : RoutePort {
            override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) {
                onResult(result(600))
                loadingAfterEach += c.rows.single().loading
            }
        }
        c = MeetingCompareController(immediate)
        c.select(rest, listOf(MeetingMember("me", me)), dest, emptyList(), 0L)
        assertEquals(listOf(true, true, false), loadingAfterEach)
        assertEquals(0, c.rows.single().detourSeconds)
    }

    @Test fun late_answer_after_clear_is_ignored() {
        val pending = mutableListOf<(RouteResult?) -> Unit>()
        val routes = object : RoutePort {
            override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) { pending += onResult }
        }
        var changes = 0
        val c = MeetingCompareController(routes) { changes++ }
        c.select(rest, listOf(MeetingMember("me", me)), dest, emptyList(), 0L)
        c.clear()
        val before = changes
        pending.forEach { it(result(600)) }
        assertNull(c.candidate)
        assertTrue(c.rows.isEmpty())
        assertEquals(before, changes)
    }

    @Test fun drops_queued_requests_on_new_candidate_and_clear() {
        val routes = object : RoutePort {
            override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) = Unit
        }
        var drops = 0
        val c = MeetingCompareController(routes, dropQueued = { drops++ })
        c.select(rest, listOf(MeetingMember("me", me)), dest, emptyList(), 0L)
        c.select(pangyo, listOf(MeetingMember("me", me)), dest, emptyList(), 0L)
        c.clear()
        assertEquals(3, drops)
    }

    @Test fun ignored_same_candidate_does_not_drop() {
        val routes = object : RoutePort {
            override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) = Unit
        }
        var drops = 0
        val c = MeetingCompareController(routes, dropQueued = { drops++ })
        c.select(rest, listOf(MeetingMember("me", me)), dest, emptyList(), 0L)
        c.select(rest, listOf(MeetingMember("me", me)), dest, emptyList(), 0L)
        assertEquals(1, drops)
    }
}
