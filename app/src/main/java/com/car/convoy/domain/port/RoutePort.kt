package com.car.convoy.domain.port

import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.RouteResult

/** SPEC R14-1: 길찾기. 카카오모빌리티 어댑터가 구현하고, 테스트에서는 가짜를 쓴다. */
interface RoutePort {
    /** [from]에서 [via]를 차례로 거쳐 [to]까지. 결과는 받는 쪽 스레드에서 한 번 온다. 실패면 null(R14-2). */
    fun route(from: GeoPoint, to: GeoPoint, via: List<GeoPoint>, onResult: (RouteResult?) -> Unit)
}
