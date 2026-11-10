package com.car.convoy.domain

/** SPEC R1. 초대 코드 검사 결과. */
enum class InviteCodeResult { OK, INVALID_FORMAT }

/** SPEC R1-2 (제안): 코드 유효 시간(초). */
const val INVITE_CODE_TTL_SECONDS = 600

/**
 * SPEC R1-1: 숫자(0~9) 정확히 6자리만 OK다. 앞뒤 공백도 고치지 않고 INVALID_FORMAT으로 본다.
 * [Char.isDigit]은 다른 문자 체계의 숫자도 받으므로 쓰지 않는다.
 */
fun validateInviteCode(code: String): InviteCodeResult =
    if (code.length == 6 && code.all { it in '0'..'9' }) InviteCodeResult.OK else InviteCodeResult.INVALID_FORMAT

/** SPEC R1-2: 600초까지는 유효하고 601초부터 만료다. */
fun isInviteCodeExpired(ageSeconds: Int): Boolean = ageSeconds > INVITE_CODE_TTL_SECONDS
