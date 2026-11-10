package com.car.convoy.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import android.content.Context
import com.car.convoy.domain.Gear
import com.car.convoy.domain.TelltaleReading

/**
 * Vehicle SDK의 기어(R11-16)와 경고등(R8-7) 구독을 domain 값으로 넘긴다. Vehicle 하나를 함께 쓴다.
 * 시그니처는 Vehicle 2.3.2 AAR을 javap로 확인했다. 경고등은 PRIVATE_CAR_SAFETY 퍼미션이 필요하다.
 * 콜백 스레드는 보장되지 않으므로 받는 쪽이 메인 스레드로 옮긴다.
 */
class VehicleSource(
    context: Context,
    private val onGear: (Gear) -> Unit,
    private val onTelltales: (List<TelltaleReading>) -> Unit,
    private val log: (String) -> Unit,
) {
    private val vehicle = Vehicle(context)

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            log("SDK 기어 $gear")
            onGear(gear.toGear())
        }

        override fun onFailed(e: Exception) = log("기어 구독 실패: $e")
    }

    private val telltaleListener = object : TelltaleInfoListener {
        override fun onTelltaleInfoUpdated(telltales: Telltales?) {
            val readings = telltales?.telltales.toReadings()
            log("SDK 경고등 ${readings.filter { it.state != com.car.convoy.domain.TelltaleState.OFF }}")
            onTelltales(readings)
        }

        override fun onFailed(e: Exception) = log("경고등 구독 실패: $e")
    }

    fun start() {
        vehicle.initialize()
        vehicle.getGear().registerGearSelection(gearListener)
        runCatching { vehicle.getSafety().registerTelltales(telltaleListener) }
            .onFailure { log("경고등 구독 시작 실패: $it") }
    }

    fun stop() {
        runCatching { vehicle.getGear().unregisterGearSelection(gearListener) }
        runCatching { vehicle.getSafety().unregisterTelltales(telltaleListener) }
        runCatching { vehicle.release() }
    }
}
