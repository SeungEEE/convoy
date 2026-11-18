package com.car.convoy.data.route

import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.RouteResult
import com.car.convoy.domain.port.RoutePort
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executor

private const val DIRECTIONS = "https://apis-navi.kakaomobility.com/v1/directions"

/** 좌표를 "경도,위도"로 쓴다. 불필요한 0은 붙이지 않는다(127.1000 → 127.1). */
private fun GeoPoint.lngLat(): String = "${longitude.toBigDecimal().stripTrailingZeros().toPlainString()},${latitude.toBigDecimal().stripTrailingZeros().toPlainString()}"

/** SPEC R14-1: 카카오모빌리티 자동차 길찾기 주소. 경유지는 `|`(%7C)로 잇는다(docs/sdk-findings.md 실측). */
fun kakaoDirectionsUrl(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>): String {
    val base = "$DIRECTIONS?origin=${from.lngLat()}&destination=${to.lngLat()}&priority=RECOMMEND"
    return if (via.isEmpty()) base else base + "&waypoints=" + via.joinToString("%7C") { it.lngLat() }
}

/**
 * SPEC R14-1·2: 응답을 해석한다. `routes[0].result_code`가 0이 아니거나 형식이 다르면 null이다.
 * 좌표는 `sections[].roads[].vertexes`에 경도·위도가 번갈아 오고, 경계에서 겹치는 점은 한 번만 둔다.
 * 좌표 개수가 홀수이거나 좌표가 하나도 없으면 손상된 응답으로 보고 null이다(Codex 리뷰).
 */
fun parseKakaoDirections(body: String): RouteResult? = runCatching {
    val route = JSONObject(body).getJSONArray("routes").getJSONObject(0)
    if (route.getInt("result_code") != 0) return null
    val summary = route.getJSONObject("summary")
    val points = mutableListOf<GeoPoint>()
    val sections = route.getJSONArray("sections")
    for (i in 0 until sections.length()) {
        val roads = sections.getJSONObject(i).getJSONArray("roads")
        for (j in 0 until roads.length()) {
            val v = roads.getJSONObject(j).getJSONArray("vertexes")
            if (v.length() % 2 != 0) return null
            for (k in 0 until v.length() step 2) {
                val p = GeoPoint(v.getDouble(k + 1), v.getDouble(k))
                if (points.lastOrNull() != p) points += p
            }
        }
    }
    if (points.isEmpty()) return null
    RouteResult(points, summary.getInt("duration"), summary.getInt("distance"))
}.getOrNull()

/**
 * SPEC R14-1: [RoutePort]의 카카오모빌리티 구현. 네트워크는 [io]에서, 결과는 [deliver]로 넘긴다(앱은 메인 스레드).
 * REST 키를 앱에 넣는 것은 시연용이다(R14-1). 실서비스는 서버가 대신 부른다.
 */
class KakaoRouteAdapter(
    private val restKey: String,
    private val io: Executor,
    private val deliver: (Runnable) -> Unit,
    private val log: (String) -> Unit = {},
) : RoutePort {
    override fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit) {
        io.execute {
            val result = runCatching {
                val conn = URL(kakaoDirectionsUrl(from, to, via)).openConnection() as HttpURLConnection
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                conn.setRequestProperty("Authorization", "KakaoAK $restKey")
                try {
                    val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                    stream?.bufferedReader()?.use { it.readText() }
                } finally {
                    conn.disconnect()
                }
            }.onFailure { log("길찾기 실패: $it") }.getOrNull()
            val parsed = result?.let(::parseKakaoDirections)
            log(if (parsed == null) "길찾기 결과 없음" else "길찾기 ${parsed.points.size}점 ${parsed.distanceMeters}m ${parsed.durationSeconds}s")
            deliver(Runnable { onResult(parsed) })
        }
    }
}
