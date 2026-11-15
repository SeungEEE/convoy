package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R9 예시표 전체. */
class FamilyTest {

    @Test
    fun none_request_becomes_pending() {
        assertEquals(FamilyState.PENDING, nextFamilyState(FamilyState.NONE, FamilyEvent.REQUEST))
    }

    @Test
    fun pending_accept_becomes_active() {
        assertEquals(FamilyState.ACTIVE, nextFamilyState(FamilyState.PENDING, FamilyEvent.ACCEPT))
    }

    @Test
    fun pending_decline_becomes_none() {
        assertEquals(FamilyState.NONE, nextFamilyState(FamilyState.PENDING, FamilyEvent.DECLINE))
    }

    @Test
    fun active_disable_becomes_none() {
        assertEquals(FamilyState.NONE, nextFamilyState(FamilyState.ACTIVE, FamilyEvent.DISABLE))
    }

    @Test
    fun none_accept_stays_none() {
        assertEquals(FamilyState.NONE, nextFamilyState(FamilyState.NONE, FamilyEvent.ACCEPT))
    }
}
