package com.car.convoy.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Codex 리뷰: 홈의 가족·수락 대기·그룹 멤버 태그 색도 대비 4.5:1 이상인지 고정한다. */
class PaletteFamilyContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(name: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue("$name 대비 ${"%.2f".format(r)} < 4.5", r >= 4.5)
    }

    @Test fun tags() {
        check("가족 태그", Palette.FamilyText, Palette.FamilyFill)
        check("수락 대기 태그", Palette.PendingText, Palette.PendingFill)
        check("그룹 멤버 태그", Palette.MemberText, Palette.MemberFill)
    }
}
