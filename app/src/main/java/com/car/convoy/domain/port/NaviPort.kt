package com.car.convoy.domain.port

import com.car.convoy.domain.Destination

/** 이 차의 내비. NaviHelper 어댑터(작업 14)가 구현하고, 테스트에서는 가짜를 쓴다. */
interface NaviPort {
    /** SPEC R5-1, R11-14: 이 차에서 목적지로 경로를 요청한다. */
    fun requestRoute(destination: Destination)

    /** SPEC R3-3, R12-4: [끊기] 때 따라가던 경로를 취소한다. 기본은 아무것도 하지 않는다(그룹 주행 테스트의 가짜용). */
    fun cancelRoute() {}
}
