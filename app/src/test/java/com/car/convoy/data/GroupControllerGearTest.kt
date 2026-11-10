package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.port.NaviPort
import org.junit.Assert.assertEquals
import org.junit.Test

/** 작업 15: 화면이 R10 판단에 쓰는 현재 기어. 처음에는 UNKNOWN(주행 중과 같게 취급)이다. */
class GroupControllerGearTest {

    private object NoNavi : NaviPort {
        override fun requestRoute(destination: Destination) {}
    }

    private val c = GroupController("me", listOf("me", "m1"), NoNavi, FakeScheduler())

    @Test fun starts_unknown() = assertEquals(Gear.UNKNOWN, c.currentGear)

    @Test fun follows_last_gear() {
        c.onGearChanged(Gear.PARK)
        assertEquals(Gear.PARK, c.currentGear)
        c.onGearChanged(Gear.DRIVE)
        assertEquals(Gear.DRIVE, c.currentGear)
    }
}
