package com.car.convoy.ui

import com.car.convoy.data.MapSnapshot
import com.car.convoy.data.MemberOnMap
import com.car.convoy.domain.Destination
import com.car.convoy.domain.DrivingRemain
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.MemberRelation
import com.car.convoy.domain.TurnGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * SPEC R14-6·7·15·16, 작업 36(AI 작성): 지도에 그릴 값.
 * 경로는 북쪽으로 곧은 세 점(약 111.2m 간격), 나는 가운데(진행 111.2m)에 있다.
 */
class GroupMapUiTest {

    private val route = listOf(GeoPoint(37.500, 127.0), GeoPoint(37.501, 127.0), GeoPoint(37.502, 127.0))
    private val me = GeoPoint(37.501, 127.0)
    private val seoul = Destination("서울역", "서울 용산구 한강대로 405", 37.502, 127.0)
    private val names = mapOf("p1" to "민수", "p3" to "수진")
    private val zone = ZoneId.of("Asia/Seoul")
    private val now = LocalDateTime.of(2026, 11, 19, 10, 8, 0).atZone(zone).toInstant().toEpochMilli()

    private fun member(id: String, progress: Double, relation: MemberRelation, gap: Double, age: Int = 0) =
        MemberOnMap(id, GeoPoint(37.500 + progress / 111_195.0, 127.0), progress, relation, gap, age)

    private fun snapshot(vararg members: MemberOnMap, meUnknown: Boolean = false, path: List<GeoPoint> = route) =
        MapSnapshot(path, me, if (meUnknown) null else 111.2, meUnknown, members.toList())

    private fun ui(
        snapshot: MapSnapshot = snapshot(),
        loading: Boolean = false,
        failed: Boolean = false,
        destination: Destination? = seoul,
        waypoints: List<Destination> = emptyList(),
        turn: TurnGuide? = null,
        remain: DrivingRemain? = null,
    ) = groupMapUi(snapshot, loading, failed, destination, waypoints, names, turn, remain, now, zone)

    // ---- 선 (R14-6·7) ----

    @Test fun whole_route_is_blue_first() {
        val lines = ui().lines
        assertEquals(1, lines.size)
        assertEquals(LineColor.BLUE, lines[0].color)
        assertEquals(route, lines[0].points)
    }

    @Test fun ahead_member_section_is_green_from_me_to_member() {
        val lines = ui(snapshot(member("p1", 166.8, MemberRelation.AHEAD, 55.6))).lines
        assertEquals(listOf(LineColor.BLUE, LineColor.GREEN), lines.map { it.color })
        val green = lines[1].points
        assertEquals(37.501, green.first().latitude, 1e-5)
        assertEquals(37.5015, green.last().latitude, 1e-5)
    }

    @Test fun behind_member_section_is_orange_from_member_to_me() {
        val lines = ui(snapshot(member("p3", 55.6, MemberRelation.BEHIND, 55.6))).lines
        assertEquals(listOf(LineColor.BLUE, LineColor.ORANGE), lines.map { it.color })
        val orange = lines[1].points
        assertEquals(37.5005, orange.first().latitude, 1e-5)
        assertEquals(37.501, orange.last().latitude, 1e-5)
    }

    @Test fun near_off_route_stale_have_no_section() {
        val lines = ui(snapshot(
            member("p1", 120.0, MemberRelation.NEAR, 8.8),
            member("p3", 50.0, MemberRelation.OFF_ROUTE, 61.2),
        )).lines
        assertEquals(listOf(LineColor.BLUE), lines.map { it.color })
        val stale = ui(snapshot(member("p1", 166.8, MemberRelation.STALE, 55.6, age = 40))).lines
        assertEquals(listOf(LineColor.BLUE), stale.map { it.color })
    }

    @Test fun me_unknown_has_no_section() {
        val lines = ui(snapshot(member("p1", 166.8, MemberRelation.ME_UNKNOWN, 55.6), meUnknown = true)).lines
        assertEquals(listOf(LineColor.BLUE), lines.map { it.color })
    }

    @Test fun no_route_no_lines() = assertTrue(ui(snapshot(path = emptyList())).lines.isEmpty())

    // ---- 마커 (R14-6, R14-16) ----

    @Test fun markers_me_and_members_with_tone() {
        val markers = ui(snapshot(
            member("p1", 166.8, MemberRelation.AHEAD, 55.6),
            member("p3", 55.6, MemberRelation.BEHIND, 55.6),
        )).markers
        assertEquals(listOf("나", "민", "수"), markers.map { it.text })
        assertEquals(listOf(MarkerTone.ME, MarkerTone.AHEAD, MarkerTone.BEHIND), markers.map { it.tone })
        assertEquals(me, markers[0].position)
    }

    @Test fun near_stale_off_route_me_unknown_markers_are_gray() {
        listOf(MemberRelation.NEAR, MemberRelation.STALE, MemberRelation.OFF_ROUTE, MemberRelation.ME_UNKNOWN).forEach { r ->
            val m = ui(snapshot(member("p1", 120.0, r, 8.8), meUnknown = r == MemberRelation.ME_UNKNOWN)).markers
            assertEquals(r.name, MarkerTone.GRAY, m.single { it.id == "p1" }.tone)
        }
    }

    @Test fun pins_destination_and_waypoints() {
        val rest = Destination("덕평휴게소", "경기 이천시 마장면", 37.2400, 127.3480)
        val pins = ui(waypoints = listOf(rest)).pins
        assertEquals(listOf("서울역" to PinKind.DESTINATION, "덕평휴게소" to PinKind.WAYPOINT), pins.map { it.name to it.kind })
    }

    // ---- 앞뒤 문구 (R14-13) ----

    @Test fun member_labels() {
        val labels = ui(snapshot(
            member("p1", 760.0, MemberRelation.AHEAD, 649.0),
            member("p3", 0.0, MemberRelation.STALE, 1199.0, age = 40),
        )).memberLabels
        assertEquals("민수 · 앞 약 650 m", labels["p1"])
        assertEquals("수진 · 위치 확인 중 · 40초 전", labels["p3"])
    }

    @Test fun me_unknown_labels_are_null() {
        val labels = ui(snapshot(member("p1", 760.0, MemberRelation.ME_UNKNOWN, 649.0), meUnknown = true)).memberLabels
        assertNull(labels["p1"])
    }

    // ---- 지도 안내 문구 (R14-2, R14-6, R14-16). 앞에 있는 것이 먼저다 ----

    @Test fun banner_no_destination() =
        assertEquals("목적지를 정하면 경로가 보여요", ui(snapshot(path = emptyList()), destination = null).banner)

    @Test fun banner_loading() = assertEquals("경로 찾는 중", ui(snapshot(path = emptyList()), loading = true).banner)

    @Test fun banner_waiting_for_first_route() = assertEquals("경로 찾는 중", ui(snapshot(path = emptyList())).banner)

    @Test fun banner_failed() = assertEquals("경로를 불러오지 못했어요", ui(snapshot(path = emptyList()), failed = true).banner)

    @Test fun banner_me_unknown() = assertEquals("내 위치 확인 중", ui(snapshot(meUnknown = true)).banner)

    @Test fun banner_none_when_ready() = assertNull(ui().banner)

    // ---- 카드 (R14-7, R14-15) ----

    @Test fun cards_hidden_until_received() {
        val u = ui()
        assertNull(u.turnCard)
        assertNull(u.arrivalCard)
    }

    @Test fun cards_shown_when_received() {
        val u = ui(turn = TurnGuide("TURN_EXIT_IC_R", 2000.0, "대왕판교IC 방면"), remain = DrivingRemain(960, 18000))
        assertEquals("2 km · 오른쪽 출구", u.turnCard?.title)
        assertEquals("대왕판교IC 방면", u.turnCard?.subtitle)
        assertEquals("10:24 도착 · 16분 · 18 km", u.arrivalCard)
    }

    @Test fun cards_hidden_without_destination() {
        val u = ui(destination = null, turn = TurnGuide("TURN_RIGHT", 704.0, ""), remain = DrivingRemain(960, 18000))
        assertNull(u.turnCard)
        assertNull(u.arrivalCard)
    }
}
