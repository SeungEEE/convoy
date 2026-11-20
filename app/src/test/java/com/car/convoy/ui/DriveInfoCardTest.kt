package com.car.convoy.ui

import com.car.convoy.data.WaypointProposal
import com.car.convoy.domain.ApplyState
import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.ProposalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R14-19, R13-7, R10 "경유지 다시 보내기" (작업 39, AI 작성): 정보 카드의 칸 조건과 멤버 줄 수. 나(me)가 리더다. */
class DriveInfoCardTest {

    private val names = mapOf("me" to "준희", "p1" to "민수", "p3" to "수진")
    private val rest = Destination("덕평휴게소", "경기 이천시 마장면", 37.24, 127.348)

    private fun panel(
        proposal: WaypointProposal? = null,
        waypoints: List<Destination> = emptyList(),
        apply: Map<String, ApplyState> = emptyMap(),
        gear: Gear = Gear.PARK,
    ) = waypointPanelModel("me", proposal, waypoints, apply, names, gear, true, true)

    // ---- R14-19 WAYPOINT: 경유지 이름, applyText, notice, showResend 중 하나라도 있으면 상태가 있다 ----

    @Test fun no_waypoint_status() {
        assertFalse(panel().hasStatus)
    }

    @Test fun waypoint_names_count_as_status() {
        assertTrue(panel(waypoints = listOf(rest)).hasStatus)
    }

    @Test fun apply_text_counts_as_status() {
        assertTrue(panel(apply = mapOf("me" to ApplyState.SENT)).hasStatus)
    }

    @Test fun notice_counts_as_status() {
        assertTrue(panel(proposal = WaypointProposal("p3", rest, ProposalState.REJECTED)).hasStatus)
    }

    @Test fun pending_request_alone_is_not_status() {
        // 요청은 REQUEST 칸이라 이 조건에 넣지 않는다.
        assertFalse(panel(proposal = WaypointProposal("p3", rest, ProposalState.PENDING)).hasStatus)
    }

    // ---- R13-7, R10: [다시 보내기]는 실패가 있을 때 보이고, 정차 중에만 누를 수 있다 ----

    @Test fun resend_enabled_when_parked() {
        val m = panel(waypoints = listOf(rest), apply = mapOf("me" to ApplyState.APPLIED, "p1" to ApplyState.FAILED))
        assertTrue(m.showResend)
        assertTrue(m.canResend)
    }

    @Test fun resend_locked_while_driving() {
        val m = panel(waypoints = listOf(rest), apply = mapOf("me" to ApplyState.APPLIED, "p1" to ApplyState.FAILED), gear = Gear.DRIVE)
        assertTrue(m.showResend)
        assertFalse(m.canResend)
    }

    @Test fun no_resend_without_failure() {
        assertFalse(panel(waypoints = listOf(rest), apply = mapOf("me" to ApplyState.APPLIED)).canResend)
    }

    // ---- R14-19 MEMBERS: 3줄까지, 넘치면 넷째 줄에 "외 n명" ----

    @Test fun member_lines_up_to_three_as_is() {
        val labels = listOf("민수 · 앞 약 650 m", "수진 · 뒤 약 1.2 km", "지훈 · 나란히")
        assertEquals(labels, infoCardMemberLines(labels))
    }

    @Test fun member_lines_four_become_three_and_more() {
        val labels = listOf("민수 · 앞 약 650 m", "수진 · 뒤 약 1.2 km", "지훈 · 나란히", "하늘 · 다른 길로 이동 중")
        assertEquals(labels.take(3) + "외 1명", infoCardMemberLines(labels))
    }

    @Test fun member_lines_empty() {
        assertEquals(emptyList<String>(), infoCardMemberLines(emptyList()))
    }
}
