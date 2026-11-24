package com.car.convoy.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** SPEC R14-20: 합류 비교 표 한 줄의 "도착" 칸과 "우회" 칸. [detour]가 null이면 우회 칸을 숨긴다(목적지 없음). */
data class MeetingRowText(val arrival: String, val detour: String?)

private val MEETING_CLOCK = DateTimeFormatter.ofPattern("HH:mm")

/**
 * SPEC R14-20 `meetingRowText`. 위치 없음 → 계산 중 → 결과 순으로 따진다.
 * [nowMillis]는 후보를 고른 시각이다(표가 저절로 바뀌지 않게). [detourSeconds]는 `detourSeconds`의 결과라 0 이상이거나 null이다.
 */
fun meetingRowText(
    nowMillis: Long,
    zone: ZoneId,
    located: Boolean,
    loading: Boolean,
    toCandidateSeconds: Int?,
    detourSeconds: Int?,
    hasDestination: Boolean,
): MeetingRowText {
    if (!located) return MeetingRowText(LOCATING, LOCATING)
    if (loading) return MeetingRowText(CALCULATING, CALCULATING)
    // HH:mm으로 쓰면 초는 버려지고 날짜는 쓰지 않는다(R14-10·15).
    val arrival = toCandidateSeconds?.let {
        "${MEETING_CLOCK.format(Instant.ofEpochMilli(nowMillis).plusSeconds(it.toLong()).atZone(zone))} 도착"
    } ?: FAILED
    val detour = if (!hasDestination) null else detourSeconds?.let { detourText(it) } ?: FAILED
    return MeetingRowText(arrival, detour)
}

private const val LOCATING = "위치 확인 중"
private const val CALCULATING = "계산 중"
private const val FAILED = "계산 못 함"

/** 분으로 반올림(0.5분은 올림). 0분이면 "우회 없음", 60분 이상은 "우회 1시간 5분"(나머지가 0이면 "우회 1시간"). */
private fun detourText(seconds: Int): String {
    val minutes = (seconds + 30) / 60
    return when {
        minutes == 0 -> "우회 없음"
        minutes < 60 -> "우회 ${minutes}분"
        minutes % 60 == 0 -> "우회 ${minutes / 60}시간"
        else -> "우회 ${minutes / 60}시간 ${minutes % 60}분"
    }
}

/** SPEC R14-20 `proposalNotice`: [checkProposal] 결과에 대한 안내 문구. 그룹 화면 [경유지로]와 합류 비교가 같이 쓴다. */
fun proposalNotice(check: ProposalCheck): String? = when (check) {
    ProposalCheck.OK -> null
    ProposalCheck.FULL -> "경유지는 2개까지 넣을 수 있어요"
    ProposalCheck.PENDING_EXISTS -> "답하지 않은 요청이 있어요"
    ProposalCheck.APPLYING -> "앞 경유지를 반영하는 중이에요"
    ProposalCheck.SAME_AS_DESTINATION -> "목적지와 같은 곳이에요"
    ProposalCheck.ALREADY_WAYPOINT -> "이미 들르는 곳이에요"
    ProposalCheck.NO_DESTINATION, ProposalCheck.NOT_ACTIVE -> "지금은 경유지를 넣을 수 없어요"
}

/** SPEC R14-20 `canOpenMeetingCompare`: 그룹이 진행 중이고 정차 중(R10 "경유지 제안")일 때만 연다. */
fun canOpenMeetingCompare(groupStatus: GroupStatus, gear: Gear): Boolean =
    groupStatus == GroupStatus.ACTIVE && canPerform(DriverAction.PROPOSE_WAYPOINT, gear)
