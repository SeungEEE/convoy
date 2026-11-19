package com.car.convoy.domain

import kotlin.math.cos

/** SPEC R14-4: 떨어진 거리 차이가 이 값 이하면 같은 거리로 본다(부동소수점 오차, Codex 리뷰). */
private const val TIE_METERS = 0.01

/** 위도·경도 한 점(경로 좌표, 멤버 위치). SDK와 무관하다. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** SPEC R14-4: 경로 위 투영 결과. 진행 거리는 경로 시작부터 경로를 따라 잰 거리, 떨어진 거리는 투영점까지의 거리다. */
data class RouteSnap(val progressMeters: Double, val offRouteMeters: Double, val point: GeoPoint)

/** SPEC R14-1: 길찾기 결과. 경로 좌표, 시간(초), 거리(m). */
data class RouteResult(val points: List<GeoPoint>, val durationSeconds: Int, val distanceMeters: Int)

/** 두 점의 하버사인 거리(m). R11-13과 같은 식을 쓴다. */
fun distanceMeters(a: GeoPoint, b: GeoPoint): Double =
    distanceMeters(Destination("", "", a.latitude, a.longitude), Destination("", "", b.latitude, b.longitude))

/** 경로 전체 길이(m): 점 사이 하버사인 거리의 합. */
fun routeLengthMeters(route: List<GeoPoint>): Double =
    route.zipWithNext().sumOf { (a, b) -> distanceMeters(a, b) }

/**
 * SPEC R14-4. 각 선분에서 가장 가까운 점을 구해 선분 범위 안으로 클램프한다.
 * 선분 투영은 그 선분 근처의 평면 근사(경도 거리 × cos 위도)로 하고, 거리는 하버사인으로 잰다.
 * 가장 가까운 투영점이 둘 이상이면(0.01m 이하 차이) 진행 거리가 작은 쪽(앞 선분)을 고른다.
 * 점이 하나면 진행 0과 그 점까지의 거리, 빈 경로면 null이다.
 */
fun snapToRoute(point: GeoPoint, route: List<GeoPoint>): RouteSnap? {
    if (route.isEmpty()) return null
    if (route.size == 1) return RouteSnap(0.0, distanceMeters(point, route[0]), route[0])

    var best: RouteSnap? = null
    var walked = 0.0
    for ((a, b) in route.zipWithNext()) {
        val segment = distanceMeters(a, b)
        val t = projectionRatio(point, a, b)
        val projected = GeoPoint(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t)
        val off = distanceMeters(point, projected)
        // 같은 거리(0.01m 이하 차이)면 먼저 찾은(진행 거리가 작은) 쪽을 둔다.
        if (best == null || off < best.offRouteMeters - TIE_METERS) {
            best = RouteSnap(walked + distanceMeters(a, projected), off, projected)
        }
        walked += segment
    }
    return best
}

/** 선분 a→b 위 투영 비율(0~1로 클램프). 위도 기준 평면 근사: 경도 차이에 cos(위도)를 곱한다. */
private fun projectionRatio(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
    val scale = cos(Math.toRadians((a.latitude + b.latitude) / 2))
    val bx = (b.longitude - a.longitude) * scale
    val by = b.latitude - a.latitude
    val px = (p.longitude - a.longitude) * scale
    val py = p.latitude - a.latitude
    val lengthSq = bx * bx + by * by
    if (lengthSq == 0.0) return 0.0
    return ((px * bx + py * by) / lengthSq).coerceIn(0.0, 1.0)
}

/**
 * SPEC R14-14: 초록·주황으로 칠할 구간. 두 진행 거리를 각각 0과 경로 길이 사이로 클램프한 뒤 작은 쪽부터 정렬한다.
 * 결과는 작은 쪽 지점, 진행 거리가 두 값 사이(양 끝 제외)인 꼭짓점들, 큰 쪽 지점 순이다.
 * 클램프한 두 값이 같으면 그 지점 하나, 점이 하나뿐인 경로는 그 점 하나, 빈 경로는 빈 목록이다.
 */
fun routeSection(route: List<GeoPoint>, fromProgress: Double, toProgress: Double): List<GeoPoint> {
    if (route.isEmpty()) return emptyList()
    if (route.size == 1) return listOf(route[0])
    val length = routeLengthMeters(route)
    val a = fromProgress.coerceIn(0.0, length)
    val b = toProgress.coerceIn(0.0, length)
    val start = minOf(a, b)
    val end = maxOf(a, b)
    val first = pointAtProgress(route, start)!!
    if (start == end) return listOf(first)

    val result = mutableListOf(first)
    var walked = 0.0
    for (i in 1 until route.size) {
        walked += distanceMeters(route[i - 1], route[i])
        if (walked > start && walked < end) result += route[i]
    }
    result += pointAtProgress(route, end)!!
    return result
}

/**
 * SPEC R14-3: 경로를 따라 [progressMeters]만큼 간 지점(시연용 가상 멤버 위치). 선분 안에서는 위도·경도를 비율로 나눈다.
 * 0보다 작으면 시작점, 경로 길이보다 크면 끝점. 점이 하나면 그 점, 빈 경로면 null이다.
 */
fun pointAtProgress(route: List<GeoPoint>, progressMeters: Double): GeoPoint? {
    if (route.isEmpty()) return null
    if (progressMeters <= 0.0 || route.size == 1) return route.first()
    var walked = 0.0
    for ((a, b) in route.zipWithNext()) {
        val segment = distanceMeters(a, b)
        if (segment > 0 && walked + segment >= progressMeters) {
            val t = (progressMeters - walked) / segment
            return GeoPoint(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t)
        }
        walked += segment
    }
    return route.last()
}
