package com.car.convoy.domain

/** SPEC R8. 경고등 종류. SDK 타입과 분리한 도메인 표현이며 어댑터(data/)가 변환한다. */
enum class TelltaleType { AIRBAG, EMERGENCY_CALL, ABS, TPMS }

/** SPEC R8. 경고등 상태. */
enum class TelltaleState { ON, OFF, BLINK }

/** SPEC R8. 사고 확인 후 시스템이 할 일. */
enum class AccidentAction { WAITING, NOTIFY_FAMILY, SAFE, NO_ACTION }

private const val CONFIRM_TIMEOUT_SECONDS = 30

/** SPEC R8-1: AIRBAG 또는 EMERGENCY_CALL이 ON 또는 BLINK이면 사고 의심이다. */
fun isAccidentSuspected(telltaleType: TelltaleType, state: TelltaleState): Boolean {
    val isAccidentTelltale = telltaleType == TelltaleType.AIRBAG || telltaleType == TelltaleType.EMERGENCY_CALL
    val isActive = state == TelltaleState.ON || state == TelltaleState.BLINK
    return isAccidentTelltale && isActive
}

/** SPEC R8-2~5: 30초 안에 응답이 없으면 가족에게 알린다. 가족이 없으면 전송하지 않는다. */
fun accidentAction(elapsedSeconds: Int, responded: Boolean, hasFamily: Boolean): AccidentAction =
    when {
        responded -> AccidentAction.SAFE
        elapsedSeconds < CONFIRM_TIMEOUT_SECONDS -> AccidentAction.WAITING
        hasFamily -> AccidentAction.NOTIFY_FAMILY
        else -> AccidentAction.NO_ACTION
    }
