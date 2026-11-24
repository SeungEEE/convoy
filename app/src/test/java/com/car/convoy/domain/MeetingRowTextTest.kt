package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** SPEC R14-20 `meetingRowText` 18행 (작업 37, AI 작성). 지금 시각은 10:00:00(Asia/Seoul). */
class MeetingRowTextTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val now = ZonedDateTime.of(2026, 11, 24, 10, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun row(located: Boolean = true, loading: Boolean = false, toCandidate: Int?, detour: Int?, destination: Boolean = true) =
        meetingRowText(now, zone, located, loading, toCandidate, detour, destination)

    private fun check(expected: MeetingRowText, actual: MeetingRowText) = assertEquals(expected, actual)

    @Test fun arrival_and_detour() = check(MeetingRowText("10:20 도착", "우회 3분"), row(toCandidate = 1200, detour = 180))

    @Test fun arrival_drops_seconds() = check(MeetingRowText("10:20 도착", "우회 3분"), row(toCandidate = 1259, detour = 180))

    @Test fun zero_detour() = check(MeetingRowText("10:20 도착", "우회 없음"), row(toCandidate = 1200, detour = 0))

    @Test fun under_half_minute_is_none() = check(MeetingRowText("10:20 도착", "우회 없음"), row(toCandidate = 1200, detour = 29))

    @Test fun half_minute_rounds_up() = check(MeetingRowText("10:20 도착", "우회 1분"), row(toCandidate = 1200, detour = 30))

    @Test fun one_hour() = check(MeetingRowText("10:20 도착", "우회 1시간"), row(toCandidate = 1200, detour = 3600))

    @Test fun just_under_hour() = check(MeetingRowText("10:20 도착", "우회 59분"), row(toCandidate = 1200, detour = 3569))

    @Test fun rounds_up_to_hour() = check(MeetingRowText("10:20 도착", "우회 1시간"), row(toCandidate = 1200, detour = 3570))

    @Test fun hour_and_minutes() = check(MeetingRowText("10:20 도착", "우회 1시간 5분"), row(toCandidate = 1200, detour = 3900))

    @Test fun detour_failed() = check(MeetingRowText("10:20 도착", "계산 못 함"), row(toCandidate = 1200, detour = null))

    @Test fun all_failed() = check(MeetingRowText("계산 못 함", "계산 못 함"), row(toCandidate = null, detour = null))

    @Test fun no_destination_hides_detour() =
        check(MeetingRowText("10:20 도착", null), row(toCandidate = 1200, detour = null, destination = false))

    @Test fun no_destination_failed() =
        check(MeetingRowText("계산 못 함", null), row(toCandidate = null, detour = null, destination = false))

    @Test fun next_day_without_date() = check(MeetingRowText("00:10 도착", "우회 3분"), row(toCandidate = 51000, detour = 180))

    @Test fun loading_hides_partial() =
        check(MeetingRowText("계산 중", "계산 중"), row(loading = true, toCandidate = 1200, detour = null))

    @Test fun no_location() =
        check(MeetingRowText("위치 확인 중", "위치 확인 중"), row(located = false, toCandidate = null, detour = null))

    @Test fun no_location_before_destination() =
        check(MeetingRowText("위치 확인 중", "위치 확인 중"), row(located = false, toCandidate = null, detour = null, destination = false))

    @Test fun no_location_before_loading() =
        check(MeetingRowText("위치 확인 중", "위치 확인 중"), row(located = false, loading = true, toCandidate = null, detour = null))
}
