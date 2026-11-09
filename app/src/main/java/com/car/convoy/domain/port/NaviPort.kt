package com.car.convoy.domain.port

import com.car.convoy.domain.Destination

/** 이 차의 내비. NaviHelper 어댑터(작업 14)가 구현하고, 테스트에서는 가짜를 쓴다. */
interface NaviPort {
    /** SPEC R5-1, R11-14: 이 차에서 목적지로 경로를 요청한다. */
    fun requestRoute(destination: Destination)
}
