package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R5, R6 예시표. 비어 있는 행은 사람이 SPEC 표를 보고 채운다. */
class DestinationSyncTest {

    // ---- R5 shouldSyncDestination ----
    @Test
    fun leader_far_syncs() {
        assertEquals(DestinationSyncResult.SYNC, shouldSyncDestination(true, 500.0))
    }

    @Test
    fun member_cannot_change() {
        assertEquals(DestinationSyncResult.NOT_LEADER, shouldSyncDestination(false, 500.0))
    }

    @Test
    fun leader_at_10_syncs() {
        assertEquals(DestinationSyncResult.SYNC, shouldSyncDestination(true, 10.0))
    }

    @Test
    fun leader_at_9_ignore_same() {
        assertEquals(DestinationSyncResult.IGNORE_SAME, shouldSyncDestination(true, 9.0))
    }

    @Test
    fun leader_at_0_ignore_same() {
        assertEquals(DestinationSyncResult.IGNORE_SAME, shouldSyncDestination(true, 0.0))
    }

    // ---- R6 shouldReroute ----
    @Test
    fun reroute_at_200_true() {
        assertTrue(shouldReroute(200.0))
    }

    @Test
    fun reroute_at_199_false() {
        assertFalse(shouldReroute(199.0))
    }

    @Test
    fun reroute_at_0_false() {
        assertFalse(shouldReroute(0.0))
    }

    @Test
    fun reroute_at_201_true() {
        assertTrue(shouldReroute(201.0))
    }
}
