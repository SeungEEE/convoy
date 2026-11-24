package com.car.convoy.data.server

import com.car.convoy.data.Cancellable
import com.car.convoy.data.Scheduler
import com.car.convoy.domain.AccidentReport
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.port.ConvoyServer
import com.car.convoy.domain.port.NaviPort

private const val VIRTUAL_ROUTE_START_DELAY_MILLIS = 6_000L

/** SPEC R13-12: 가상 멤버는 경유지를 받으면 4초 뒤 반영된다. (제안) */
private const val VIRTUAL_WAYPOINT_APPLY_DELAY_MILLIS = 4_000L

/**
 * SPEC R11-14, R11-18: 인메모리 목업 서버.
 * 내 차([myId])에 온 목적지는 내 내비로 경로를 요청한다. 가상 멤버는 목적지를 받으면 6초 뒤
 * ROUTE_STARTED를 만들고, 출발·도착은 시연자가 [depart], [arrive]로 만든다(R11-8의 데모용 대체 이벤트).
 */
class FakeConvoyServer(
    private val myId: String,
    private val myNavi: NaviPort,
    private val scheduler: Scheduler,
    /** SPEC R13-12: 가상 멤버의 경유지 반영 결과(true면 반영, false면 실패). 마지막 람다 자리는 [onVirtualEvent]다. */
    private val onVirtualWaypoint: (memberId: String, applied: Boolean) -> Unit = { _, _ -> },
    private val onVirtualEvent: (memberId: String, event: GroupEvent) -> Unit,
) : ConvoyServer {

    private val pendingRouteStarts = mutableMapOf<String, Cancellable>()
    private val pendingWaypoints = mutableMapOf<String, Cancellable>()

    /** 시연용(R13-12): 다음 경유지 전달을 실패로 만들 가상 멤버. */
    private val failNextWaypoint = mutableSetOf<String>()

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

    /** SPEC R13-5·12: 내 차는 내 내비에 경유지를 넣고, 가상 멤버는 4초 뒤 반영(또는 시연용 실패)한다. */
    override fun sendWaypoint(memberId: String, waypoint: Destination) {
        if (memberId == myId) {
            myNavi.addWaypoint(waypoint)
            return
        }
        val applied = !failNextWaypoint.remove(memberId)
        pendingWaypoints.remove(memberId)?.cancel()
        pendingWaypoints[memberId] = scheduler.schedule(VIRTUAL_WAYPOINT_APPLY_DELAY_MILLIS) {
            pendingWaypoints.remove(memberId)
            onVirtualWaypoint(memberId, applied)
        }
    }

    /** 시연용(R13-12): 이 가상 멤버에게 가는 다음 경유지를 실패로 만든다. */
    fun failNextWaypoint(memberId: String) {
        require(memberId != myId) { "내 차의 반영은 실제 SDK에서 온다: $memberId" }
        failNextWaypoint += memberId
    }

    /** SPEC R11-22, R13-14: 버린 세션의 가상 멤버 예약(경로 시작, 경유지 반영)을 모두 취소한다. */
    fun cancelPending() {
        pendingRouteStarts.values.forEach { it.cancel() }
        pendingRouteStarts.clear()
        pendingWaypoints.values.forEach { it.cancel() }
        pendingWaypoints.clear()
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
