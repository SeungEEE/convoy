package com.car.convoy.domain

/** SPEC R10. 사용자가 하려는 동작. */
enum class DriverAction {
    REGISTER_FRIEND,
    REGISTER_FAMILY,
    CREATE_OR_INVITE_GROUP,
    CHANGE_MY_STATUS,
    ACCEPT_INVITE,
    ACCEPT_OR_END_FOLLOW,
    RESPOND_ACCIDENT_CHECK,

    /** R10 표의 "그룹 목적지 변경"(R11-20). 설정이라 정차 중에만 한다. */
    SET_GROUP_DESTINATION,

    /** R10 표의 "따라가기 요청"(R2-5). 설정이라 정차 중에만 한다. */
    REQUEST_FOLLOW,

    /** R10 표의 "주행 끝내기"(R11-20, R11-22). 주행 중에도 된다. */
    END_DRIVE,

    /** R10 표의 "그룹 해산"(R4-5). 설정이라 정차 중에만 한다. */
    DISBAND_GROUP,

    /** R10 표의 "가족 해제·지정 취소"(R9-2, R9-5). 언제든 끌 수 있어 주행 중에도 된다. */
    DISABLE_FAMILY,
}

/**
 * SPEC R10-1: 설정은 정차(PARK) 중에만 한다.
 * 주행 중에는 상태 변경, 초대 수락, 따라가기 수락·끊기, 사고 확인 응답, 가족 해제·지정 취소, 주행 끝내기만 가능하다.
 * PARK이 아닌 기어(NEUTRAL, REVERSE, UNKNOWN 포함)는 모두 주행 중으로 본다.
 */
fun canPerform(action: DriverAction, gear: Gear): Boolean =
    gear == Gear.PARK || action in ALLOWED_WHILE_DRIVING

private val ALLOWED_WHILE_DRIVING = setOf(
    DriverAction.CHANGE_MY_STATUS,
    DriverAction.ACCEPT_INVITE,
    DriverAction.ACCEPT_OR_END_FOLLOW,
    DriverAction.RESPOND_ACCIDENT_CHECK,
    DriverAction.END_DRIVE,
    DriverAction.DISABLE_FAMILY,
)
