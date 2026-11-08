package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R10 예시표 14칸과, PARK이 아닌 기어를 DRIVE와 같게 취급하는 규칙. */
class DrivingRestrictionTest {

    // ---- 표: PARK ----
    @Test fun park_register_friend() = assertEquals(true, canPerform(DriverAction.REGISTER_FRIEND, Gear.PARK))
    @Test fun park_register_family() = assertEquals(true, canPerform(DriverAction.REGISTER_FAMILY, Gear.PARK))
    @Test fun park_create_or_invite_group() = assertEquals(true, canPerform(DriverAction.CREATE_OR_INVITE_GROUP, Gear.PARK))
    @Test fun park_change_my_status() = assertEquals(true, canPerform(DriverAction.CHANGE_MY_STATUS, Gear.PARK))
    @Test fun park_accept_invite() = assertEquals(true, canPerform(DriverAction.ACCEPT_INVITE, Gear.PARK))
    @Test fun park_accept_or_end_follow() = assertEquals(true, canPerform(DriverAction.ACCEPT_OR_END_FOLLOW, Gear.PARK))
    @Test fun park_respond_accident_check() = assertEquals(true, canPerform(DriverAction.RESPOND_ACCIDENT_CHECK, Gear.PARK))

    // ---- 표: DRIVE ----
    @Test fun drive_register_friend() = assertEquals(false, canPerform(DriverAction.REGISTER_FRIEND, Gear.DRIVE))
    @Test fun drive_register_family() = assertEquals(false, canPerform(DriverAction.REGISTER_FAMILY, Gear.DRIVE))
    @Test fun drive_create_or_invite_group() = assertEquals(false, canPerform(DriverAction.CREATE_OR_INVITE_GROUP, Gear.DRIVE))
    @Test fun drive_change_my_status() = assertEquals(true, canPerform(DriverAction.CHANGE_MY_STATUS, Gear.DRIVE))
    @Test fun drive_accept_invite() = assertEquals(true, canPerform(DriverAction.ACCEPT_INVITE, Gear.DRIVE))
    @Test fun drive_accept_or_end_follow() = assertEquals(true, canPerform(DriverAction.ACCEPT_OR_END_FOLLOW, Gear.DRIVE))
    @Test fun drive_respond_accident_check() = assertEquals(true, canPerform(DriverAction.RESPOND_ACCIDENT_CHECK, Gear.DRIVE))

    // ---- 표 아래 주석: NEUTRAL, REVERSE, UNKNOWN은 DRIVE와 같다 ----
    @Test
    fun neutral_blocks_settings_allows_driving_actions() {
        assertEquals(false, canPerform(DriverAction.REGISTER_FRIEND, Gear.NEUTRAL))
        assertEquals(true, canPerform(DriverAction.RESPOND_ACCIDENT_CHECK, Gear.NEUTRAL))
    }

    @Test
    fun reverse_blocks_settings_allows_driving_actions() {
        assertEquals(false, canPerform(DriverAction.REGISTER_FAMILY, Gear.REVERSE))
        assertEquals(true, canPerform(DriverAction.ACCEPT_INVITE, Gear.REVERSE))
    }

    @Test
    fun unknown_blocks_settings_allows_driving_actions() {
        assertEquals(false, canPerform(DriverAction.CREATE_OR_INVITE_GROUP, Gear.UNKNOWN))
        assertEquals(true, canPerform(DriverAction.CHANGE_MY_STATUS, Gear.UNKNOWN))
    }
}
