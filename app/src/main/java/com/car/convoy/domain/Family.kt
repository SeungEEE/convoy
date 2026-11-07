package com.car.convoy.domain

/** SPEC R9. 가족 공유 상태. */
enum class FamilyState { NONE, PENDING, ACTIVE }

/** SPEC R9. 가족 공유 상태를 바꾸는 이벤트. */
enum class FamilyEvent { REQUEST, ACCEPT, DECLINE, DISABLE }

/**
 * SPEC R9: 요청 → 대기, 수락 → 활성, 거절 → 없음, 끄기 → 없음.
 * 표에 없는 조합은 상태를 바꾸지 않는다(예: 요청 없이 수락할 수 없다).
 */
fun nextFamilyState(current: FamilyState, event: FamilyEvent): FamilyState =
    when (current to event) {
        FamilyState.NONE to FamilyEvent.REQUEST -> FamilyState.PENDING
        FamilyState.PENDING to FamilyEvent.ACCEPT -> FamilyState.ACTIVE
        FamilyState.PENDING to FamilyEvent.DECLINE -> FamilyState.NONE
        FamilyState.ACTIVE to FamilyEvent.DISABLE -> FamilyState.NONE
        else -> current
    }
