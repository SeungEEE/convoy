package com.car.convoy.ui

import com.car.convoy.domain.Destination
import com.car.convoy.domain.ReportLocation
import com.car.convoy.domain.distanceMeters
import java.util.Locale

/** SPEC R12-4: 앞차 따라가기 화면에 보일 값. */
data class FollowScreenModel(
    val title: String,
    /** "약 850m 앞", "약 1.2km 앞", 내 위치를 모르면 "거리 확인 중". */
    val distanceLabel: String,
    /** 앞차 위치 갱신 뒤 지난 시간("2초 전"). */
    val updatedLabel: String,
    /** 경로 다시 요청 횟수("2번"). */
    val rerouteLabel: String,
    /** 마지막 요청 뒤 앞차가 움직인 거리("80m"). */
    val movedLabel: String,
    val showRetry: Boolean,
    /** 앞차가 된 멤버 이름(시안의 "나 → 민수" 그림, 함께 가는 멤버 목록). */
    val leaderName: String = "",
)

/** SPEC R12-4: 앞차까지 거리는 내 위치(R8-8 LocationPort)와 앞차 위치 사이의 distanceMeters다. */
fun followScreenModel(
    leaderName: String,
    me: ReportLocation?,
    leader: Destination,
    leaderUpdatedAt: Long,
    now: Long,
    reroutes: Int,
    moved: Double,
    failed: Boolean,
): FollowScreenModel {
    val distance = me?.let { distanceMeters(Destination("", "", it.latitude, it.longitude), leader) }
    return FollowScreenModel(
        title = "$leaderName 차를 따라가는 중",
        distanceLabel = when {
            distance == null -> "거리 확인 중"
            distance < 1_000 -> "약 ${Math.round(distance)}m 앞"
            else -> "약 ${String.format(Locale.US, "%.1f", distance / 1_000)}km 앞"
        },
        updatedLabel = "${((now - leaderUpdatedAt).coerceAtLeast(0) / 1_000)}초 전",
        rerouteLabel = "${reroutes}번",
        movedLabel = "${Math.round(moved)}m",
        showRetry = failed,
        leaderName = leaderName,
    )
}
