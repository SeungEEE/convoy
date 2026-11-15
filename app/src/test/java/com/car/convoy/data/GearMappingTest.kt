package com.car.convoy.data

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import com.car.convoy.data.vehicle.toGear
import com.car.convoy.domain.Gear
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R11-16 기어 변환. */
class GearMappingTest {

    @Test fun park() = assertEquals(Gear.PARK, VehicleGear.GEAR_PARK.toGear())
    @Test fun drive() = assertEquals(Gear.DRIVE, VehicleGear.GEAR_DRIVE.toGear())
    @Test fun neutral() = assertEquals(Gear.NEUTRAL, VehicleGear.GEAR_NEUTRAL.toGear())
    @Test fun reverse() = assertEquals(Gear.REVERSE, VehicleGear.GEAR_REVERSE.toGear())
    @Test fun unknown() = assertEquals(Gear.UNKNOWN, VehicleGear.GEAR_UNKNOWN.toGear())
    @Test fun null_is_unknown() = assertEquals(Gear.UNKNOWN, (null as VehicleGear?).toGear())

    @Test fun numbered_gears_are_drive() = listOf(
        VehicleGear.GEAR_1, VehicleGear.GEAR_2, VehicleGear.GEAR_3,
        VehicleGear.GEAR_4, VehicleGear.GEAR_5, VehicleGear.GEAR_6,
        VehicleGear.GEAR_7, VehicleGear.GEAR_8, VehicleGear.GEAR_9,
    ).forEach { assertEquals("$it", Gear.DRIVE, it.toGear()) }
}
