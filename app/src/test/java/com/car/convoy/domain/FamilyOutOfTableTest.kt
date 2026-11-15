package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SPEC R9-4: 표에 없는 조합은 상태를 바꾸지 않는다.
 */
class FamilyOutOfTableTest {

    @Test
    fun active_request_stays_active() {
        assertEquals(FamilyState.ACTIVE, nextFamilyState(FamilyState.ACTIVE, FamilyEvent.REQUEST))
    }

    @Test
    fun pending_request_stays_pending() {
        assertEquals(FamilyState.PENDING, nextFamilyState(FamilyState.PENDING, FamilyEvent.REQUEST))
    }

    @Test
    fun active_accept_stays_active() {
        assertEquals(FamilyState.ACTIVE, nextFamilyState(FamilyState.ACTIVE, FamilyEvent.ACCEPT))
    }

    @Test
    fun none_decline_stays_none() {
        assertEquals(FamilyState.NONE, nextFamilyState(FamilyState.NONE, FamilyEvent.DECLINE))
    }

    @Test
    fun none_disable_stays_none() {
        assertEquals(FamilyState.NONE, nextFamilyState(FamilyState.NONE, FamilyEvent.DISABLE))
    }

    // SPEC R9 표에 "PENDING + DISABLE → NONE(지정 취소)"이 추가되어 표 밖 조합이 아니게 됐다(작업 24, 사용자 승인).
    @Test
    fun pending_disable_becomes_none() {
        assertEquals(FamilyState.NONE, nextFamilyState(FamilyState.PENDING, FamilyEvent.DISABLE))
    }
}
