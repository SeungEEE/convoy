package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R9 표의 새 행(작업 24): 수락 대기 중 지정 취소. R10 "가족 해제·지정 취소"는 주행 중에도 된다. */
class FamilyCancelTest {

    @Test fun pending_disable_cancels() =
        assertEquals(FamilyState.NONE, nextFamilyState(FamilyState.PENDING, FamilyEvent.DISABLE))

    @Test fun disable_family_allowed_always() {
        Gear.entries.forEach { assertEquals("$it", true, canPerform(DriverAction.DISABLE_FAMILY, it)) }
    }

    @Test fun register_family_only_parked() {
        assertEquals(true, canPerform(DriverAction.REGISTER_FAMILY, Gear.PARK))
        assertEquals(false, canPerform(DriverAction.REGISTER_FAMILY, Gear.DRIVE))
    }
}
