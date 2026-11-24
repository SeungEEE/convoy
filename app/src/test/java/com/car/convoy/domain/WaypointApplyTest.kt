package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R13-6·7 `nextApplyState` 6행, `applySummary` 5행(AI 작성). */
class WaypointApplyTest {

    // ---- nextApplyState ----
    @Test
    fun sent_added_applied() {
        assertEquals(ApplyState.APPLIED, nextApplyState(ApplyState.SENT, ApplyEvent.WAYPOINT_ADDED))
    }

    @Test
    fun sent_add_failed() {
        assertEquals(ApplyState.FAILED, nextApplyState(ApplyState.SENT, ApplyEvent.ADD_FAILED))
    }

    @Test
    fun sent_timeout_failed() {
        assertEquals(ApplyState.FAILED, nextApplyState(ApplyState.SENT, ApplyEvent.TIMEOUT))
    }

    @Test
    fun failed_resend_sent() {
        assertEquals(ApplyState.SENT, nextApplyState(ApplyState.FAILED, ApplyEvent.RESEND))
    }

    @Test
    fun applied_resend_stays_applied() {
        assertEquals(ApplyState.APPLIED, nextApplyState(ApplyState.APPLIED, ApplyEvent.RESEND))
    }

    @Test
    fun failed_late_added_applied() {
        assertEquals(ApplyState.APPLIED, nextApplyState(ApplyState.FAILED, ApplyEvent.WAYPOINT_ADDED))
    }

    // ---- applySummary ----
    @Test
    fun all_applied_complete() {
        assertEquals(ApplySummary(3, 3, true, 0, 0), applySummary(listOf(ApplyState.APPLIED, ApplyState.APPLIED, ApplyState.APPLIED)))
    }

    @Test
    fun one_sent_waiting() {
        assertEquals(ApplySummary(2, 3, false, 1, 0), applySummary(listOf(ApplyState.APPLIED, ApplyState.APPLIED, ApplyState.SENT)))
    }

    @Test
    fun one_failed() {
        assertEquals(ApplySummary(2, 3, false, 0, 1), applySummary(listOf(ApplyState.APPLIED, ApplyState.FAILED, ApplyState.APPLIED)))
    }

    @Test
    fun alone_sent() {
        assertEquals(ApplySummary(0, 1, false, 1, 0), applySummary(listOf(ApplyState.SENT)))
    }

    @Test
    fun empty_not_complete() {
        assertEquals(ApplySummary(0, 0, false, 0, 0), applySummary(emptyList()))
    }
}
