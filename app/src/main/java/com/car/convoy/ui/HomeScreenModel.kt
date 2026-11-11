package com.car.convoy.ui

import com.car.convoy.data.GroupRoster
import com.car.convoy.data.RosterState
import com.car.convoy.domain.DriverAction
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import com.car.convoy.domain.canPerform
import com.car.convoy.domain.visibleInFriendList

/** 연결된 사람 한 줄. */
data class PersonRow(
    val id: String,
    val name: String,
    val status: UserStatus,
    val statusLabel: String,
    /** "수락 대기", "그룹 멤버", 그룹 밖이면 null([그룹에 초대]). */
    val groupLabel: String?,
    val canInvite: Boolean,
)

/** 함께 출발 카드의 멤버 칩. */
data class DepartureChip(val id: String, val name: String, val isLeader: Boolean, val pending: Boolean)

/** 시연용 패널: 그룹 초대를 받은 가상 인물. */
data class PendingInvite(val id: String, val name: String)

/** SPEC R2-5, R4-5: 홈에 보일 값. Compose 화면은 이 값만 그린다. */
data class HomeScreenModel(
    val myStatus: UserStatus,
    val people: List<PersonRow>,
    val departure: List<DepartureChip>,
    val canStartDestination: Boolean,
    val showResume: Boolean,
    val canDisband: Boolean,
    val pendingInvite: PendingInvite?,
)

/** 홈 1열에서 쌓는 칸(가족 카드는 작업 24에서 맨 앞에 붙는다). */
enum class HomeSlot { DEPARTURE, DEMO, PEOPLE }

const val MY_NAME = "나"

/**
 * SPEC R4-5: 오프라인은 숨기고(R2-2), 그룹 밖이면 [그룹에 초대], 보낸 뒤 "수락 대기", 수락하면 "그룹 멤버".
 * 출발은 수락한 멤버가 1명 이상이고 정차 중이며 주행이 "없음"일 때만 된다.
 */
fun homeScreenModel(
    people: List<Friend>,
    myStatus: UserStatus,
    roster: GroupRoster,
    driveActive: Boolean,
    gear: Gear,
): HomeScreenModel {
    val names = people.associate { it.id to it.name }
    val rows = people.filter { visibleInFriendList(it.status) }.map { p ->
        PersonRow(
            id = p.id,
            name = p.name,
            status = p.status,
            statusLabel = userStatusLabel(p.status),
            groupLabel = when (roster.stateOf(p.id)) {
                RosterState.INVITED -> "수락 대기"
                RosterState.MEMBER -> "그룹 멤버"
                null -> null
            },
            canInvite = roster.canInviteNow(p.id, gear, driveActive),
        )
    }
    val chips = listOf(DepartureChip("me", MY_NAME, isLeader = true, pending = false)) +
        roster.members.map { DepartureChip(it, names[it] ?: it, isLeader = false, pending = false) } +
        roster.pendingInOrder.map { DepartureChip(it, names[it] ?: it, isLeader = false, pending = true) }
    return HomeScreenModel(
        myStatus = myStatus,
        people = rows,
        departure = chips,
        canStartDestination = !driveActive && roster.members.isNotEmpty() &&
            canPerform(DriverAction.SET_GROUP_DESTINATION, gear),
        showResume = driveActive,
        canDisband = roster.canDisbandNow(gear, driveActive),
        pendingInvite = roster.firstPendingInvite?.let { PendingInvite(it, names[it] ?: it) },
    )
}

/** SPEC R4-5: 1열 순서. 함께 출발, 시연용 패널(응답할 초대가 있을 때), 연결된 사람. */
fun homeSingleColumnOrder(model: HomeScreenModel): List<HomeSlot> = buildList {
    add(HomeSlot.DEPARTURE)
    if (model.pendingInvite != null) add(HomeSlot.DEMO)
    add(HomeSlot.PEOPLE)
}
