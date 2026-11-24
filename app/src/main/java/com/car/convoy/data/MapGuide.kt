package com.car.convoy.data

import com.car.convoy.domain.DrivingRemain
import com.car.convoy.domain.TurnGuide

/**
 * SPEC R14-7·15: 내비에서 받은 회전 안내 첫 항목과 목적지까지 남은 시간·거리를 들고 있는다.
 * 지우는 시점(목적지·경유지 변경, 경로 요청 실패, 경로 취소, 주행 끝내기)은 부르는 쪽이 정하고 [clear]를 부른다.
 * 모든 메서드는 같은 스레드(메인)에서 부른다.
 */
class MapGuide {
    var turn: TurnGuide? = null
        private set

    var remain: DrivingRemain? = null
        private set

    /** SPEC R14-7: onTBTInfo. 첫 항목만 쓰고, 목록이 비면 카드를 숨긴다. */
    fun onTurns(turns: List<TurnGuide>) {
        turn = turns.firstOrNull()
    }

    /** SPEC R14-15: onDrivingInfo의 목적지 항목. 목적지 항목이 없으면(null) 이전 값을 그대로 둔다. */
    fun onDestinationRemain(value: DrivingRemain?) {
        if (value != null) remain = value
    }

    fun clear() {
        turn = null
        remain = null
    }
}
