package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC R14-5·7·8·10, 작업 33(AI 작성): relation 12행, detourSeconds 5행, redrawIntervalSeconds 5행, turnLabel 25행, formatDistance 4행. */
class MapRulesTest {

    // ---- relation: 따로 적지 않은 내 위치는 경로에서 0m, 갱신 후 2초 ----
    private fun rel(
        member: Double,
        memberOff: Double = 5.0,
        memberAge: Int = 2,
        my: Double = 1000.0,
        myOff: Double = 0.0,
        myAge: Int = 2,
    ) = relation(my, myOff, myAge, member, memberOff, memberAge)

    @Test fun ahead() = assertEquals(MemberRelation.AHEAD, rel(1650.0))
    @Test fun behind() = assertEquals(MemberRelation.BEHIND, rel(0.0))
    @Test fun near_ahead_30() = assertEquals(MemberRelation.NEAR, rel(1030.0))
    @Test fun near_behind_30() = assertEquals(MemberRelation.NEAR, rel(970.0))
    @Test fun ahead_31() = assertEquals(MemberRelation.AHEAD, rel(1031.0))
    @Test fun off_route_101() = assertEquals(MemberRelation.OFF_ROUTE, rel(1650.0, memberOff = 101.0))
    @Test fun on_route_at_100() = assertEquals(MemberRelation.AHEAD, rel(1650.0, memberOff = 100.0))
    @Test fun stale_31() = assertEquals(MemberRelation.STALE, rel(1650.0, memberAge = 31))
    @Test fun fresh_at_30() = assertEquals(MemberRelation.AHEAD, rel(1650.0, memberAge = 30))
    @Test fun stale_before_off_route() = assertEquals(MemberRelation.STALE, rel(1650.0, memberOff = 101.0, memberAge = 31))
    @Test fun my_location_stale() = assertEquals(MemberRelation.ME_UNKNOWN, rel(1650.0, myAge = 31))
    @Test fun my_location_off_route() = assertEquals(MemberRelation.ME_UNKNOWN, rel(1650.0, myOff = 101.0))
    @Test fun same_progress_is_near() = assertEquals(MemberRelation.NEAR, rel(1000.0)) // Codex 리뷰

    // ---- detourSeconds ----
    @Test fun detour_180() = assertEquals(180, detourSeconds(900, 1080))
    @Test fun detour_zero() = assertEquals(0, detourSeconds(900, 900))
    @Test fun detour_negative_is_zero() = assertEquals(0, detourSeconds(900, 850))
    @Test fun detour_direct_failed() = assertNull(detourSeconds(null, 1080))
    @Test fun detour_via_failed() = assertNull(detourSeconds(900, null))

    // ---- redrawIntervalSeconds ----
    @Test fun redraw_park() = assertEquals(2, redrawIntervalSeconds(Gear.PARK))
    @Test fun redraw_drive() = assertEquals(10, redrawIntervalSeconds(Gear.DRIVE))
    @Test fun redraw_neutral() = assertEquals(10, redrawIntervalSeconds(Gear.NEUTRAL))
    @Test fun redraw_reverse() = assertEquals(10, redrawIntervalSeconds(Gear.REVERSE))
    @Test fun redraw_unknown() = assertEquals(10, redrawIntervalSeconds(Gear.UNKNOWN))

    // ---- turnLabel: SDK TurnType의 이름(문자열)을 받는다(domain은 SDK를 모른다) ----
    @Test fun turn_labels() {
        val table = mapOf(
            "TURN_STRAIGHT" to "직진",
            "TURN_RIGHT" to "우회전",
            "TURN_LEFT" to "좌회전",
            "TURN_KEEP_R" to "오른쪽 길",
            "TURN_KEEP_L" to "왼쪽 길",
            "TURN_SHARP_R" to "급우회전",
            "TURN_SHARP_L" to "급좌회전",
            "TURN_UTURN" to "유턴",
            "TURN_ENTER_IC_R" to "고속도로 진입",
            "TURN_ENTER_IC_L" to "고속도로 진입",
            "TURN_ENTER_IC_C" to "고속도로 진입",
            "TURN_EXIT_IC_R" to "오른쪽 출구",
            "TURN_EXIT_IC_L" to "왼쪽 출구",
            "TURN_EXIT_IC_C" to "출구",
            "TURN_ENTER_UNDER_R" to "지하차도 진입",
            "TURN_AVOID_UNDER_C" to "지하차도 옆길",
            "TURN_ENTER_OVER_L" to "고가차도 진입",
            "TURN_AVOID_OVER_R" to "고가차도 옆길",
            "TURN_ENTER_ROUNDABOUT_02" to "회전교차로",
            "TURN_ENTER_ROUNDABOUT_12" to "회전교차로",
            "TURN_EXIT_ROUNDABOUT" to "회전교차로 나가기",
            "TURN_TOLL" to "요금소",
            "TURN_ENTER_SERVICEAREA" to "휴게소",
            "TURN_RESTAREA" to "휴게소",
            "TURN_TUNNEL" to "터널",
            "TURN_WAY" to "경유지",
            "TURN_ARRIVE" to "도착",
            "TURN_DEVIDE_MERGE" to "갈림길",
        )
        table.forEach { (type, label) -> assertEquals(type, label, turnLabel(type)) }
    }

    @Test fun turn_unknown_has_no_label() = assertNull(turnLabel("TURN_UNKNOWN"))

    // Codex 리뷰: 접미사 묶음은 허용된 값만 인정한다(SPEC turnLabel 표).
    @Test fun turn_suffix_groups_all_variants() {
        val groups = mapOf(
            "TURN_ENTER_IC_" to "고속도로 진입",
            "TURN_ENTER_UNDER_" to "지하차도 진입",
            "TURN_AVOID_UNDER_" to "지하차도 옆길",
            "TURN_ENTER_OVER_" to "고가차도 진입",
            "TURN_AVOID_OVER_" to "고가차도 옆길",
        )
        groups.forEach { (prefix, label) -> listOf("R", "L", "C").forEach { assertEquals(prefix + it, label, turnLabel(prefix + it)) } }
        listOf("02", "03", "04", "06", "08", "09", "10", "12").forEach {
            assertEquals(it, "회전교차로", turnLabel("TURN_ENTER_ROUNDABOUT_$it"))
        }
    }

    @Test fun turn_invalid_suffix_has_no_label() {
        listOf("TURN_ENTER_IC_X", "TURN_ENTER_UNDER_", "TURN_AVOID_OVER_RR", "TURN_ENTER_ROUNDABOUT_13", "TURN_ENTER_ROUNDABOUT_05", "TURN_ENTER_ROUNDABOUT_")
            .forEach { assertNull(it, turnLabel(it)) }
    }

    @Test fun turn_unlisted_has_no_label() = assertNull(turnLabel("TURN_SOMETHING_NEW"))

    // ---- formatDistance ----
    @Test fun distance_704() = assertEquals("700 m", formatDistance(704.0))
    @Test fun distance_995_is_1km() = assertEquals("1 km", formatDistance(995.0))
    @Test fun distance_1500() = assertEquals("1.5 km", formatDistance(1500.0))
    @Test fun distance_2000() = assertEquals("2 km", formatDistance(2000.0))
    @Test fun distance_zero() = assertEquals("0 m", formatDistance(0.0))
    @Test fun distance_negative_is_zero() = assertEquals("0 m", formatDistance(-1000.0)) // Codex 리뷰
}
