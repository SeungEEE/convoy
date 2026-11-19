package com.car.convoy.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

/** SPEC R14-7: 회전 안내 한 항목(NaviHelper TBTInfo). domain은 SDK를 모르므로 TurnType은 이름으로 받는다. */
data class TurnGuide(val turnTypeName: String, val distanceMeters: Double, val description: String?)

/** SPEC R14-15: onDrivingInfo의 목적지 항목(DrivingState)의 남은 시간(초)과 거리(m). */
data class DrivingRemain(val seconds: Int, val meters: Int)

/** SPEC R14-15: 회전 안내 카드의 두 줄. 둘째 줄(방면)이 없으면 null. */
data class TurnCardText(val title: String, val subtitle: String?)

private const val STALE_MINUTE_SECONDS = 60

/**
 * SPEC R14-13: 멤버 앞뒤 문구. 거리는 formatDistance 표기다. ME_UNKNOWN이면 null(화면은 "내 위치 확인 중"만).
 * [gapMeters]는 relation에 쓴 진행 거리 차이의 절댓값이고, 호출하는 쪽이 보장한다.
 */
fun memberMapLabel(name: String, relation: MemberRelation, gapMeters: Double, ageSeconds: Int): String? = when (relation) {
    MemberRelation.ME_UNKNOWN -> null
    MemberRelation.AHEAD -> "$name · 앞 약 ${formatDistance(gapMeters)}"
    MemberRelation.BEHIND -> "$name · 뒤 약 ${formatDistance(gapMeters)}"
    MemberRelation.NEAR -> "$name · 나란히"
    MemberRelation.OFF_ROUTE -> "$name · 다른 길로 이동 중"
    MemberRelation.STALE -> "$name · 위치 확인 중 · ${ageText(ageSeconds)}"
}

/** 60초 미만이면 "40초 전", 이상이면 분으로 내림해 "2분 전". */
private fun ageText(seconds: Int): String =
    if (seconds < STALE_MINUTE_SECONDS) "${seconds}초 전" else "${seconds / STALE_MINUTE_SECONDS}분 전"

/**
 * SPEC R14-15: 첫 줄은 "거리 · 문구"(문구가 없으면 거리만), 둘째 줄은 방면의 앞뒤 공백을 뺀 값(비면 없음).
 */
fun turnCardText(turnTypeName: String, distanceMeters: Double, description: String?): TurnCardText {
    val distance = formatDistance(distanceMeters)
    val title = turnLabel(turnTypeName)?.let { "$distance · $it" } ?: distance
    return TurnCardText(title, description?.trim()?.takeIf { it.isNotEmpty() })
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

/**
 * SPEC R14-15: "10:24 도착 · 16분 · 18 km". 도착 시각은 지금 + 남은 시간이고 초는 버린다(날짜는 쓰지 않음).
 * 남은 시간은 분으로 반올림(0.5분은 올림)하고, 0분이면 "1분 미만", 60분 이상이면 "1시간 20분"(나머지 0분이면 "1시간").
 * 0보다 작은 값은 0으로 본다.
 */
fun arrivalCardText(nowMillis: Long, zone: ZoneId, remainSeconds: Int, remainMeters: Int): String {
    val seconds = max(remainSeconds, 0)
    // HH:mm으로 쓰면 초는 버려진다.
    val arrival = CLOCK.format(Instant.ofEpochMilli(nowMillis).plusSeconds(seconds.toLong()).atZone(zone))
    val minutes = (seconds + 30) / 60
    val duration = when {
        minutes == 0 -> "1분 미만"
        minutes < 60 -> "${minutes}분"
        minutes % 60 == 0 -> "${minutes / 60}시간"
        else -> "${minutes / 60}시간 ${minutes % 60}분"
    }
    return "$arrival 도착 · $duration · ${formatDistance(max(remainMeters, 0).toDouble())}"
}
