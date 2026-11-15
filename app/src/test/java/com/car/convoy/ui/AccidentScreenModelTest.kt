package com.car.convoy.ui

import com.car.convoy.domain.AccidentReport
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.ReportLocation
import com.car.convoy.domain.TelltaleType
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R8-2~5, R8-9, R8-10, 작업 21: 사고 확인 화면과 가족 알림 카드에 보일 값. */
class AccidentScreenModelTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    // 2026-11-10 10:48:31 KST
    private val at = java.time.ZonedDateTime.of(2026, 11, 10, 10, 48, 31, 0, seoul).toInstant().toEpochMilli()
    private val report = AccidentReport(at, ReportLocation(37.4127, 127.0947, "수정구 시흥동"), listOf(TelltaleType.AIRBAG))

    private fun model(
        checking: Boolean = false,
        remaining: Int = 0,
        family: FamilyState = FamilyState.ACTIVE,
        sent: AccidentReport? = null,
    ) = accidentScreenModel(checking, remaining, family, sent, seoul)

    // ---- "괜찮으세요?" (R8-1~5) ----
    @Test fun prompt_hidden_when_not_checking() = assertFalse(model().showPrompt)

    @Test fun prompt_with_family_counts_down() {
        val m = model(checking = true, remaining = 18)
        assertTrue(m.showPrompt)
        assertEquals("18", m.remainingLabel)
        assertEquals("응답이 없으면 18초 뒤 가족에게 알려요", m.promptDetail)
    }

    @Test fun prompt_without_family_says_no_alert() =
        assertEquals("가족이 등록되어 있지 않아 알리지 않아요", model(checking = true, remaining = 18, family = FamilyState.NONE).promptDetail)

    // ---- 가족 표시 (R8-9, R9-2) ----
    @Test fun demo_family_label() = assertEquals("가족 1명 (시연 설정)", model().familyLabel)
    @Test fun family_off_label() = assertEquals("가족 공유 꺼짐", model(family = FamilyState.NONE).familyLabel)

    // ---- 가족에게 보낸 알림 카드 (R8-10) ----
    @Test fun no_card_without_sent_report() = assertNull(model().sentCard)

    @Test fun card_shows_time_location_and_telltale() = assertEquals(
        SentCard(timeLabel = "10:48:31", locationLabel = "수정구 시흥동 (37.4127, 127.0947)", telltaleLabel = "에어백"),
        model(sent = report).sentCard,
    )

    @Test fun card_without_location() = assertEquals(
        "위치 확인 불가",
        model(sent = report.copy(location = null)).sentCard!!.locationLabel,
    )

    @Test fun card_lists_multiple_telltales() = assertEquals(
        "에어백, 긴급호출",
        model(sent = report.copy(telltales = listOf(TelltaleType.AIRBAG, TelltaleType.EMERGENCY_CALL))).sentCard!!.telltaleLabel,
    )

    // 확인 중에는 카드를 보이지 않는다(새 확인이 시작되면 이전 보고서는 이미 지워진다, R8-10).
    @Test fun no_card_while_checking() = assertNull(model(checking = true, remaining = 10, sent = report).sentCard)
}
