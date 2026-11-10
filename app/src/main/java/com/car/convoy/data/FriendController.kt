package com.car.convoy.data

import com.car.convoy.domain.FollowEvent
import com.car.convoy.domain.FollowLink
import com.car.convoy.domain.FollowState
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import com.car.convoy.domain.canStartFollowRequest
import com.car.convoy.domain.nextFollowState

/**
 * 작업 22(SPEC R2-5, R3): 가상 친구 목록, 내 상태, 진행 중인 따라가기 하나를 든다.
 * 외부 서버가 없으므로 친구의 응답은 시연용 패널이 [respond]로 만든다. 같은 스레드에서만 부른다.
 */
class FriendController(val friends: List<Friend>, myStatus: UserStatus = UserStatus.ACTIVE) {

    /** R2-1: 내 상태. R10 "내 상태 변경"이라 주행 중에도 바꿀 수 있다. */
    var myStatus: UserStatus = myStatus
        private set

    /** 진행 중인 따라가기(REQUESTED 또는 ACTIVE). ENDED가 되면 null로 비운다. */
    var follow: FollowLink? = null
        private set

    fun setMyStatus(status: UserStatus) {
        myStatus = status
    }

    /** R2-5: 조건(canStartFollowRequest)을 만족할 때만 REQUESTED로 시작한다. 보냈으면 true. */
    fun requestFollow(friendId: String, gear: Gear): Boolean {
        val friend = friends.firstOrNull { it.id == friendId } ?: return false
        if (!canStartFollowRequest(friend.status, gear, followInProgress = follow != null)) return false
        follow = FollowLink(friendId, FollowState.REQUESTED)
        return true
    }

    /** R2-5 시연용: 요청받은 가상 친구의 [수락]·[거절]. ACCEPT와 DECLINE만 받는다. */
    fun respond(event: FollowEvent) {
        if (event == FollowEvent.ACCEPT || event == FollowEvent.DECLINE) apply(event)
    }

    /** R3-3: [끊기]. R10 "따라가기 수락·끊기"라 주행 중에도 된다. */
    fun cancelFollow() = apply(FollowEvent.CANCEL)

    private fun apply(event: FollowEvent) {
        val current = follow ?: return
        val next = nextFollowState(current.state, event)
        follow = if (next == FollowState.ENDED) null else current.copy(state = next)
    }
}
