package com.car.convoy.data.route

import com.car.convoy.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R14-1·2, 작업 35(AI 작성): 카카오모빌리티 길찾기 요청 주소와 응답 해석. 응답 형식은 docs/sdk-findings.md 실측. */
class KakaoRouteTest {

    @Test fun url_without_waypoints() {
        val url = kakaoDirectionsUrl(GeoPoint(37.4127, 127.0947), GeoPoint(37.3948, 127.1112), emptyList())
        assertEquals("https://apis-navi.kakaomobility.com/v1/directions?origin=127.0947,37.4127&destination=127.1112,37.3948&priority=RECOMMEND", url)
    }

    @Test fun url_with_waypoints_joined_by_bar() {
        val url = kakaoDirectionsUrl(GeoPoint(37.425, 127.089), GeoPoint(37.3948, 127.1112), listOf(GeoPoint(37.4127, 127.0947), GeoPoint(37.40, 127.10)))
        assertTrue(url, url.endsWith("&waypoints=127.0947,37.4127%7C127.1,37.4"))
    }

    @Test fun parses_points_duration_distance_across_sections() {
        val json = """
            {"trans_id":"x","routes":[{"result_code":0,"result_msg":"길찾기 성공",
              "summary":{"distance":3350,"duration":704},
              "sections":[
                {"roads":[{"vertexes":[127.0947,37.4127,127.0950,37.4120]},{"vertexes":[127.0950,37.4120,127.0960,37.4100]}]},
                {"roads":[{"vertexes":[127.0960,37.4100,127.1112,37.3948]}]}
              ]}]}
        """.trimIndent()
        val r = parseKakaoDirections(json)!!
        assertEquals(704, r.durationSeconds)
        assertEquals(3350, r.distanceMeters)
        // 경도·위도가 번갈아 오고, 구간·도로 경계에서 겹치는 점은 한 번만 둔다.
        assertEquals(listOf(GeoPoint(37.4127, 127.0947), GeoPoint(37.4120, 127.0950), GeoPoint(37.4100, 127.0960), GeoPoint(37.3948, 127.1112)), r.points)
    }

    @Test fun failure_code_is_null() {
        assertNull(parseKakaoDirections("""{"trans_id":"x","routes":[{"result_code":104,"result_msg":"출발지와 도착지가 5 m 이내"}]}"""))
    }

    @Test fun odd_vertexes_is_null() { // Codex 리뷰: 손상된 응답을 부분 성공으로 보지 않는다
        assertNull(parseKakaoDirections("""{"routes":[{"result_code":0,"summary":{"distance":1,"duration":1},"sections":[{"roads":[{"vertexes":[127.0,37.0,127.1]}]}]}]}"""))
    }

    @Test fun no_points_is_null() {
        assertNull(parseKakaoDirections("""{"routes":[{"result_code":0,"summary":{"distance":1,"duration":1},"sections":[{"roads":[{"vertexes":[]}]}]}]}"""))
    }

    @Test fun malformed_is_null() {
        assertNull(parseKakaoDirections("not json"))
        assertNull(parseKakaoDirections("""{"routes":[]}"""))
    }
}
