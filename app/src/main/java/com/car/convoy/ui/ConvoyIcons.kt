package com.car.convoy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 시안(Convoy 그룹 흐름 시안)의 선 아이콘. 아이콘 라이브러리를 더 넣지 않으려고 24 단위 격자에 직접 그린다.
 * 장식용이라 접근성 설명은 옆 글자가 맡는다.
 */
enum class ConvoyIcon { Pin, Check, ChevronRight, ChevronLeft, Search, Shield, Home, Building, Link, Group }

@Composable
fun Icon24(icon: ConvoyIcon, color: Color, size: Dp = 32.dp, strokeWidth: Float = 2.2f) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val stroke = Stroke(width = strokeWidth * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(vararg pts: Pair<Float, Float>) = Path().apply {
            pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
        }
        when (icon) {
            ConvoyIcon.Pin -> {
                drawPath(Path().apply {
                    moveTo(12 * u, 21 * u)
                    cubicTo(12 * u, 21 * u, 5 * u, 14.8f * u, 5 * u, 9.5f * u)
                    cubicTo(5 * u, 5.6f * u, 8.1f * u, 2.5f * u, 12 * u, 2.5f * u)
                    cubicTo(15.9f * u, 2.5f * u, 19 * u, 5.6f * u, 19 * u, 9.5f * u)
                    cubicTo(19 * u, 14.8f * u, 12 * u, 21 * u, 12 * u, 21 * u)
                    close()
                }, color, style = stroke)
                drawCircle(color, radius = 2.5f * u, center = Offset(12 * u, 9.5f * u), style = stroke)
            }
            ConvoyIcon.Check -> drawPath(p(5f to 12.5f, 9.5f to 17f, 19f to 7.5f), color, style = stroke)
            ConvoyIcon.ChevronRight -> drawPath(p(9f to 6f, 15f to 12f, 9f to 18f), color, style = stroke)
            ConvoyIcon.ChevronLeft -> drawPath(p(15f to 6f, 9f to 12f, 15f to 18f), color, style = stroke)
            ConvoyIcon.Search -> {
                drawCircle(color, radius = 6.5f * u, center = Offset(11 * u, 11 * u), style = stroke)
                drawPath(p(16f to 16f, 20.5f to 20.5f), color, style = stroke)
            }
            ConvoyIcon.Shield -> drawPath(
                p(12f to 3f, 19f to 6f, 19f to 11f, 16.5f to 17f, 12f to 21f, 7.5f to 17f, 5f to 11f, 5f to 6f, 12f to 3f),
                color, style = stroke,
            )
            ConvoyIcon.Home -> drawPath(p(4f to 11f, 12f to 4f, 20f to 11f, 20f to 20f, 4f to 20f, 4f to 11f), color, style = stroke)
            ConvoyIcon.Building -> building(u, color, stroke)
            // 앞차 따라가기: 두 점을 잇는 선.
            ConvoyIcon.Link -> {
                drawCircle(color, radius = 2.5f * u, center = Offset(6 * u, 17 * u), style = stroke)
                drawCircle(color, radius = 2.5f * u, center = Offset(18 * u, 7 * u), style = stroke)
                drawPath(p(8f to 15.5f, 16f to 8.5f), color, style = stroke)
            }
            // 함께 출발: 세 점(그룹).
            ConvoyIcon.Group -> listOf(6f to 17f, 18f to 17f, 12f to 7f).forEach { (x, y) ->
                drawCircle(color, radius = 2.5f * u, center = Offset(x * u, y * u), style = stroke)
            }
        }
    }
}

private fun DrawScope.building(u: Float, color: Color, stroke: Stroke) {
    drawRoundRect(color, topLeft = Offset(5 * u, 4 * u), size = Size(14 * u, 16 * u), cornerRadius = CornerRadius(1.5f * u), style = stroke)
    listOf(9f to 8f, 13f to 8f, 9f to 12f, 13f to 12f).forEach { (x, y) ->
        drawLine(color, Offset(x * u, y * u), Offset((x + 2) * u, y * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}
