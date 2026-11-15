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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC R8-3: 타이머가 30초보다 일찍 울려도(예약 시계와 측정 시계가 어긋남) 확인을 끝내지 않고 남은 시간만큼 다시 기다린다.
 * 에뮬레이터에서 21.8초 만에 타이머가 울려 아무것도 보내지 않고 끝난 것을 발견했다.
 */
class AccidentControllerEarlyTimerTest {

    /** 예약은 요청한 지연대로 실행하지만, nowMillis는 실제 흐른 시간의 [ratio]배만 보고하는 스케줄러. */
    private class SkewedScheduler(private val ratio: Double) : Scheduler {
        val inner = FakeScheduler()
        private var t = 0L
        override fun nowMillis(): Long = (t * ratio).toLong()
        override fun schedule(delayMillis: Long, action: () -> Unit) = inner.schedule(delayMillis, action)
        fun advanceBy(ms: Long) {
            // 1ms 단위로 흘려 측정 시계도 함께 간다.
            repeat((ms / 100).toInt()) { t += 100; inner.advanceBy(100) }
        }
    }

    private class RecordingServer : ConvoyServer {
        val reports = mutableListOf<AccidentReport>()
        override fun sendDestination(memberId: String, destination: Destination) {}
        override fun notifyFamily(report: AccidentReport) { reports += report }
    }

    private object NoLocation : LocationPort {
        override fun requestLocation() {}
    }

    @Test fun early_timer_keeps_checking_then_notifies_at_30s_measured() {
        val server = RecordingServer()
        val s = SkewedScheduler(ratio = 0.75)
        val c = AccidentController(server, NoLocation, s, FamilyState.ACTIVE, wallClock = { 0L })
        c.onTelltales(listOf(TelltaleReading(TelltaleType.AIRBAG, TelltaleState.ON)))
        s.advanceBy(30_000) // 측정 시계로는 22.5초: 아직 기다려야 한다
        assertTrue(c.checking)
        assertEquals(0, server.reports.size)
        s.advanceBy(10_100) // 측정 시계로 30초를 넘는다
        assertEquals(1, server.reports.size)
    }
}
