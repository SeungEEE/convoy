package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R7 예시표 전체. */
class ArrivalTest {

    // ---- gearEvent ----
    @Test
    fun park_to_drive_with_destination_departs() {
        assertEquals(GearEvent.DEPARTED, gearEvent(Gear.PARK, Gear.DRIVE, hasDestination = true))
    }

    @Test
    fun park_to_drive_without_destination_departs() {
        assertEquals(GearEvent.DEPARTED, gearEvent(Gear.PARK, Gear.DRIVE, hasDestination = false))
    }

    @Test
    fun drive_to_park_without_destination_arrives() {
        assertEquals(GearEvent.ARRIVED, gearEvent(Gear.DRIVE, Gear.PARK, hasDestination = false))
    }

    @Test
    fun drive_to_park_with_destination_is_none() {
        assertEquals(GearEvent.NONE, gearEvent(Gear.DRIVE, Gear.PARK, hasDestination = true))
    }

    @Test
    fun drive_to_neutral_is_none() {
        assertEquals(GearEvent.NONE, gearEvent(Gear.DRIVE, Gear.NEUTRAL, hasDestination = false))
    }

    @Test
    fun reverse_to_drive_is_none() {
        assertEquals(GearEvent.NONE, gearEvent(Gear.REVERSE, Gear.DRIVE, hasDestination = false))
    }

    @Test
    fun unknown_to_drive_is_none() {
        assertEquals(GearEvent.NONE, gearEvent(Gear.UNKNOWN, Gear.DRIVE, hasDestination = false))
    }

    @Test
    fun park_to_park_is_none() {
        assertEquals(GearEvent.NONE, gearEvent(Gear.PARK, Gear.PARK, hasDestination = false))
    }

    // ---- isGroupFinished ----
    @Test
    fun all_arrived_finishes() {
        assertTrue(isGroupFinished(listOf(MemberState.ARRIVED, MemberState.ARRIVED, MemberState.ARRIVED)))
    }

    @Test
    fun someone_still_driving_does_not_finish() {
        assertFalse(isGroupFinished(listOf(MemberState.ARRIVED, MemberState.DEPARTED, MemberState.ARRIVED)))
    }

    @Test
    fun solo_arrived_finishes() {
        assertTrue(isGroupFinished(listOf(MemberState.ARRIVED)))
    }

    @Test
    fun empty_group_does_not_finish() {
        assertFalse(isGroupFinished(emptyList()))
    }
}
