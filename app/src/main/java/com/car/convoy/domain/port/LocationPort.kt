package com.car.convoy.domain.port

/** SPEC R8-8: 내 차 위치 요청. 결과는 비동기로 온다(NaviHelper의 onCurrentLocationInfo, 폴링). */
interface LocationPort {
    fun requestLocation()
}
