package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R1 예시표 전체(작업 1, 2). */
class InviteCodeTest {

    @Test
    fun six_digits_is_ok() {
        assertEquals(InviteCodeResult.OK, validateInviteCode("123456"))
    }

    @Test
    fun all_zero_six_digits_is_ok() {
        assertEquals(InviteCodeResult.OK, validateInviteCode("000000"))
    }

    @Test
    fun five_chars_is_invalid() {
        assertEquals(InviteCodeResult.INVALID_FORMAT, validateInviteCode("12345"))
    }

    @Test
    fun seven_chars_is_invalid() {
        assertEquals(InviteCodeResult.INVALID_FORMAT, validateInviteCode("1234567"))
    }

    @Test
    fun letter_is_invalid() {
        assertEquals(InviteCodeResult.INVALID_FORMAT, validateInviteCode("12345a"))
    }

    @Test
    fun empty_is_invalid() {
        assertEquals(InviteCodeResult.INVALID_FORMAT, validateInviteCode(""))
    }

    @Test
    fun surrounding_spaces_are_invalid() {
        assertEquals(InviteCodeResult.INVALID_FORMAT, validateInviteCode(" 12345 "))
    }

    @Test
    fun age_0_is_not_expired() {
        assertEquals(false, isInviteCodeExpired(0))
    }

    @Test
    fun age_599_is_not_expired() {
        assertEquals(false, isInviteCodeExpired(599))
    }

    @Test
    fun age_600_is_not_expired() {
        assertEquals(false, isInviteCodeExpired(600))
    }

    @Test
    fun age_601_is_expired() {
        assertEquals(true, isInviteCodeExpired(601))
    }
}
