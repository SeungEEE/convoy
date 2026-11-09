package com.car.convoy.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** SPEC R11-13. SDK와 무관한 목적지. SDK의 RouteInfo로 바꾸는 일은 어댑터(작업 14)가 한다. */
data class Destination(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** SPEC R11-13: 두 좌표 사이의 하버사인 거리(m). */
fun distanceMeters(a: Destination, b: Destination): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    // 부동소수점 오차로 h가 [0, 1]을 조금 벗어나면 asin이 NaN을 내므로 범위를 묶는다.
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
}
