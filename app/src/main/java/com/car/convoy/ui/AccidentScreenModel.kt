package com.car.convoy.ui

import com.car.convoy.domain.AccidentReport
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.TelltaleType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** SPEC R8-10: 가족에게 보낸 알림 카드. */
data class SentCard(val timeLabel: String, val locationLabel: String, val telltaleLabel: String)

/** SPEC R8, 작업 21: 사고 확인 화면과 가족 알림 카드에 보일 값. */
data class AccidentScreenModel(
    val showPrompt: Boolean,
    val remainingLabel: String,
    val promptDetail: String,
    /** 이전 머리줄 문구(작업 21). 작업 24부터 화면은 HomeScreenModel.familyHeader를 쓰고 이 값은 쓰지 않는다. */
    val familyLabel: String,
    val sentCard: SentCard?,
)

private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun telltaleName(type: TelltaleType): String = when (type) {
    TelltaleType.AIRBAG -> "에어백"
    TelltaleType.EMERGENCY_CALL -> "긴급호출"
    TelltaleType.ABS -> "ABS"
    TelltaleType.TPMS -> "타이어 공기압"
}

/**
 * SPEC R8-2~5, R8-9, R8-10: 확인 중이면 "괜찮으세요?"와 남은 초를 보여 준다. 가족이 없으면 알리지 않는다고 안내한다.
 * 카드는 실제로 보낸 보고서가 있고 확인 중이 아닐 때만 보여 준다.
 */
fun accidentScreenModel(
    checking: Boolean,
    remainingSeconds: Int,
    family: FamilyState,
    lastSentReport: AccidentReport?,
    zone: ZoneId,
): AccidentScreenModel {
    val hasFamily = family == FamilyState.ACTIVE
    return AccidentScreenModel(
        showPrompt = checking,
        remainingLabel = remainingSeconds.toString(),
        promptDetail = if (hasFamily) "응답이 없으면 ${remainingSeconds}초 뒤 가족에게 알려요" else "가족이 등록되어 있지 않아 알리지 않아요",
        familyLabel = if (hasFamily) "가족 1명 (시연 설정)" else "가족 공유 꺼짐",
        sentCard = lastSentReport?.takeIf { !checking }?.let { r ->
            SentCard(
                timeLabel = TIME.format(Instant.ofEpochMilli(r.atMillis).atZone(zone)),
                locationLabel = r.location?.let { l ->
                    "${l.address ?: "주소 없음"} (${"%.4f".format(Locale.US, l.latitude)}, ${"%.4f".format(Locale.US, l.longitude)})"
                } ?: "위치 확인 불가",
                telltaleLabel = r.telltales.joinToString(", ") { telltaleName(it) },
            )
        },
    )
}
