package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupMemberState
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-22, Codex 리뷰: [주행 끝내기]로 버린 세션의 가상 멤버 예약(6초 뒤 경로 시작)은 취소한다. */
class GroupControllerCloseTest {

    private object NoNavi : NaviPort {
        override fun requestRoute(destination: Destination) {}
    }

    private val dest = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)

    @Test fun close_cancels_pending_virtual_route_start() {
        val scheduler = FakeScheduler()
        val updates = mutableListOf<GroupUpdate>()
        val c = GroupController("me", listOf("me", "p1"), NoNavi, scheduler) { updates += it }
        c.setDestination(dest)
        c.close()
        scheduler.advanceBy(10_000)
        assertTrue(updates.none { it.memberId == "p1" })
        assertEquals(GroupMemberState.REROUTING, c.session.memberState("p1"))
    }

    @Test fun without_close_virtual_member_starts_after_6s() {
        val scheduler = FakeScheduler()
        val c = GroupController("me", listOf("me", "p1"), NoNavi, scheduler)
        c.setDestination(dest)
        scheduler.advanceBy(6_000)
        assertEquals(GroupMemberState.DRIVING, c.session.memberState("p1"))
    }
}
