package com.car.convoy.ui

import com.car.convoy.domain.FollowLink
import com.car.convoy.domain.FollowState
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import com.car.convoy.domain.canStartFollowRequest
import com.car.convoy.domain.visibleInFriendList

/** SPEC R2-5: 화면 위 탭. */
enum class AppTab(val label: String) { FRIENDS("친구"), GROUP("그룹 주행") }

/** 친구 목록의 한 줄. */
data class FriendRow(
    val id: String,
    val name: String,
    val status: UserStatus,
    val statusLabel: String,
    /** 이 친구와의 따라가기 상태 문구. 없으면 null. */
    val followLabel: String?,
    val canRequest: Boolean,
    /** [따라가기 요청]을 못 누르는 이유. */
    val note: String?,
    val showCancel: Boolean,
)

/** 시연용 패널: 요청을 받은 가상 친구. */
data class PendingRequest(val friendId: String, val name: String)

/** SPEC R2-5: 친구 목록 화면에 보일 값. Compose 화면은 이 값만 그린다. */
data class FriendScreenModel(
    val myStatus: UserStatus,
    val rows: List<FriendRow>,
    val pendingRequest: PendingRequest?,
)

/** SPEC R2-1, R2-5: 상태 문구. */
fun userStatusLabel(status: UserStatus): String = when (status) {
    UserStatus.ACTIVE -> "활동 중"
    UserStatus.BUSY -> "업무 중"
    UserStatus.OFFLINE -> "오프라인"
}

/**
 * SPEC R2-5: OFFLINE 친구는 숨기고(R2-2), 진행 중인(REQUESTED·ACTIVE) 따라가기가 있으면 다른 친구의 요청을 막는다.
 * ENDED인 따라가기는 없는 것과 같다. [끊기]는 R10 "따라가기 수락·끊기"라 기어와 무관하게 보인다.
 */
fun friendScreenModel(
    friends: List<Friend>,
    myStatus: UserStatus,
    follow: FollowLink?,
    gear: Gear,
): FriendScreenModel {
    val live = follow?.takeIf { it.state != FollowState.ENDED }
    val rows = friends.filter { visibleInFriendList(it.status) }.map { f ->
        val mine = live?.takeIf { it.friendId == f.id }
        val canRequest = canStartFollowRequest(f.status, gear, followInProgress = live != null)
        FriendRow(
            id = f.id,
            name = f.name,
            status = f.status,
            statusLabel = userStatusLabel(f.status),
            followLabel = when (mine?.state) {
                FollowState.REQUESTED -> "수락 기다리는 중"
                FollowState.ACTIVE -> "따라가는 중"
                else -> null
            },
            canRequest = canRequest,
            note = when {
                mine != null || canRequest -> null
                f.status == UserStatus.BUSY -> "업무 중이라 요청할 수 없어요"
                live == null && gear != Gear.PARK -> "정차 중에만 요청할 수 있어요"
                else -> null
            },
            showCancel = mine?.state == FollowState.ACTIVE,
        )
    }
    val pending = live?.takeIf { it.state == FollowState.REQUESTED }?.let { l ->
        friends.firstOrNull { it.id == l.friendId }?.let { PendingRequest(it.id, it.name) }
    }
    return FriendScreenModel(myStatus, rows, pending)
}
