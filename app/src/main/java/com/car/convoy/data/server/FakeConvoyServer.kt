package com.car.convoy.data.server

import com.car.convoy.data.Cancellable
import com.car.convoy.data.Scheduler
import com.car.convoy.domain.AccidentReport
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.port.ConvoyServer
import com.car.convoy.domain.port.NaviPort

private const val VIRTUAL_ROUTE_START_DELAY_MILLIS = 6_000L

/**
 * SPEC R11-14, R11-18: 인메모리 목업 서버.
 * 내 차([myId])에 온 목적지는 내 내비로 경로를 요청한다. 가상 멤버는 목적지를 받으면 6초 뒤
 * ROUTE_STARTED를 만들고, 출발·도착은 시연자가 [depart], [arrive]로 만든다(R11-8의 데모용 대체 이벤트).
 */
class FakeConvoyServer(
    private val myId: String,
    private val myNavi: NaviPort,
    private val scheduler: Scheduler,
    private val onVirtualEvent: (memberId: String, event: GroupEvent) -> Unit,
) : ConvoyServer {

    private val pendingRouteStarts = mutableMapOf<String, Cancellable>()

    /** SPEC R8-10: 가족에게 보낸 알림. 받는 차가 없어 여기에 모아 화면에 보여 준다. */
    val familyAlerts = mutableListOf<AccidentReport>()

    override fun notifyFamily(report: AccidentReport) {
        familyAlerts += report
    }

    override fun sendDestination(memberId: String, destination: Destination) {
        if (memberId == myId) {
            myNavi.requestRoute(destination)
            return
        }
        // 재탐색 중에 새 목적지가 오면 이전 예약을 취소하고 다시 6초를 센다(R11-18).
        pendingRouteStarts.remove(memberId)?.cancel()
        pendingRouteStarts[memberId] = scheduler.schedule(VIRTUAL_ROUTE_START_DELAY_MILLIS) {
            pendingRouteStarts.remove(memberId)
            onVirtualEvent(memberId, GroupEvent.ROUTE_STARTED)
        }
    }

    /** 시연용: 가상 멤버가 출발한다. */
    fun depart(memberId: String) = virtual(memberId, GroupEvent.GEAR_DEPARTED)

    /** 시연용: 가상 멤버가 도착한다. */
    fun arrive(memberId: String) = virtual(memberId, GroupEvent.NAVI_ARRIVED)

    private fun virtual(memberId: String, event: GroupEvent) {
        require(memberId != myId) { "내 차의 이벤트는 실제 SDK에서 온다: $memberId" }
        onVirtualEvent(memberId, event)
    }
}
