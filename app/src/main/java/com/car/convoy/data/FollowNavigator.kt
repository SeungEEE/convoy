package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.distanceMeters
import com.car.convoy.domain.moveNorth
import com.car.convoy.domain.port.NaviPort
import com.car.convoy.domain.shouldReroute

/** SPEC R12-3: 가상 앞차의 시작 위치. 에뮬레이터 내 차 위치에서 북쪽으로 약 1km다. */
val FOLLOW_START = Destination("", "", 37.4217, 127.0947)

/**
 * 작업 25(SPEC R6-1~2, R12-2~4): 앞차 위치로 내 내비에 경로를 요청한다. 앞차가 마지막 요청 지점에서 200m 이상
 * 움직였을 때만(shouldReroute) 다시 요청한다. 서버가 없어 앞차 위치는 시연용 [moveLeader]로 옮긴다(R12-3).
 * 같은 스레드에서만 부른다. 경로 요청 실패는 어댑터가 [onRouteFailed]로 호출 중에 바로 알린다(R11-19와 같다).
 */
class FollowNavigator(private val navi: NaviPort, private val scheduler: Scheduler) {

    var active = false
        private set

    /** 앞차의 지금 위치. */
    var leader: Destination = FOLLOW_START
        private set

    private var lastRequested: Destination = FOLLOW_START

    /** R12-4: 처음 요청을 뺀 다시 요청 횟수. */
    var rerouteCount = 0
        private set

    var leaderUpdatedAtMillis = 0L
        private set

    var routeFailed = false
        private set

    /** R12-4: 마지막 요청 뒤 앞차가 움직인 거리(m). */
    val movedSinceLastRequest: Double get() = distanceMeters(lastRequested, leader)

    /** R12-2: `ACTIVE`가 되면 앞차 위치를 "<이름> 차"로 요청한다. */
    fun start(leaderName: String) {
        active = true
        rerouteCount = 0
        leader = FOLLOW_START.copy(name = "$leaderName 차")
        leaderUpdatedAtMillis = scheduler.nowMillis()
        request()
    }

    /** R12-3: 시연용 [100m 이동] [300m 이동]. 앞차의 지금 위치에서 북쪽으로 더 옮긴다(누적). */
    fun moveLeader(meters: Double) {
        if (!active) return
        leader = moveNorth(leader, meters)
        leaderUpdatedAtMillis = scheduler.nowMillis()
        if (shouldReroute(movedSinceLastRequest)) {
            rerouteCount++
            request()
        }
    }

    fun onRouteFailed() {
        routeFailed = true
    }

    /** R12-4: 실패했을 때만 앞차의 지금 위치로 다시 요청한다(R11-19와 같다). */
    fun retry() {
        if (!active || !routeFailed) return
        request()
    }

    /** R3-3, R12-4 [끊기]: 따라가던 경로를 취소한다. */
    fun stop() {
        if (!active) return
        active = false
        navi.cancelRoute()
    }

    private fun request() {
        routeFailed = false
        lastRequested = leader
        navi.requestRoute(leader)
    }
}
