package com.car.convoy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SMALL = 28.sp
private val BODY = 30.sp
private val CardShape = RoundedCornerShape(28.dp)
private val ButtonShape = RoundedCornerShape(18.dp)
// 남색 카드 대신 브랜드 파랑을 쓴다. 작은 칸은 흰 글자 대비를 위해 진한 브랜드(PaletteBrandContrastTest).
private val OnDarkSoft = Palette.OnBrandSoft
private val DarkTile = Palette.BrandDeep

/** SPEC R12-4, 작업 25: 앞차 따라가기 화면. [model]만 그리고, 누름은 콜백으로 넘긴다. */
@Composable
fun FollowScreen(
    model: FollowScreenModel,
    onHome: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDemoMove: (Double) -> Unit,
    headerExtra: @Composable () -> Unit = {},
) {
    val main: @Composable () -> Unit = {
        Column(
            Modifier.fillMaxWidth().background(Palette.Brand, CardShape).padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text("내 내비가 앞차 위치로 안내하고 있어요", fontSize = SMALL, color = OnDarkSoft)
            Text(model.distanceLabel, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat("앞차 위치 갱신", model.updatedLabel, Modifier.weight(1f))
                Stat("경로 다시 요청", model.rerouteLabel, Modifier.weight(1f))
                Stat("마지막 요청 뒤 이동", model.movedLabel, Modifier.weight(1f))
            }
        }
        Box(
            Modifier.fillMaxWidth().background(Palette.Card, CardShape).border(1.dp, Color(0xFFE3E6EA), CardShape).padding(26.dp),
        ) {
            Text("앞차가 마지막 요청 지점에서 200m 이상 움직이면 경로를 다시 잡아요", fontSize = SMALL, color = Palette.Ink)
        }
        if (model.showRetry) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 96.dp).border(2.dp, Palette.Danger, ButtonShape).clip(ButtonShape).clickable(onClick = onRetry),
                contentAlignment = Alignment.Center,
            ) { Text("내 차 경로 다시 요청", fontSize = BODY, fontWeight = FontWeight.Bold, color = Palette.Danger) }
        }
    }
    val demo: @Composable () -> Unit = { DemoMovePanel(onDemoMove) }
    ScreenFrame(
        header = { single ->
            HeaderRows(
                single,
                wordmark = false,
                lead = {
                    Text(model.title, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                },
                trail = {
                    Spacer(Modifier.weight(1f))
                    headerExtra()
                    DriveButton("홈으로", Palette.Ink, Color(0xFFF1F3F6), null, onHome)
                    // R3-3, R12-4: [끊기]는 주행 중에도 된다.
                    DriveButton("끊기", Palette.Danger, Palette.Card, Color(0xFFE8B4AE), onStop)
                },
            )
        },
        singleColumn = { main(); demo() },
        twoColumns = {
            Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(20.dp)) { main() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) { demo() }
        },
    )
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(DarkTile, RoundedCornerShape(20.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = SMALL, color = OnDarkSoft)
        Text(value, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
private fun DriveButton(label: String, fg: Color, bg: Color, line: Color?, onClick: () -> Unit) {
    var m = Modifier.widthIn(min = 160.dp).heightIn(min = 80.dp).background(bg, ButtonShape)
    if (line != null) m = m.border(2.dp, line, ButtonShape)
    Box(m.clip(ButtonShape).clickable(onClick = onClick).padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = SMALL, fontWeight = FontWeight.Bold, color = fg)
    }
}

/** R12-3: 서버가 없어 앞차 위치를 버튼으로 옮긴다. 내 내비 경로 요청은 실제 SDK 호출이다. */
@Composable
private fun DemoMovePanel(onMove: (Double) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFFFFFBF4), RoundedCornerShape(24.dp))
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFB98A45), cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                )
            }
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Chip("시연용", Palette.Demo, SMALL)
            Text("앞차 위치 옮기기", fontSize = SMALL, color = Palette.Ink)
        }
        Text("서버가 없어 앞차 위치를 버튼으로 옮겨요. 내 내비 경로 요청은 실제로 나가요.", fontSize = SMALL, color = Palette.Soft)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(100.0, 300.0).forEach { m ->
                Box(
                    Modifier.weight(1f).heightIn(min = 80.dp).background(Palette.Card, ButtonShape)
                        .border(2.dp, Color(0xFFB98A45), ButtonShape).clip(ButtonShape).clickable { onMove(m) },
                    contentAlignment = Alignment.Center,
                ) { Text("${m.toInt()}m 이동", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Palette.Demo) }
            }
        }
    }
}
