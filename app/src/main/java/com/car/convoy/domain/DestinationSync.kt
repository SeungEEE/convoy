package com.car.convoy.domain

/** SPEC R5. 목적지 동기화 판단 결과. */
enum class DestinationSyncResult { SYNC, IGNORE_SAME, NOT_LEADER }

/** SPEC R5: 리더만 바꿀 수 있고, 현재 목적지와 10m 미만이면 같은 목적지로 본다. */
fun shouldSyncDestination(isLeader: Boolean, distanceFromCurrentMeters: Double): DestinationSyncResult =
    TODO("SPEC R5")

/** SPEC R6: 마지막 요청 목적지에서 200m 이상 벗어나면 다시 요청한다. */
fun shouldReroute(distanceFromLastTargetMeters: Double): Boolean =
    TODO("SPEC R6")
