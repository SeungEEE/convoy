package com.car.convoy.data

import com.car.convoy.domain.AccidentAction
import com.car.convoy.domain.AccidentReport
import com.car.convoy.domain.FamilyEvent
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.ReportLocation
import com.car.convoy.domain.TelltaleReading
import com.car.convoy.domain.TelltaleType
import com.car.convoy.domain.accidentAction
import com.car.convoy.domain.anyAccidentSuspected
import com.car.convoy.domain.nextFamilyState
import com.car.convoy.domain.shouldStartAccidentCheck
import com.car.convoy.domain.port.ConvoyServer
import com.car.convoy.domain.port.LocationPort
import com.car.convoy.domain.suspectedTelltales

private const val CONFIRM_TIMEOUT_MILLIS = 30_000L
private const val LOCATION_POLL_MILLIS = 2_000L

/**
 * 작업 20: 경고등 → "괜찮으세요?" 확인 → 30초 무응답이면 가족 전송(SPEC R8, R9-2).
 * [initialFamily]는 시연에서 ACTIVE로 시작한다(R8-9). 모든 메서드는 같은 스레드(메인)에서 부른다.
 */
class AccidentController(
    private val server: ConvoyServer,
    private val location: LocationPort,
    private val scheduler: Scheduler,
    initialFamily: FamilyState,
    private val onChange: () -> Unit = {},
    /** 보고서에 넣는 실제 시각(R8-10 카드). 30초 측정은 [scheduler]의 단조 시계로 한다. */
    private val wallClock: () -> Long = System::currentTimeMillis,
) {
    var familyState: FamilyState = initialFamily
        private set

    /** 지금 "괜찮으세요?"를 묻는 중인가. */
    var checking = false
        private set

    /** 가장 최근 확인에서 실제로 가족에게 보낸 알림(R8-10). 보내지 않았거나 새 확인 중이면 null. */
    var lastSentReport: AccidentReport? = null
        private set

    private var wasSuspected = false
    private var startedAt = 0L
    private var telltales: List<TelltaleType> = emptyList()
    private var latestLocation: ReportLocation? = null
    private var timeout: Cancellable? = null
    private var poll: Cancellable? = null

    /** 남은 초(올림). 확인 중이 아니면 0. */
    val remainingSeconds: Int
        get() = if (!checking) 0 else {
            val left = CONFIRM_TIMEOUT_MILLIS - (scheduler.nowMillis() - startedAt)
            ((left + 999) / 1000).toInt().coerceAtLeast(0)
        }

    /** SPEC R8-1, R8-4, R8-6: 경고등 목록이 올 때마다 부른다. */
    fun onTelltales(readings: List<TelltaleReading>) {
        val suspected = anyAccidentSuspected(readings)
        if (shouldStartAccidentCheck(wasSuspected, suspected, checking)) start(suspectedTelltales(readings))
        wasSuspected = suspected
    }

    /** SPEC R8-2: [괜찮아요]. 아무것도 보내지 않는다. */
    fun respondOk() {
        if (!checking) return
        check(accidentAction(elapsedSeconds(), responded = true, hasFamily = hasFamily()) == AccidentAction.SAFE)
        finish()
    }

    /** SPEC R8-8: 확인 중에 온 위치만 쓴다. */
    fun onLocation(value: ReportLocation) {
        if (checking) latestLocation = value
    }

    /** SPEC R9-2: 공유를 끄면 이후 가족에게 아무것도 보내지 않는다. */
    fun disableFamilySharing() {
        familyState = nextFamilyState(familyState, FamilyEvent.DISABLE)
        onChange()
    }

    private fun start(types: List<TelltaleType>) {
        checking = true
        startedAt = scheduler.nowMillis()
        telltales = types
        latestLocation = null
        // R8-10: 카드는 이번 확인에서 실제로 보낸 경우에만 보여 준다. 이전 확인의 보고서는 지운다.
        lastSentReport = null
        requestLocationRepeatedly()
        timeout = scheduler.schedule(CONFIRM_TIMEOUT_MILLIS) { onTimeout() }
        onChange()
    }

    private fun requestLocationRepeatedly() {
        location.requestLocation()
        poll = scheduler.schedule(LOCATION_POLL_MILLIS) { if (checking) requestLocationRepeatedly() }
    }

    private fun onTimeout() {
        // 예약 시계(Handler)와 측정 시계가 어긋나 일찍 울리면, 끝내지 않고 남은 시간만큼 다시 기다린다(R8-3).
        val leftMillis = CONFIRM_TIMEOUT_MILLIS - (scheduler.nowMillis() - startedAt)
        if (leftMillis > 0) {
            timeout = scheduler.schedule(leftMillis) { onTimeout() }
            return
        }
        // R8-3: 정확히 30초가 되면 판단한다. R8-5: 가족이 없으면 전송하지 않는다.
        if (accidentAction(elapsedSeconds(), responded = false, hasFamily = hasFamily()) == AccidentAction.NOTIFY_FAMILY) {
            val report = AccidentReport(wallClock(), latestLocation, telltales)
            server.notifyFamily(report)
            lastSentReport = report
        }
        finish()
    }

    private fun finish() {
        checking = false
        timeout?.cancel()
        poll?.cancel()
        timeout = null
        poll = null
        onChange()
    }

    private fun hasFamily() = familyState == FamilyState.ACTIVE

    private fun elapsedSeconds() = ((scheduler.nowMillis() - startedAt) / 1000).toInt()
}
