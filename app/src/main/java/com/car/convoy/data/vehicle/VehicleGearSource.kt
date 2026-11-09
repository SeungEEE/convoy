package com.car.convoy.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.content.Context
import com.car.convoy.domain.Gear

/**
 * Vehicle SDK의 기어 구독을 domain [Gear]로 넘긴다(R11-16). 시그니처는 Vehicle 2.3.2 AAR을 javap로 확인했다.
 * 콜백 스레드는 보장되지 않으므로 받는 쪽이 메인 스레드로 옮긴다.
 */
class VehicleGearSource(
    context: Context,
    private val onGear: (Gear) -> Unit,
    private val log: (String) -> Unit,
) {
    private val vehicle = Vehicle(context)

    private val listener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            log("SDK 기어 $gear")
            onGear(gear.toGear())
        }

        override fun onFailed(e: Exception) = log("기어 구독 실패: $e")
    }

    fun start() {
        vehicle.initialize()
        vehicle.getGear().registerGearSelection(listener)
    }

    fun stop() {
        runCatching { vehicle.getGear().unregisterGearSelection(listener) }
        runCatching { vehicle.release() }
    }
}
