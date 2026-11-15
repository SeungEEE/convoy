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
import org.junit.Test

/** SPEC R8-10: 카드의 시각은 실제 시각이다. 30초 측정(단조 시계)과 보고서 시각(벽시계)을 나눈다. Codex 리뷰. */
class AccidentControllerWallClockTest {

    private class RecordingServer : ConvoyServer {
        val reports = mutableListOf<AccidentReport>()
        override fun sendDestination(memberId: String, destination: Destination) {}
        override fun notifyFamily(report: AccidentReport) { reports += report }
    }

    private object NoLocation : LocationPort {
        override fun requestLocation() {}
    }

    @Test fun report_time_comes_from_wall_clock() {
        val server = RecordingServer()
        val scheduler = FakeScheduler()
        val wallStart = 1_794_268_800_000L // 2026-11-10 00:00 UTC 근처
        val c = AccidentController(server, NoLocation, scheduler, FamilyState.ACTIVE, wallClock = { wallStart + scheduler.nowMillis() })
        c.onTelltales(listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON)))
        scheduler.advanceBy(30_000)
        assertEquals(wallStart + 30_000, server.reports.single().atMillis)
    }
}
