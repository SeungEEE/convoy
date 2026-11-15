package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GroupEffect
import com.car.convoy.domain.GroupMemberState.*
import com.car.convoy.domain.GroupStatus
import com.car.convoy.domain.MemberEffect
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 작업 14: 내 차의 내비·기어 이벤트와 가상 멤버를 GroupSession에 잇는다 (R7, R11-3, R11-4, R11-17, R11-18). */
class GroupControllerTest {

    private class RecordingNavi : NaviPort {
        val requested = mutableListOf<Destination>()
        override fun requestRoute(destination: Destination) { requested += destination }
    }

    private val dest = Destination("서울역", "서울 용산구 한강대로 405", 37.5547, 126.9707)
    private val navi = RecordingNavi()
    private val scheduler = FakeScheduler()
    private val updates = mutableListOf<GroupUpdate>()
    private val c = GroupController("me", listOf("me", "m1", "m2"), navi, scheduler) { updates += it }

    private fun state(id: String) = c.session.memberState(id)

    @Test fun set_destination_requests_my_route_and_virtual_members_start_after_six_seconds() {
        c.setDestination(dest)
        assertEquals(listOf(dest), navi.requested)
        listOf("me", "m1", "m2").forEach { assertEquals(REROUTING, state(it)) }
        scheduler.advanceBy(6_000)
        assertEquals(DRIVING, state("m1"))
        assertEquals(DRIVING, state("m2"))
        assertEquals(REROUTING, state("me"))
    }

    @Test fun my_route_started_with_same_name_drives() {
        c.setDestination(dest)
        c.onRouteStarted("서울역")
        assertEquals(DRIVING, state("me"))
    }

    @Test fun my_stale_route_started_is_ignored() {
        c.setDestination(dest)
        c.onRouteStarted("강남역")
        assertEquals(REROUTING, state("me"))
    }

    @Test fun my_gear_park_to_drive_shares_departure() {
        c.onGearChanged(Gear.PARK)
        c.onGearChanged(Gear.DRIVE)
        assertEquals(DRIVING, state("me"))
        assertEquals(MemberEffect.SHARE_DEPARTED, updates.last().result.memberEffect)
    }

    @Test fun first_gear_reading_is_not_departure() {
        c.onGearChanged(Gear.DRIVE)
        assertEquals(IDLE, state("me"))
        assertTrue(updates.isEmpty())
    }

    @Test fun with_destination_drive_to_park_is_not_arrival() {
        c.setDestination(dest)
        c.onRouteStarted("서울역")
        c.onGearChanged(Gear.DRIVE)
        c.onGearChanged(Gear.PARK)
        assertEquals(DRIVING, state("me"))
    }

    @Test fun without_destination_drive_to_park_is_arrival() {
        c.onGearChanged(Gear.PARK)
        c.onGearChanged(Gear.DRIVE)
        c.onGearChanged(Gear.PARK)
        assertEquals(ARRIVED, state("me"))
    }

    @Test fun my_arrival_needs_same_name_and_address() {
        c.setDestination(dest)
        c.onRouteStarted("서울역")
        c.onDestinationArrived("서울역", "다른 주소")
        assertEquals(DRIVING, state("me"))
        c.onDestinationArrived("서울역", "서울 용산구 한강대로 405")
        assertEquals(ARRIVED, state("me"))
    }

    @Test fun everyone_arrives_and_group_finishes_once() {
        c.setDestination(dest)
        scheduler.advanceBy(6_000)
        c.onRouteStarted("서울역")
        c.server.arrive("m1")
        c.server.arrive("m2")
        c.onDestinationArrived("서울역", "서울 용산구 한강대로 405")
        assertEquals(GroupStatus.FINISHED, c.session.status)
        assertEquals(1, updates.count { it.result.groupEffect == GroupEffect.FINISH_GROUP })
        assertEquals(GroupUpdate("me", com.car.convoy.domain.GroupEvent.NAVI_ARRIVED,
            com.car.convoy.domain.MemberEventResult(MemberEffect.SHARE_ARRIVED, GroupEffect.FINISH_GROUP)), updates.last())
    }
}
