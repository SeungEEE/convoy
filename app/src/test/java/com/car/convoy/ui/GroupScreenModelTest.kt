package com.car.convoy.ui

import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.GroupMemberState
import com.car.convoy.domain.GroupSession
import com.car.convoy.domain.port.ConvoyServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-20: 그룹 주행 화면에 보일 값. */
class GroupScreenModelTest {

    private object NoServer : ConvoyServer {
        override fun sendDestination(memberId: String, destination: Destination) {}
    }

    private val ids = listOf("me", "m1", "m2")
    private val names = mapOf("me" to "나", "m1" to "일행 1", "m2" to "일행 2")
    private val dest = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)

    private fun model(
        session: GroupSession = GroupSession("me", ids, NoServer),
        isLeader: Boolean = true,
        gear: Gear = Gear.PARK,
        canRetry: Boolean = false,
    ) = groupScreenModel(session, "me", ids, names, isLeader, gear, canRetry)

    // ---- 상태 문구 ----
    @Test fun labels() {
        assertEquals("출발 전", memberStateLabel(GroupMemberState.IDLE))
        assertEquals("경로 찾는 중", memberStateLabel(GroupMemberState.REROUTING))
        assertEquals("주행 중", memberStateLabel(GroupMemberState.DRIVING))
        assertEquals("도착", memberStateLabel(GroupMemberState.ARRIVED))
    }

    // ---- 멤버 목록 ----
    @Test fun members_in_order_with_names_labels_and_virtual_flag() {
        val s = GroupSession("me", ids, NoServer)
        s.setDestination("me", dest)
        assertEquals(
            listOf(
                MemberRow("me", "나", GroupMemberState.REROUTING, "경로 찾는 중", isVirtual = false),
                MemberRow("m1", "일행 1", GroupMemberState.REROUTING, "경로 찾는 중", isVirtual = true),
                MemberRow("m2", "일행 2", GroupMemberState.REROUTING, "경로 찾는 중", isVirtual = true),
            ),
            model(session = s).members,
        )
    }

    @Test fun demo_panel_lists_virtual_members_only() = assertEquals(listOf("m1", "m2"), model().demoMemberIds)

    // ---- 목적지와 완료 배너 ----
    @Test fun no_destination() = assertNull(model().destinationName)

    @Test fun destination_name() {
        val s = GroupSession("me", ids, NoServer)
        s.setDestination("me", dest)
        assertEquals("서울역", model(session = s).destinationName)
    }

    @Test fun finished_banner() {
        val s = GroupSession("me", ids, NoServer)
        s.setDestination("me", dest)
        ids.forEach { s.onMemberEvent(it, GroupEvent.ROUTE_STARTED) }
        assertFalse(model(session = s).finished)
        ids.forEach { s.onMemberEvent(it, GroupEvent.NAVI_ARRIVED) }
        assertTrue(model(session = s).finished)
    }

    // ---- 목적지 고르기: 리더 + canPerform(SET_GROUP_DESTINATION) + 그룹 ACTIVE(R11-9) ----
    @Test fun leader_parked_can_choose() = assertTrue(model().canChooseDestination)
    @Test fun leader_driving_cannot_choose() = assertFalse(model(gear = Gear.DRIVE).canChooseDestination)
    @Test fun leader_unknown_gear_cannot_choose() = assertFalse(model(gear = Gear.UNKNOWN).canChooseDestination)
    @Test fun member_cannot_choose() = assertFalse(model(isLeader = false).canChooseDestination)

    @Test fun finished_group_cannot_choose() {
        val s = GroupSession("me", ids, NoServer)
        s.setDestination("me", dest)
        ids.forEach { s.onMemberEvent(it, GroupEvent.ROUTE_STARTED) }
        ids.forEach { s.onMemberEvent(it, GroupEvent.NAVI_ARRIVED) }
        assertFalse(model(session = s).canChooseDestination)
    }

    // ---- 다시 요청 버튼 (R11-19) ----
    @Test fun retry_hidden_by_default() = assertFalse(model().showRetry)
    @Test fun retry_shown_when_controller_allows() = assertTrue(model(canRetry = true).showRetry)
}
