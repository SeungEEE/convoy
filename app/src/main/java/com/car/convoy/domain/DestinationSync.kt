package com.car.convoy.domain

/** SPEC R5. 목적지 동기화 판단 결과. */
enum class DestinationSyncResult { SYNC, IGNORE_SAME, NOT_LEADER }

private const val SAME_DESTINATION_THRESHOLD_METERS = 10.0
private const val REROUTE_THRESHOLD_METERS = 200.0

/** SPEC R5: 리더만 바꿀 수 있고, 현재 목적지와 10m 미만이면 같은 목적지로 본다. */
fun shouldSyncDestination(isLeader: Boolean, distanceFromCurrentMeters: Double): DestinationSyncResult =
    when {
        !isLeader -> DestinationSyncResult.NOT_LEADER
        distanceFromCurrentMeters < SAME_DESTINATION_THRESHOLD_METERS -> DestinationSyncResult.IGNORE_SAME
        else -> DestinationSyncResult.SYNC
    }

/** SPEC R6: 마지막 요청 목적지에서 200m 이상 벗어나면 다시 요청한다. */
fun shouldReroute(distanceFromLastTargetMeters: Double): Boolean =
    distanceFromLastTargetMeters >= REROUTE_THRESHOLD_METERS
