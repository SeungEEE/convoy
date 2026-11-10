package com.car.convoy.data

import com.car.convoy.domain.AccidentReport
import com.car.convoy.domain.Destination
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.ReportLocation
import com.car.convoy.domain.TelltaleReading
import com.car.convoy.domain.TelltaleState
import com.car.convoy.domain.TelltaleType
import com.car.convoy.domain.port.ConvoyServer
import com.car.convoy.domain.port.LocationPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 작업 20: 경고등 → "괜찮으세요?" 확인 → 30초 → 가족 전송 (SPEC R8-1~10, R9-2). */
class AccidentControllerTest {

    private class RecordingServer : ConvoyServer {
        val reports = mutableListOf<AccidentReport>()
        override fun sendDestination(memberId: String, destination: Destination) {}
        override fun notifyFamily(report: AccidentReport) { reports += report }
    }

    private class CountingLocation : LocationPort {
        var requests = 0
        override fun requestLocation() { requests++ }
    }

    private val server = RecordingServer()
    private val location = CountingLocation()
    private val scheduler = FakeScheduler()
    private val c = AccidentController(server, location, scheduler, FamilyState.ACTIVE, wallClock = scheduler::nowMillis)

    private val airbagOn = listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON))
    private val allOff = listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.OFF))
    private val here = ReportLocation(37.4127, 127.0947, "성남시 수정구 시흥동")

    // ---- 확인 시작 (R8-1, R8-4, R8-6) ----
    @Test fun airbag_starts_check() {
        c.onTelltales(airbagOn)
        assertTrue(c.checking)
        assertEquals(30, c.remainingSeconds)
    }

    @Test fun other_telltale_does_not_start() {
        c.onTelltales(listOf(TelltaleReading(TelltaleType.ABS, TelltaleState.ON)))
        assertFalse(c.checking)
    }

    @Test fun repeated_list_while_on_does_not_restart_after_ok() {
        c.onTelltales(airbagOn)
        c.respondOk()
        c.onTelltales(airbagOn)
        assertFalse(c.checking)
    }

    @Test fun off_then_on_starts_again() {
        c.onTelltales(airbagOn)
        c.respondOk()
        c.onTelltales(allOff)
        c.onTelltales(airbagOn)
        assertTrue(c.checking)
    }

    @Test fun remaining_seconds_counts_down() {
        c.onTelltales(airbagOn)
        scheduler.advanceBy(12_000)
        assertEquals(18, c.remainingSeconds)
    }

    // ---- 응답 (R8-2) ----
    @Test fun ok_before_30s_sends_nothing() {
        c.onTelltales(airbagOn)
        scheduler.advanceBy(29_999)
        c.respondOk()
        scheduler.advanceBy(10_000)
        assertEquals(emptyList<AccidentReport>(), server.reports)
        assertFalse(c.checking)
        assertNull(c.lastSentReport)
    }

    // ---- 30초 무응답 → 가족 전송 (R8-3, R8-8) ----
    @Test fun no_response_at_exactly_30s_notifies_family_with_latest_location() {
        c.onTelltales(airbagOn)
        scheduler.advanceBy(5_000)
        c.onLocation(ReportLocation(37.0, 127.0, "이전 위치"))
        scheduler.advanceBy(10_000)
        c.onLocation(here)
        assertEquals(emptyList<AccidentReport>(), server.reports)
        scheduler.advanceBy(15_000)
        assertEquals(1, server.reports.size)
        val report = server.reports.single()
        assertEquals(here, report.location)
        assertEquals(listOf(TelltaleType.AIRBAG), report.telltales)
        assertEquals(30_000L, report.atMillis)
        assertEquals(report, c.lastSentReport)
        assertFalse(c.checking)
    }

    @Test fun no_location_sends_null_location() {
        c.onTelltales(airbagOn)
        scheduler.advanceBy(30_000)
        assertNull(server.reports.single().location)
    }

    @Test fun location_outside_check_is_not_used() {
        c.onLocation(here)
        c.onTelltales(airbagOn)
        scheduler.advanceBy(30_000)
        assertNull(server.reports.single().location)
    }

    @Test fun location_is_requested_at_start_and_every_2s_until_end() {
        c.onTelltales(airbagOn)
        assertEquals(1, location.requests)
        scheduler.advanceBy(10_000)
        assertEquals(6, location.requests)
        scheduler.advanceBy(20_000)
        val atEnd = location.requests
        scheduler.advanceBy(10_000)
        assertEquals(atEnd, location.requests)
    }

    // ---- 가족 없음·공유 끔 (R8-5, R9-2) ----
    @Test fun no_family_checks_but_does_not_send() {
        val noFamily = AccidentController(server, location, scheduler, FamilyState.NONE)
        noFamily.onTelltales(airbagOn)
        assertTrue(noFamily.checking)
        scheduler.advanceBy(30_000)
        assertEquals(emptyList<AccidentReport>(), server.reports)
        assertNull(noFamily.lastSentReport)
    }

    @Test fun disabling_family_stops_sending() {
        c.disableFamilySharing()
        assertEquals(FamilyState.NONE, c.familyState)
        c.onTelltales(airbagOn)
        scheduler.advanceBy(30_000)
        assertEquals(emptyList<AccidentReport>(), server.reports)
    }
}
