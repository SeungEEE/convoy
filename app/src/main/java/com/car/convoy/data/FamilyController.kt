package com.car.convoy.data

import com.car.convoy.domain.DriverAction
import com.car.convoy.domain.FamilyEvent
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.canPerform
import com.car.convoy.domain.nextFamilyState
import com.car.convoy.domain.visibleInFriendList

/**
 * 작업 24(SPEC R9-1, R9-2, R9-5): 가족 한 명. 운전자가 연결된 사람을 지정하면(정차 중, R10 "가족 등록")
 * 그 사람이 수락해야 등록된다. 그룹 주행과 무관하게 항상 연결된다. 해제·지정 취소는 언제든 된다.
 * 가상 인물의 응답은 시연용 패널이 [respond]로 만든다.
 */
class FamilyController(private val people: List<Friend>) {

    var state: FamilyState = FamilyState.NONE
        private set

    /** 지정한(수락 대기 또는 등록된) 사람. `NONE`이면 null. */
    var familyId: String? = null
        private set

    val familyName: String? get() = familyId?.let { id -> people.firstOrNull { it.id == id }?.name }

    /** R9-5: [가족으로 설정]을 누를 수 있는가. 가족은 한 명이라 `NONE`일 때만 된다. */
    fun canDesignate(id: String, gear: Gear): Boolean {
        val person = people.firstOrNull { it.id == id } ?: return false
        return state == FamilyState.NONE &&
            visibleInFriendList(person.status) &&
            canPerform(DriverAction.REGISTER_FAMILY, gear)
    }

    fun designate(id: String, gear: Gear): Boolean {
        if (!canDesignate(id, gear)) return false
        state = nextFamilyState(state, FamilyEvent.REQUEST)
        familyId = id
        return true
    }

    /** R9-1: 지정된 사람의 수락·거절. 수락 대기일 때만 받는다. */
    fun respond(accept: Boolean) {
        if (state != FamilyState.PENDING) return
        apply(if (accept) FamilyEvent.ACCEPT else FamilyEvent.DECLINE)
    }

    /** R9-2, R9-5: [가족 해제](ACTIVE)와 [지정 취소](PENDING). R10 "가족 해제·지정 취소"라 주행 중에도 된다. */
    fun release() = apply(FamilyEvent.DISABLE)

    private fun apply(event: FamilyEvent) {
        state = nextFamilyState(state, event)
        if (state == FamilyState.NONE) familyId = null
    }
}
