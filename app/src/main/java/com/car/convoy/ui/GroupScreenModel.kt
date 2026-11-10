package com.car.convoy.ui

import com.car.convoy.domain.DriverAction
import com.car.convoy.domain.Gear
import com.car.convoy.domain.GroupMemberState
import com.car.convoy.domain.GroupSession
import com.car.convoy.domain.GroupStatus
import com.car.convoy.domain.canPerform

/** 멤버 목록의 한 줄. */
data class MemberRow(val id: String, val name: String, val stateLabel: String, val isVirtual: Boolean)

/** SPEC R11-20: 그룹 주행 화면에 보일 값. Compose 화면은 이 값만 그린다. */
data class GroupScreenModel(
    val destinationName: String?,
    val finished: Boolean,
    val members: List<MemberRow>,
    val canChooseDestination: Boolean,
    val showRetry: Boolean,
    val demoMemberIds: List<String>,
)

/** SPEC R11-20: 멤버 상태 문구. */
fun memberStateLabel(state: GroupMemberState): String = when (state) {
    GroupMemberState.IDLE -> "출발 전"
    GroupMemberState.REROUTING -> "경로 찾는 중"
    GroupMemberState.DRIVING -> "주행 중"
    GroupMemberState.ARRIVED -> "도착"
}

/**
 * SPEC R10, R11-9, R11-18~20: 세션 상태를 화면 값으로 바꾼다.
 * 목적지 고르기는 리더이고 정차 중(canPerform)이며 그룹이 ACTIVE일 때만 된다(종료 뒤 변경은 무시되므로, R11-9).
 * 내 차가 아닌 멤버는 가상 멤버이고 시연 패널에 나온다(R11-18).
 */
fun groupScreenModel(
    session: GroupSession,
    myId: String,
    memberIds: List<String>,
    names: Map<String, String>,
    isLeader: Boolean,
    gear: Gear,
    canRetryMyRoute: Boolean,
): GroupScreenModel {
    val active = session.status == GroupStatus.ACTIVE
    return GroupScreenModel(
        destinationName = session.destination?.name,
        finished = session.status == GroupStatus.FINISHED,
        members = memberIds.map { id ->
            MemberRow(id, names[id] ?: id, memberStateLabel(session.memberState(id)), isVirtual = id != myId)
        },
        canChooseDestination = isLeader && active && canPerform(DriverAction.SET_GROUP_DESTINATION, gear),
        showRetry = canRetryMyRoute,
        demoMemberIds = memberIds.filter { it != myId },
    )
}
