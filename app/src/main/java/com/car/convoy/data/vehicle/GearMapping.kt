package com.car.convoy.data.vehicle

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import com.car.convoy.domain.Gear

/** SPEC R11-16: SDK 기어를 domain 기어로 바꾼다. GEAR_1~9는 DRIVE, null은 UNKNOWN이다. */
fun VehicleGear?.toGear(): Gear = when (this) {
    VehicleGear.GEAR_PARK -> Gear.PARK
    VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
    VehicleGear.GEAR_REVERSE -> Gear.REVERSE
    VehicleGear.GEAR_DRIVE,
    VehicleGear.GEAR_1, VehicleGear.GEAR_2, VehicleGear.GEAR_3,
    VehicleGear.GEAR_4, VehicleGear.GEAR_5, VehicleGear.GEAR_6,
    VehicleGear.GEAR_7, VehicleGear.GEAR_8, VehicleGear.GEAR_9 -> Gear.DRIVE
    VehicleGear.GEAR_UNKNOWN, null -> Gear.UNKNOWN
}
