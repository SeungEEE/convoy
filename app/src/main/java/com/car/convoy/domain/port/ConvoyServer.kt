package com.car.convoy.domain.port

import com.car.convoy.domain.AccidentReport
import com.car.convoy.domain.Destination

/** 그룹 멤버 사이의 전달. FakeConvoyServer(작업 14)가 구현하고, 테스트에서는 가짜를 쓴다. */
interface ConvoyServer {
    /** SPEC R11-14: 멤버의 차에 목적지를 전달한다. 받은 차는 자기 NaviPort.requestRoute를 호출한다. */
    fun sendDestination(memberId: String, destination: Destination)

    /** SPEC R8-3: 가족에게 사고 의심 알림을 보낸다. 기본은 아무것도 하지 않는다(그룹 전용 가짜 구현용). */
    fun notifyFamily(report: AccidentReport) {}
}
