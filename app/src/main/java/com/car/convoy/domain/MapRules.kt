package com.car.convoy.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** SPEC R14-5: 내 경로 위에서 멤버와의 관계. */
enum class MemberRelation { ME_UNKNOWN, STALE, OFF_ROUTE, NEAR, AHEAD, BEHIND }

/** SPEC R14-5: 경로에서 이만큼 넘게 떨어지면 경로 밖이다. (제안) */
private const val OFF_ROUTE_METERS = 100.0

/** SPEC R14-5: 진행 거리 차이가 이 값 이하면 나란히다. (제안) */
private const val NEAR_METERS = 30.0

/** SPEC R14-5: 위치가 이 시간(초)을 넘게 갱신되지 않으면 오래됐다. (제안) */
private const val STALE_SECONDS = 30

/**
 * SPEC R14-5. 위에서부터 차례로 따진다: ME_UNKNOWN → STALE → OFF_ROUTE → NEAR → AHEAD/BEHIND.
 * 진행 거리와 떨어진 거리는 snapToRoute(R14-4)의 결과다.
 */
fun relation(
    myProgress: Double,
    myOffRouteMeters: Double,
    myAgeSeconds: Int,
    memberProgress: Double,
    memberOffRouteMeters: Double,
    memberAgeSeconds: Int,
): MemberRelation = when {
    myAgeSeconds > STALE_SECONDS || myOffRouteMeters > OFF_ROUTE_METERS -> MemberRelation.ME_UNKNOWN
    memberAgeSeconds > STALE_SECONDS -> MemberRelation.STALE
    memberOffRouteMeters > OFF_ROUTE_METERS -> MemberRelation.OFF_ROUTE
    kotlin.math.abs(memberProgress - myProgress) <= NEAR_METERS -> MemberRelation.NEAR
    memberProgress > myProgress -> MemberRelation.AHEAD
    else -> MemberRelation.BEHIND
}

/** SPEC R14-10: 우회 시간 = 경유 − 곧장(음수면 0). 길찾기가 실패한 쪽이 있으면 null(계산 못 함). */
fun detourSeconds(directSeconds: Int?, viaSeconds: Int?): Int? {
    if (directSeconds == null || viaSeconds == null) return null
    return (viaSeconds - directSeconds).coerceAtLeast(0)
}

/** SPEC R14-8: 마커 다시 그리기 간격(초). 정차 중 2초, 그 밖(주행 중으로 취급)은 10초. (제안) */
fun redrawIntervalSeconds(gear: Gear): Int = if (gear == Gear.PARK) 2 else 10

/**
 * SPEC R14-7: 회전 안내 문구. NaviHelper의 TurnType 이름을 받는다(domain은 SDK를 모른다).
 * TURN_UNKNOWN과 표에 없는 이름은 null이다(거리와 방면만 보여 준다).
 */
fun turnLabel(turnTypeName: String): String? {
    EXACT_TURN_LABELS[turnTypeName]?.let { return it }
    return PREFIX_TURN_LABELS.firstOrNull { (prefix, _) -> turnTypeName.startsWith(prefix) }?.second
}

private val EXACT_TURN_LABELS = mapOf(
    "TURN_STRAIGHT" to "직진",
    "TURN_RIGHT" to "우회전",
    "TURN_LEFT" to "좌회전",
    "TURN_KEEP_R" to "오른쪽 길",
    "TURN_KEEP_L" to "왼쪽 길",
    "TURN_SHARP_R" to "급우회전",
    "TURN_SHARP_L" to "급좌회전",
    "TURN_UTURN" to "유턴",
    "TURN_EXIT_IC_R" to "오른쪽 출구",
    "TURN_EXIT_IC_L" to "왼쪽 출구",
    "TURN_EXIT_IC_C" to "출구",
    "TURN_EXIT_ROUNDABOUT" to "회전교차로 나가기",
    "TURN_TOLL" to "요금소",
    "TURN_ENTER_SERVICEAREA" to "휴게소",
    "TURN_RESTAREA" to "휴게소",
    "TURN_TUNNEL" to "터널",
    "TURN_WAY" to "경유지",
    "TURN_ARRIVE" to "도착",
    "TURN_DEVIDE_MERGE" to "갈림길",
)

/** `*` 묶음(끝이 _R, _L, _C 또는 회전교차로 번호). 정확한 이름이 먼저다. */
private val PREFIX_TURN_LABELS = listOf(
    "TURN_ENTER_IC_" to "고속도로 진입",
    "TURN_ENTER_UNDER_" to "지하차도 진입",
    "TURN_AVOID_UNDER_" to "지하차도 옆길",
    "TURN_ENTER_OVER_" to "고가차도 진입",
    "TURN_AVOID_OVER_" to "고가차도 옆길",
    "TURN_ENTER_ROUNDABOUT_" to "회전교차로",
)

/**
 * SPEC R14-7: 먼저 10m 단위로 반올림한다. 1,000m 미만이면 "700 m",
 * 이상이면 km로 소수 첫째 자리까지 반올림하고 ".0"은 뺀다(995m → "1 km"). (제안)
 */
fun formatDistance(meters: Double): String {
    val rounded = Math.round(meters / 10.0) * 10
    if (rounded < 1000) return "$rounded m"
    val km = BigDecimal(rounded).divide(BigDecimal(1000)).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros()
    return "${km.toPlainString()} km"
}
