package com.car.convoy.domain

import com.car.convoy.domain.TelltaleState.*
import com.car.convoy.domain.TelltaleType.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R8-1, R8-4, R8-6: 경고등 목록에서 사고 의심 여부와 새 확인 시작 시점. */
class AccidentCheckTest {

    private fun r(type: TelltaleType, state: TelltaleState) = TelltaleReading(type, state)

    // ---- 목록에 사고 의심 항목이 하나라도 있는가 ----
    @Test fun empty_list_is_not_suspected() = assertFalse(anyAccidentSuspected(emptyList()))
    @Test fun airbag_on_in_list() = assertTrue(anyAccidentSuspected(listOf(r(ABS, ON), r(AIRBAG, ON))))
    @Test fun emergency_call_blink_in_list() = assertTrue(anyAccidentSuspected(listOf(r(EMERGENCY_CALL, BLINK))))
    @Test fun only_other_telltales() = assertFalse(anyAccidentSuspected(listOf(r(ABS, ON), r(TPMS, ON), r(AIRBAG, OFF))))

    @Test fun suspected_types_lists_only_active_accident_telltales() = assertEquals(
        listOf(AIRBAG),
        suspectedTelltales(listOf(r(AIRBAG, ON), r(EMERGENCY_CALL, OFF), r(ABS, ON))),
    )

    // ---- 새 확인 시작: 의심 아님 → 의심으로 바뀌고, 확인 중이 아닐 때만 ----
    @Test fun rising_edge_starts() = assertTrue(shouldStartAccidentCheck(wasSuspected = false, isSuspected = true, checking = false))
    @Test fun still_on_does_not_restart() = assertFalse(shouldStartAccidentCheck(wasSuspected = true, isSuspected = true, checking = false))
    @Test fun not_suspected_does_not_start() = assertFalse(shouldStartAccidentCheck(wasSuspected = false, isSuspected = false, checking = false))
    @Test fun already_checking_does_not_start() = assertFalse(shouldStartAccidentCheck(wasSuspected = false, isSuspected = true, checking = true))
}
