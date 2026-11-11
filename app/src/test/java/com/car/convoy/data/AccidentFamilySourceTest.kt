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

/** SPEC R9-1, R8-5(작업 24): 사고 쪽은 가족 상태를 FamilyController에서 읽는다. 30초가 지난 때의 상태로 보낼지 정한다. */
class AccidentFamilySourceTest {

    private class RecordingServer : ConvoyServer {
        val reports = mutableListOf<AccidentReport>()
        override fun sendDestination(memberId: String, destination: Destination) {}
        override fun notifyFamily(report: AccidentReport) { reports += report }
    }

    private object NoLocation : LocationPort {
        override fun requestLocation() {}
    }

    private val airbagOn = listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON))

    private fun run(state: () -> FamilyState): Int {
        val server = RecordingServer()
        val scheduler = FakeScheduler()
        val c = AccidentController(server, NoLocation, scheduler, FamilyState.NONE, wallClock = scheduler::nowMillis, familySource = state)
        c.onTelltales(airbagOn)
        scheduler.advanceBy(30_000)
        return server.reports.size
    }

    @Test fun sends_when_source_says_active() = assertEquals(1, run { FamilyState.ACTIVE })

    @Test fun no_send_when_source_says_pending() = assertEquals(0, run { FamilyState.PENDING })

    @Test fun no_send_when_source_says_none() = assertEquals(0, run { FamilyState.NONE })

    @Test fun reads_state_at_timeout() {
        var state = FamilyState.NONE
        val server = RecordingServer()
        val scheduler = FakeScheduler()
        val c = AccidentController(server, NoLocation, scheduler, FamilyState.NONE, wallClock = scheduler::nowMillis, familySource = { state })
        c.onTelltales(airbagOn)
        scheduler.advanceBy(10_000)
        state = FamilyState.ACTIVE
        scheduler.advanceBy(20_000)
        assertEquals(1, server.reports.size)
    }

    @Test fun family_state_reflects_source() {
        val c = AccidentController(RecordingServer(), NoLocation, FakeScheduler(), FamilyState.NONE, familySource = { FamilyState.ACTIVE })
        assertEquals(FamilyState.ACTIVE, c.familyState)
    }
}
