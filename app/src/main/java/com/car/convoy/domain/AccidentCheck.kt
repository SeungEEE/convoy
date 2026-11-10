package com.car.convoy.domain

/** SPEC R8. 경고등 하나의 값. 어댑터가 SDK 목록에서 만든다(R8-7). */
data class TelltaleReading(val type: TelltaleType, val state: TelltaleState)

/** SPEC R8-8. 가족에게 보내는 위치. 없으면 보고서의 location이 null이다("위치 확인 불가"). */
data class ReportLocation(val latitude: Double, val longitude: Double, val address: String?)

/** SPEC R8-3, R8-10. 가족에게 보내는 사고 의심 알림: 시각, 위치, 경고등 종류. */
data class AccidentReport(val atMillis: Long, val location: ReportLocation?, val telltales: List<TelltaleType>)

/** SPEC R8-1: 목록에 사고 의심 경고등이 하나라도 있는가. */
fun anyAccidentSuspected(readings: List<TelltaleReading>): Boolean = suspectedTelltales(readings).isNotEmpty()

/** SPEC R8-1, R8-10: 목록에서 사고 의심인 경고등 종류(보고서에 넣는다). */
fun suspectedTelltales(readings: List<TelltaleReading>): List<TelltaleType> =
    readings.filter { isAccidentSuspected(it.type, it.state) }.map { it.type }.distinct()

/** SPEC R8-4, R8-6: "사고 의심 아님 → 사고 의심"으로 바뀌고 확인 중이 아닐 때만 새 확인을 시작한다. */
fun shouldStartAccidentCheck(wasSuspected: Boolean, isSuspected: Boolean, checking: Boolean): Boolean =
    !wasSuspected && isSuspected && !checking
