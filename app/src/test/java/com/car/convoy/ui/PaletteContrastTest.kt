package com.car.convoy.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.car.convoy.domain.GroupMemberState
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pleos UI 가이드: 텍스트·아이콘 대비 4.5:1 이상(docs/sdk-findings.md). 화면에서 실제로 쓰는 글자색·배경 조합. */
class PaletteContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(name: String, text: Color, bg: Color) {
        val r = ratio(text, bg)
        assertTrue("$name 대비 ${"%.2f".format(r)} < 4.5", r >= 4.5)
    }

    /** Chip: 글자색 위에 같은 색 12% 배경(GroupScreen.Chip). */
    private fun chip(name: String, color: Color, under: Color) =
        check("$name 칩", color, color.copy(alpha = 0.12f).compositeOver(under))

    private val textColors = mapOf(
        "Ink" to Palette.Ink, "Soft" to Palette.Soft, "Accent" to Palette.Accent,
        "Danger" to Palette.Danger, "Demo" to Palette.Demo, "Done" to Palette.Done,
    )

    @Test fun text_on_card_and_page() {
        textColors.forEach { (n, c) ->
            check("$n/Card", c, Palette.Card)
            check("$n/Page", c, Palette.Page)
        }
    }

    @Test fun chips_on_card_and_page() {
        val chipColors = textColors + GroupMemberState.entries.associate { "member ${it.name}" to memberStateColor(it) } +
            UserStatus.entries.associate { "user ${it.name}" to userStatusColor(it) }
        chipColors.forEach { (n, c) ->
            chip(n, c, Palette.Card)
            chip(n, c, Palette.Page)
        }
    }

    @Test fun white_on_state_circles_and_selected_buttons() {
        GroupMemberState.entries.forEach { check("흰 글자/${it.name}", Color.White, memberStateColor(it)) }
        UserStatus.entries.forEach { check("흰 글자/${it.name}", Color.White, userStatusColor(it)) }
        check("흰 글자/Accent", Color.White, Palette.Accent)
        check("흰 글자/Done", Color.White, Palette.Done)
    }

    @Test fun filled_areas() {
        check("Accent/선택 버튼 바탕", Palette.Accent, Palette.ButtonFill)
        check("Done/모두 도착 배너", Palette.Done, Palette.DoneFill)
        check("Danger/사고 원", Palette.Danger, Palette.DangerSoft)
    }
}
