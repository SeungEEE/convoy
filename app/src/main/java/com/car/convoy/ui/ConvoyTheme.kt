package com.car.convoy.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.car.convoy.R

/**
 * Pleos UI 가이드가 권장하는 Asta Sans(OFL, docs/licenses/AstaSans-OFL.txt). 가변 글꼴 파일 하나에서 굵기를 고른다.
 * 차에서 내려받지 않도록 앱에 넣었다.
 */
@OptIn(ExperimentalTextApi::class) // FontVariation(가변 글꼴 굵기). Compose BOM 2025.09에서 아직 실험 API다.
private fun asta(weight: FontWeight) =
    Font(R.font.asta_sans, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val ConvoyFont = FontFamily(
    asta(FontWeight.Normal),
    asta(FontWeight.Medium),
    asta(FontWeight.SemiBold),
    asta(FontWeight.Bold),
    asta(FontWeight.ExtraBold),
)

private val convoyColorScheme = lightColorScheme(
    primary = Palette.Brand,
    onPrimary = Color.White,
    primaryContainer = Palette.MemberFill,
    onPrimaryContainer = Palette.BrandNavy,
    secondary = Palette.BrandCyan,
    onSecondary = Palette.BrandNavy,
    secondaryContainer = Palette.BrandCyanFill,
    onSecondaryContainer = Palette.BrandNavy,
    tertiary = Palette.Driving,
    tertiaryContainer = Palette.ButtonFill,
    onTertiaryContainer = Palette.Ink,
    background = Palette.Page,
    onBackground = Palette.Ink,
    surface = Palette.Card,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.ButtonFill,
    onSurfaceVariant = Palette.Ink,
    outline = Palette.ButtonLine,
    error = Palette.Danger,
    onError = Color.White,
)

private fun TextStyle.asta() = copy(fontFamily = ConvoyFont)

private val convoyTypography = Typography().run {
    copy(
        displayLarge = displayLarge.asta(), displayMedium = displayMedium.asta(), displaySmall = displaySmall.asta(),
        headlineLarge = headlineLarge.asta(), headlineMedium = headlineMedium.asta(), headlineSmall = headlineSmall.asta(),
        titleLarge = titleLarge.asta(), titleMedium = titleMedium.asta(), titleSmall = titleSmall.asta(),
        bodyLarge = bodyLarge.asta(), bodyMedium = bodyMedium.asta(), bodySmall = bodySmall.asta(),
        labelLarge = labelLarge.asta(), labelMedium = labelMedium.asta(), labelSmall = labelSmall.asta(),
    )
}

/** 앱 공용 테마: 브랜드 색 구성과 Asta Sans. */
@Composable
fun ConvoyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = convoyColorScheme, typography = convoyTypography, content = content)
}
