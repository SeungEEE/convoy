package com.car.convoy.domain

/** SPEC R7. 기어 상태. SDK 타입과 분리한 도메인 표현이며 어댑터(data/)가 변환한다. */
enum class Gear { PARK, DRIVE, NEUTRAL, REVERSE, UNKNOWN }

/** SPEC R7. 기어 변화가 만든 이벤트. */
enum class GearEvent { DEPARTED, ARRIVED, NONE }

/** SPEC R7. 그룹 멤버의 이동 상태. */
enum class MemberState { DEPARTED, ARRIVED }

/**
 * SPEC R7-1~3: P→D는 출발, 목적지가 없을 때 D→P는 도착이다.
 * 목적지가 있으면 D→P는 도착으로 보지 않는다(도착은 내비 이벤트가 알린다).
 */
fun gearEvent(previous: Gear, current: Gear, hasDestination: Boolean): GearEvent =
    TODO("SPEC R7-1~3")

/** SPEC R7-4: 멤버가 있고 모두 ARRIVED이면 그룹을 종료한다. */
fun isGroupFinished(memberStates: List<MemberState>): Boolean =
    TODO("SPEC R7-4")
