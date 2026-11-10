package com.car.convoy.domain

/** SPEC R2-1. 내 상태. */
enum class UserStatus { ACTIVE, BUSY, OFFLINE }

/** SPEC R2-3. 따라가기 요청 가능 여부. */
enum class FollowRequestResult { OK, REJECT_BUSY, REJECT_OFFLINE }

/** SPEC R3. 따라가기 상태. */
enum class FollowState { REQUESTED, ACTIVE, ENDED }

/** SPEC R3. 따라가기 상태를 바꾸는 이벤트. CANCEL은 양쪽 어느 쪽이든 끊기다. */
enum class FollowEvent { ACCEPT, DECLINE, CANCEL }

/** SPEC R2-2: OFFLINE은 목록에 보이지 않는다(앱을 안 쓰는 것과 구분되지 않게). */
fun visibleInFriendList(status: UserStatus): Boolean = status != UserStatus.OFFLINE

/** SPEC R2-3: 활동 중인 상대에게만 따라가기를 요청할 수 있다. */
fun canRequestFollow(targetStatus: UserStatus): FollowRequestResult = when (targetStatus) {
    UserStatus.ACTIVE -> FollowRequestResult.OK
    UserStatus.BUSY -> FollowRequestResult.REJECT_BUSY
    UserStatus.OFFLINE -> FollowRequestResult.REJECT_OFFLINE
}

/**
 * SPEC R3: 수락 → 활성, 거절 → 끝, 끊기(요청 중이든 활성이든) → 끝.
 * 표에 없는 조합은 R9처럼 상태를 바꾸지 않는다(예: 끝난 뒤의 수락·끊기).
 */
fun nextFollowState(current: FollowState, event: FollowEvent): FollowState =
    when (current to event) {
        FollowState.REQUESTED to FollowEvent.ACCEPT -> FollowState.ACTIVE
        FollowState.REQUESTED to FollowEvent.DECLINE -> FollowState.ENDED
        FollowState.REQUESTED to FollowEvent.CANCEL -> FollowState.ENDED
        FollowState.ACTIVE to FollowEvent.CANCEL -> FollowState.ENDED
        else -> current
    }
