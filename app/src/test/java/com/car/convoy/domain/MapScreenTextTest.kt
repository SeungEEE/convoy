package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * SPEC R14-13·14·15, 작업 36(AI 작성): memberMapLabel 10행, routeSection 9행 + 점 하나·빈 경로,
 * turnCardText 6행, arrivalCardText 11행.
 */
class MapScreenTextTest {

    // ---- memberMapLabel (R14-13) ----

    private fun label(relation: MemberRelation, gap: Double, age: Int) = memberMapLabel("민수", relation, gap, age)

    @Test fun ahead_649() = assertEquals("민수 · 앞 약 650 m", label(MemberRelation.AHEAD, 649.0, 2))
    @Test fun behind_1199() = assertEquals("민수 · 뒤 약 1.2 km", label(MemberRelation.BEHIND, 1199.0, 2))
    @Test fun behind_995() = assertEquals("민수 · 뒤 약 1 km", label(MemberRelation.BEHIND, 995.0, 2))
    @Test fun near() = assertEquals("민수 · 나란히", label(MemberRelation.NEAR, 20.0, 2))
    @Test fun off_route() = assertEquals("민수 · 다른 길로 이동 중", label(MemberRelation.OFF_ROUTE, 649.0, 2))
    @Test fun stale_40() = assertEquals("민수 · 위치 확인 중 · 40초 전", label(MemberRelation.STALE, 649.0, 40))
    @Test fun stale_59() = assertEquals("민수 · 위치 확인 중 · 59초 전", label(MemberRelation.STALE, 649.0, 59))
    @Test fun stale_60() = assertEquals("민수 · 위치 확인 중 · 1분 전", label(MemberRelation.STALE, 649.0, 60))
    @Test fun stale_125() = assertEquals("민수 · 위치 확인 중 · 2분 전", label(MemberRelation.STALE, 649.0, 125))
    @Test fun me_unknown_is_null() = assertNull(label(MemberRelation.ME_UNKNOWN, 649.0, 2))

    // ---- routeSection (R14-14). 경로는 snapToRoute 표와 같은 세 점, 점마다 ±1m ----

    private val route = listOf(GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.502, 127.0))

    private fun section(from: Double, to: Double, vararg expected: GeoPoint, path: List<GeoPoint> = route) {
        val actual = routeSection(path, from, to)
        assertEquals("점 개수 $actual", expected.size, actual.size)
        expected.zip(actual).forEachIndexed { i, (e, a) ->
            assertTrue("점 $i: $a 는 $e 에서 1m 안이어야 한다", distanceMeters(e, a) <= 1.0)
        }
    }

    @Test fun whole_route() = section(0.0, 222.4, GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.502, 127.0))

    @Test fun across_middle_vertex() = section(55.6, 166.8, GeoPoint(37.5005, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.5015, 127.0))

    @Test fun reversed_order_is_sorted() = section(166.8, 55.6, GeoPoint(37.5005, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.5015, 127.0))

    @Test fun inside_one_segment() = section(20.0, 80.0, GeoPoint(37.50018, 127.0), GeoPoint(37.50072, 127.0))

    @Test fun clamped_both_ends() = section(-50.0, 300.0, GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.502, 127.0))

    @Test fun same_progress_is_one_point() = section(166.8, 166.8, GeoPoint(37.5015, 127.0))

    @Test fun clamped_to_same_start_is_one_point() = section(-50.0, 0.0, GeoPoint(37.500, 127.0))

    @Test fun clamped_to_same_end_is_one_point() = section(222.4, 300.0, GeoPoint(37.502, 127.0))

    @Test fun vertex_on_end_is_not_repeated() {
        val l1 = routeLengthMeters(route.subList(0, 2))
        section(l1, 166.8, GeoPoint(37.501, 127.0), GeoPoint(37.5015, 127.0))
    }

    @Test fun single_point_route() =
        section(-50.0, 300.0, GeoPoint(37.500, 127.0), path = listOf(GeoPoint(37.500, 127.0)))

    @Test fun empty_route() = assertEquals(emptyList<GeoPoint>(), routeSection(emptyList(), 0.0, 100.0))

    // ---- turnCardText (R14-15) ----

    private fun turn(type: String, meters: Double, description: String, title: String, subtitle: String?) {
        val card = turnCardText(type, meters, description)
        assertEquals(title, card.title)
        assertEquals(subtitle, card.subtitle)
    }

    @Test fun exit_ic_right() = turn("TURN_EXIT_IC_R", 2000.0, "대왕판교IC 방면", "2 km · 오른쪽 출구", "대왕판교IC 방면")
    @Test fun empty_direction() = turn("TURN_RIGHT", 704.0, "", "700 m · 우회전", null)
    @Test fun blank_direction() = turn("TURN_RIGHT", 704.0, " ", "700 m · 우회전", null)
    @Test fun unknown_turn_shows_distance_only() = turn("TURN_UNKNOWN", 300.0, "판교 방면", "300 m", "판교 방면")
    @Test fun direction_is_trimmed() = turn("TURN_RIGHT", 704.0, " 판교 방면 ", "700 m · 우회전", "판교 방면")
    @Test fun unknown_turn_and_blank_direction() = turn("TURN_UNKNOWN", 300.0, " ", "300 m", null)

    // ---- arrivalCardText (R14-15). 시각은 서울 기준 ----

    private val zone = ZoneId.of("Asia/Seoul")

    private fun at(hour: Int, minute: Int, second: Int): Long =
        LocalDateTime.of(2026, 11, 19, hour, minute, second).atZone(zone).toInstant().toEpochMilli()

    private fun arrival(now: Long, seconds: Int, meters: Int) = arrivalCardText(now, zone, seconds, meters)

    @Test fun arrival_16_minutes() = assertEquals("10:24 도착 · 16분 · 18 km", arrival(at(10, 8, 0), 960, 18000))
    @Test fun arrival_drops_seconds() = assertEquals("10:24 도착 · 16분 · 18 km", arrival(at(10, 8, 59), 960, 18000))
    @Test fun arrival_89_seconds() = assertEquals("10:09 도착 · 1분 · 700 m", arrival(at(10, 8, 0), 89, 704))
    @Test fun arrival_90_seconds_rounds_up() = assertEquals("10:09 도착 · 2분 · 700 m", arrival(at(10, 8, 0), 90, 704))
    @Test fun arrival_under_one_minute() = assertEquals("10:08 도착 · 1분 미만 · 120 m", arrival(at(10, 8, 0), 29, 120))
    @Test fun arrival_one_hour() = assertEquals("11:08 도착 · 1시간 · 60 km", arrival(at(10, 8, 0), 3600, 60000))
    @Test fun arrival_hour_and_minutes() = assertEquals("11:28 도착 · 1시간 20분 · 95 km", arrival(at(10, 8, 0), 4800, 95000))
    @Test fun arrival_after_midnight() = assertEquals("00:10 도착 · 20분 · 15 km", arrival(at(23, 50, 0), 1200, 15000))
    @Test fun arrival_negative_is_zero() = assertEquals("10:08 도착 · 1분 미만 · 0 m", arrival(at(10, 8, 0), -5, -10))
    @Test fun arrival_59_minutes() = assertEquals("11:07 도착 · 59분 · 60 km", arrival(at(10, 8, 0), 3569, 60000))
    @Test fun arrival_rounds_to_one_hour() = assertEquals("11:07 도착 · 1시간 · 60 km", arrival(at(10, 8, 0), 3570, 60000))
}
