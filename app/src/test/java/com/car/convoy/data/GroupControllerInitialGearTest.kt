package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Test

/** 작업 23: 주행마다 새 컨트롤러를 만들 때, SDK가 다시 보내 주지 않는 현재 기어를 넘겨받는다. */
class GroupControllerInitialGearTest {

    private object NoNavi : NaviPort {
        override fun requestRoute(destination: Destination) {}
    }

    @Test fun starts_with_given_gear_and_departs_on_drive() {
        val updates = mutableListOf<GroupUpdate>()
        val c = GroupController("me", listOf("me", "p1"), NoNavi, FakeScheduler(), initialGear = Gear.PARK) { updates += it }
        assertEquals(Gear.PARK, c.currentGear)
        c.onGearChanged(Gear.DRIVE)
        assertEquals(listOf(GroupEvent.GEAR_DEPARTED), updates.map { it.event })
    }
}
