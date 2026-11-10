package com.car.convoy.data

import com.car.convoy.domain.AccidentReport
import com.car.convoy.domain.Destination
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.TelltaleReading
import com.car.convoy.domain.TelltaleState
import com.car.convoy.domain.TelltaleType
import com.car.convoy.domain.port.ConvoyServer
import com.car.convoy.domain.port.LocationPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC R8-10: 알림 카드는 이번 확인에서 실제로 보낸 경우에만. 새 확인이 시작되면 이전 보고서를 지운다(에뮬레이터 확인 중 발견). */
class AccidentControllerReportResetTest {

    private class RecordingServer : ConvoyServer {
        val reports = mutableListOf<AccidentReport>()
        override fun sendDestination(memberId: String, destination: Destination) {}
        override fun notifyFamily(report: AccidentReport) { reports += report }
    }

    private object NoLocation : LocationPort {
        override fun requestLocation() {}
    }

    private val server = RecordingServer()
    private val scheduler = FakeScheduler()
    private val c = AccidentController(server, NoLocation, scheduler, FamilyState.ACTIVE)

    private val on = listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON))
    private val off = listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.OFF))

    @Test fun new_check_clears_previous_report_and_ok_keeps_it_empty() {
        c.onTelltales(on)
        scheduler.advanceBy(30_000)
        assertEquals(1, server.reports.size)
        c.onTelltales(off)
        c.onTelltales(on)
        assertNull(c.lastSentReport)
        c.respondOk()
        assertNull(c.lastSentReport)
        assertEquals(1, server.reports.size)
    }
}
