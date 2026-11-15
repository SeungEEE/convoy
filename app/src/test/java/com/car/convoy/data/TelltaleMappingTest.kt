package com.car.convoy.data

import ai.pleos.playground.vehicle.api.model.telltale.TelltaleData
import com.car.convoy.data.vehicle.toReadings
import com.car.convoy.domain.TelltaleReading
import com.car.convoy.domain.TelltaleState
import com.car.convoy.domain.TelltaleType
import org.junit.Assert.assertEquals
import org.junit.Test
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState as SdkState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleType as SdkType

/** SPEC R8-7: SDK 경고등 중 AIRBAG, EMERGENCY_CALL, ABS, TPMS만 domain으로 바꾸고 나머지는 무시한다. */
class TelltaleMappingTest {

    private fun d(type: SdkType, state: SdkState) = TelltaleData(type, state, 0, 0, 0)

    @Test fun maps_four_types_and_states() = assertEquals(
        listOf(
            TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON),
            TelltaleReading(TelltaleType.EMERGENCY_CALL, TelltaleState.BLINK),
            TelltaleReading(TelltaleType.ABS, TelltaleState.OFF),
            TelltaleReading(TelltaleType.TPMS, TelltaleState.ON),
        ),
        listOf(
            d(SdkType.AIRBAG, SdkState.ON),
            d(SdkType.EMERGENCY_CALL, SdkState.BLINK),
            d(SdkType.ABS, SdkState.OFF),
            d(SdkType.TPMS, SdkState.ON),
        ).toReadings(),
    )

    @Test fun ignores_other_types() = assertEquals(
        listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON)),
        listOf(d(SdkType.SEAT_BELT, SdkState.ON), d(SdkType.AIRBAG, SdkState.ON), d(SdkType.MASTER_WARNING, SdkState.BLINK)).toReadings(),
    )

    @Test fun null_list_is_empty() = assertEquals(emptyList<TelltaleReading>(), (null as List<TelltaleData>?).toReadings())
}
