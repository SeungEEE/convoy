package com.car.convoy.data

import com.car.convoy.data.navi.isArrivalForRequest
import com.car.convoy.data.navi.isRouteStartedForRequest
import com.car.convoy.data.server.FakeConvoyServer
import com.car.convoy.domain.Destination
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GearEvent
import com.car.convoy.domain.GroupEvent
import com.car.convoy.domain.GroupSession
import com.car.convoy.domain.GroupStatus
import com.car.convoy.domain.MemberEventResult
import com.car.convoy.domain.gearEvent
import com.car.convoy.domain.port.NaviPort

/** 멤버 이벤트 하나가 적용된 결과. 화면(작업 15)과 로그가 받는다. */
data class GroupUpdate(val memberId: String, val event: GroupEvent, val result: MemberEventResult)

/**
 * 작업 14: 내 차(리더)의 내비·기어 이벤트와 가상 멤버를 [GroupSession]에 잇는다.
 * 모든 메서드는 같은 스레드(앱에서는 메인 스레드)에서 부른다.
 */
class GroupController(
    private val myId: String,
    memberIds: List<String>,
    private val myNavi: NaviPort,
    scheduler: Scheduler,
    private val onUpdate: (GroupUpdate) -> Unit = {},
) {
    val server = FakeConvoyServer(myId, myNavi, scheduler, ::apply)
    val session = GroupSession(myId, memberIds, server)

    private var lastGear = Gear.UNKNOWN

    /** 화면이 R10 판단(canPerform)에 쓰는 현재 기어. 처음에는 UNKNOWN이라 주행 중과 같게 취급된다. */
    val currentGear: Gear
        get() = lastGear

    /** SPEC R11-19: 내 차의 경로 요청이 실패했는가. 화면이 알리고 [retryMyRoute]를 제공한다. */
    var myRouteFailed = false
        private set

    /** SPEC R11-19: 지금 [retryMyRoute]가 요청을 보내는가. 화면은 이 값일 때만 [다시 요청]을 보여 준다. */
    val canRetryMyRoute: Boolean
        get() = myRouteFailed && session.status == GroupStatus.ACTIVE && session.destination != null

    fun setDestination(destination: Destination) {
        val before = session.destination
        val failedBefore = myRouteFailed
        // 새 요청이 나가면 그 결과로 실패 표시를 다시 정한다. 실패는 요청 도중에 onMyRouteFailed로 바로 온다.
        myRouteFailed = false
        session.setDestination(myId, destination)
        // 요청이 나가지 않았으면(IGNORE_SAME, NOT_LEADER, FINISHED) 이전 실패 표시를 그대로 둔다.
        if (session.destination == before) myRouteFailed = failedBefore
    }

    /**
     * SPEC R11-19: 어댑터가 SDK 예외로 경로 요청 실패를 알린다. 그룹 상태는 바꾸지 않는다.
     * 예외는 requestRoute 호출 중에 동기로 나므로, 어댑터는 이 메서드를 큐에 넣지 않고 바로 부른다
     * (늦게 도착한 이전 요청의 실패가 새 요청의 실패로 보이지 않게).
     */
    fun onMyRouteFailed() {
        myRouteFailed = true
    }

    /**
     * SPEC R11-19: 실패 표시가 있고 그룹이 ACTIVE일 때만 현재 그룹 목적지를 내 내비에만 다시 요청한다.
     * shouldSyncDestination을 거치지 않고, 그룹 상태와 가상 멤버는 건드리지 않는다.
     */
    fun retryMyRoute() {
        if (!canRetryMyRoute) return
        val destination = session.destination ?: return
        myRouteFailed = false
        myNavi.requestRoute(destination)
    }

    /** SPEC R11-17: 마지막 요청과 이름이 같은 경로 시작만 받는다. */
    fun onRouteStarted(name: String?) {
        if (isRouteStartedForRequest(name, session.destination)) apply(myId, GroupEvent.ROUTE_STARTED)
    }

    /** SPEC R11-4: 마지막 요청과 이름·주소가 같은 도착만 받는다. */
    fun onDestinationArrived(name: String?, address: String?) {
        if (isArrivalForRequest(name, address, session.destination)) apply(myId, GroupEvent.NAVI_ARRIVED)
    }

    /** SPEC R7-1~3, R11-3: 기어 변화로 출발·도착을 만든다. GEAR_ARRIVED는 목적지가 없을 때만 나온다. */
    fun onGearChanged(gear: Gear) {
        val event = when (gearEvent(lastGear, gear, hasDestination = session.destination != null)) {
            GearEvent.DEPARTED -> GroupEvent.GEAR_DEPARTED
            GearEvent.ARRIVED -> GroupEvent.GEAR_ARRIVED
            GearEvent.NONE -> null
        }
        lastGear = gear
        if (event != null) apply(myId, event)
    }

    private fun apply(memberId: String, event: GroupEvent) {
        val result = session.onMemberEvent(memberId, event)
        onUpdate(GroupUpdate(memberId, event, result))
    }
}
