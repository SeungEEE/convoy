package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R8 예시표 전체. */
class AccidentTest {

    // ---- isAccidentSuspected ----
    @Test
    fun airbag_on_is_suspected() {
        assertTrue(isAccidentSuspected(TelltaleType.AIRBAG, TelltaleState.ON))
    }

    @Test
    fun airbag_blink_is_suspected() {
        assertTrue(isAccidentSuspected(TelltaleType.AIRBAG, TelltaleState.BLINK))
    }

    @Test
    fun emergency_call_on_is_suspected() {
        assertTrue(isAccidentSuspected(TelltaleType.EMERGENCY_CALL, TelltaleState.ON))
    }

    @Test
    fun airbag_off_is_not_suspected() {
        assertFalse(isAccidentSuspected(TelltaleType.AIRBAG, TelltaleState.OFF))
    }

    @Test
    fun abs_on_is_not_suspected() {
        assertFalse(isAccidentSuspected(TelltaleType.ABS, TelltaleState.ON))
    }

    @Test
    fun tpms_on_is_not_suspected() {
        assertFalse(isAccidentSuspected(TelltaleType.TPMS, TelltaleState.ON))
    }

    // ---- accidentAction ----
    @Test
    fun at_0s_no_response_waits() {
        assertEquals(AccidentAction.WAITING, accidentAction(0, responded = false, hasFamily = true))
    }

    @Test
    fun at_29s_no_response_waits() {
        assertEquals(AccidentAction.WAITING, accidentAction(29, responded = false, hasFamily = true))
    }

    @Test
    fun at_30s_no_response_notifies() {
        assertEquals(AccidentAction.NOTIFY_FAMILY, accidentAction(30, responded = false, hasFamily = true))
    }

    @Test
    fun at_31s_no_response_notifies() {
        assertEquals(AccidentAction.NOTIFY_FAMILY, accidentAction(31, responded = false, hasFamily = true))
    }

    @Test
    fun at_29s_responded_is_safe() {
        assertEquals(AccidentAction.SAFE, accidentAction(29, responded = true, hasFamily = true))
    }

    @Test
    fun at_30s_responded_is_safe() {
        assertEquals(AccidentAction.SAFE, accidentAction(30, responded = true, hasFamily = true))
    }

    @Test
    fun at_30s_no_family_no_action() {
        assertEquals(AccidentAction.NO_ACTION, accidentAction(30, responded = false, hasFamily = false))
    }
}
