package com.car.convoy.data.vehicle

import ai.pleos.playground.vehicle.api.model.telltale.TelltaleData
import com.car.convoy.domain.TelltaleReading
import com.car.convoy.domain.TelltaleState
import com.car.convoy.domain.TelltaleType
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState as SdkState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleType as SdkType

/** SPEC R8-7: SDK 경고등 중 AIRBAG, EMERGENCY_CALL, ABS, TPMS만 바꾸고 나머지는 버린다. */
fun List<TelltaleData>?.toReadings(): List<TelltaleReading> = orEmpty().mapNotNull { d ->
    val type = when (d.type) {
        SdkType.AIRBAG -> TelltaleType.AIRBAG
        SdkType.EMERGENCY_CALL -> TelltaleType.EMERGENCY_CALL
        SdkType.ABS -> TelltaleType.ABS
        SdkType.TPMS -> TelltaleType.TPMS
        else -> null
    } ?: return@mapNotNull null
    val state = when (d.state) {
        SdkState.ON -> TelltaleState.ON
        SdkState.BLINK -> TelltaleState.BLINK
        else -> TelltaleState.OFF
    }
    TelltaleReading(type, state)
}
