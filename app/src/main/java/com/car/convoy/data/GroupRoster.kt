package com.car.convoy.data

import com.car.convoy.domain.DriverAction
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.InviteResult
import com.car.convoy.domain.canInvite
import com.car.convoy.domain.canPerform
import com.car.convoy.domain.visibleInFriendList

/** SPEC R4-5: 이번 주행 그룹에서 한 사람의 상태. */
enum class RosterState { INVITED, MEMBER }

/**
 * 작업 23(SPEC R4-4, R4-5): 이번 주행 그룹. 나(리더)는 항상 있고, 연결된 사람을 초대해 수락받는다.
 * 초대와 해산은 주행이 "없음"이고 정차 중일 때만 된다. 응답은 시연용 패널이 [respond]로 만든다.
 */
class GroupRoster(private val people: List<Friend>) {

    // 초대한 순서를 지킨다.
    private val entries = LinkedHashMap<String, RosterState>()

    /** R4-5: `canInvite`의 인원. 나, 수락한 멤버, 수락 대기를 모두 센다. */
    val headCount: Int get() = 1 + entries.size

    /** 수락한 멤버(초대한 순서). */
    val members: List<String> get() = entries.filterValues { it == RosterState.MEMBER }.keys.toList()

    /** 수락 대기(초대한 순서). */
    val pendingInOrder: List<String> get() = entries.filterValues { it == RosterState.INVITED }.keys.toList()

    /** 시연용 패널이 응답을 만들 첫 수락 대기. */
    val firstPendingInvite: String? get() = pendingInOrder.firstOrNull()

    val isEmpty: Boolean get() = entries.isEmpty()

    fun stateOf(id: String): RosterState? = entries[id]

    /** R4-5 [그룹에 초대]를 누를 수 있는가. */
    fun canInviteNow(id: String, gear: Gear, driveActive: Boolean): Boolean {
        val person = people.firstOrNull { it.id == id } ?: return false
        return !driveActive &&
            id !in entries &&
            visibleInFriendList(person.status) &&
            canInvite(headCount, isFriend = true) == InviteResult.OK &&
            canPerform(DriverAction.CREATE_OR_INVITE_GROUP, gear)
    }

    fun invite(id: String, gear: Gear, driveActive: Boolean): Boolean {
        if (!canInviteNow(id, gear, driveActive)) return false
        entries[id] = RosterState.INVITED
        return true
    }

    /** R4-4: 수락하면 멤버, 거절하면 다시 초대할 수 있는 사람으로 돌아간다. 수락 대기만 받는다. */
    fun respond(id: String, accept: Boolean) {
        if (entries[id] != RosterState.INVITED) return
        if (accept) entries[id] = RosterState.MEMBER else entries.remove(id)
    }

    fun canDisbandNow(gear: Gear, driveActive: Boolean): Boolean =
        !isEmpty && !driveActive && canPerform(DriverAction.DISBAND_GROUP, gear)

    /** R4-5 [그룹 해산]: 멤버와 수락 대기를 모두 비운다. */
    fun disband(gear: Gear, driveActive: Boolean): Boolean {
        if (!canDisbandNow(gear, driveActive)) return false
        entries.clear()
        return true
    }
}
