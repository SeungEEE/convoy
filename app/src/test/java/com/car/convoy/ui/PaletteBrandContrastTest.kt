package com.car.convoy.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** 브랜드 색 하나(#086AB5)로 정리한 화면의 글자·바탕 조합도 대비 4.5:1 이상이다(Pleos UI 가이드). */
class PaletteBrandContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(name: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue("$name 대비 ${"%.2f".format(r)} < 4.5", r >= 4.5)
    }

    @Test fun brand_is_the_icon_blue() = assertTrue(Palette.Brand == Color(0xFF086AB5))

    @Test fun text_on_brand_fills() {
        check("흰 글자/브랜드", Color.White, Palette.Brand)
        check("연한 글자/브랜드", Palette.OnBrandSoft, Palette.Brand)
        check("흰 글자/진한 브랜드", Color.White, Palette.BrandDeep)
        check("연한 글자/진한 브랜드", Palette.OnBrandSoft, Palette.BrandDeep)
    }

    @Test fun blue_text_on_light() {
        check("강조 글자/바탕", Palette.Accent, Palette.Page)
        check("강조 글자/버튼 바탕", Palette.Accent, Palette.ButtonFill)
    }
}
