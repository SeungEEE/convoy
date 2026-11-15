package com.car.convoy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 차량 화면 기준: 버튼 80dp 이상, 글자 28sp 이상(R11-20). 시안: Convoy 그룹 흐름 시안 2-B.
private val SMALL = 28.sp
private val BODY = 30.sp
private val CardShape = RoundedCornerShape(32.dp)
private val ButtonShape = RoundedCornerShape(20.dp)
private val OnDark = Color(0xFFC9D0DA)
private val Dashed = Color(0xFF5B8DEF)

/** SPEC R12-4, 작업 25: 앞차 따라가기 화면(시안). [model]만 그리고, 누름은 콜백으로 넘긴다. */
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
        LeaderCard(model)
        Row(
            Modifier.fillMaxWidth().background(Palette.Card, RoundedCornerShape(28.dp)).border(1.dp, Palette.CardLine, RoundedCornerShape(28.dp))
                .padding(horizontal = 30.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.size(64.dp).background(Palette.MemberFill, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Icon24(ConvoyIcon.Link, Palette.MemberText, 34.dp, 2.2f)
            }
            Text("앞차가 마지막 요청 지점에서 200m 이상 움직이면 경로를 다시 잡아요. 자주 바뀌지 않게 하려는 기준이에요.",
                fontSize = SMALL, color = Color(0xFF3B4552), lineHeight = 40.sp)
        }
        if (model.showRetry) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 96.dp).background(Palette.Card, ButtonShape).border(2.dp, Palette.Danger, ButtonShape)
                    .clip(ButtonShape).clickable(onClick = onRetry),
                contentAlignment = Alignment.Center,
            ) { Text("내 차 경로 다시 요청", fontSize = BODY, fontWeight = FontWeight.Bold, color = Palette.Danger) }
        }
    }
    val side: @Composable () -> Unit = {
        MembersCard(model.leaderName)
        DemoMovePanel(onDemoMove)
        Text("[끊기]는 주행 중에도 눌러요. 끊으면 따라가던 경로를 취소해요.", fontSize = SMALL, color = Palette.Soft,
            lineHeight = 40.sp, modifier = Modifier.padding(horizontal = 6.dp))
    }
    ScreenFrame(
        header = { single -> FollowHeader(model, single, onHome, onStop, headerExtra) },
        singleColumn = { main(); side() },
        twoColumns = {
            Column(Modifier.weight(1.35f), verticalArrangement = Arrangement.spacedBy(24.dp)) { main() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) { side() }
        },
    )
}

/** 시안 머리줄: [‹](홈으로), "앞차 따라가기 / 민수 차를 따라가는 중", 큰 [✕ 끊기]. 둘 다 주행 중에도 된다(R3-3, R10). */
@Composable
private fun FollowHeader(model: FollowScreenModel, single: Boolean, onHome: () -> Unit, onStop: () -> Unit, extra: @Composable () -> Unit) {
    val title: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Box(
                Modifier.size(80.dp).background(Palette.Card, RoundedCornerShape(24.dp)).border(1.dp, Palette.CardLine, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)).clickable(onClick = onHome),
                contentAlignment = Alignment.Center,
            ) { Icon24(ConvoyIcon.ChevronLeft, Palette.Ink, 36.dp, 2.4f) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("앞차 따라가기", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Palette.Soft)
                Text(model.title, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
            }
        }
    }
    val actions: @Composable () -> Unit = {
        extra()
        Row(
            Modifier.heightIn(min = 88.dp).background(Palette.Card, RoundedCornerShape(24.dp)).border(2.dp, Color(0xFFE8B4AE), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)).clickable(onClick = onStop).padding(horizontal = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Canvas(Modifier.size(28.dp)) {
                val w = 2.6.dp.toPx()
                drawLine(Palette.Danger, Offset(4.dp.toPx(), 4.dp.toPx()), Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), w, StrokeCap.Round)
                drawLine(Palette.Danger, Offset(size.width - 4.dp.toPx(), 4.dp.toPx()), Offset(4.dp.toPx(), size.height - 4.dp.toPx()), w, StrokeCap.Round)
            }
            Text("끊기", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Danger)
        }
    }
    if (single) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            title()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { actions() }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            title()
            Spacer(Modifier.weight(1f))
            actions()
        }
    }
}

/** 시안의 짙은 카드: "나 → 앞차" 그림과 숫자 세 칸. */
@Composable
private fun LeaderCard(model: FollowScreenModel) {
    Column(
        Modifier.fillMaxWidth().background(Palette.Night, CardShape).padding(horizontal = 44.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Text("내 내비가 앞차 위치로 안내하고 있어요", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Palette.OnNightSoft)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(96.dp).background(Color(0xFF2B3647), CircleShape), contentAlignment = Alignment.Center) {
                    Text("나", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Text("내 차", fontSize = SMALL, color = OnDark)
            }
            Column(Modifier.weight(1f).padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(model.distanceLabel, fontSize = BODY, fontWeight = FontWeight.Bold, color = Color.White)
                Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                    drawLine(
                        Dashed, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height,
                        cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(22.dp.toPx(), 14.dp.toPx())),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(112.dp).drawBehind { drawCircle(Dashed.copy(alpha = 0.35f), radius = size.minDimension / 2 + 8.dp.toPx()) }
                        .background(Palette.FamilyFill, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(model.leaderName.take(1), fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Palette.FamilyText) }
                Text("앞차 · ${model.leaderName}", fontSize = SMALL, color = OnDark)
            }
        }
        // 문구가 두 줄로 넘어가도 세 칸 높이를 맞춘다.
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("앞차 위치 갱신", model.updatedLabel, Modifier.weight(1f))
            Stat("경로 다시 요청", model.rerouteLabel, Modifier.weight(1f))
            Stat("요청 뒤 이동", model.movedLabel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.fillMaxHeight().background(Palette.NightTile, RoundedCornerShape(22.dp)).padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = SMALL, color = Palette.OnNightSoft)
        Text(value, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

/** 시안의 "함께 가는 멤버". 앞차의 실제 주행 상태는 앱이 모르므로 역할만 보여 준다. */
@Composable
private fun MembersCard(leaderName: String) {
    Column(
        Modifier.fillMaxWidth().background(Palette.Card, CardShape).border(1.dp, Palette.CardLine, CardShape).padding(horizontal = 32.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("함께 가는 멤버", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, modifier = Modifier.padding(bottom = 10.dp))
        MemberLine(leaderName.take(1), leaderName, Palette.FamilyFill, Palette.FamilyText, "앞차", Palette.Ink, Color(0xFFE7E9ED))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEF0F3)))
        MemberLine("나", "내 차", Palette.MemberFill, Palette.MemberText, "따라가는 중", Palette.MemberText, Palette.MemberFill)
    }
}

@Composable
private fun MemberLine(initial: String, name: String, avatarBg: Color, avatarFg: Color, tag: String, tagFg: Color, tagBg: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(64.dp).background(avatarBg, CircleShape), contentAlignment = Alignment.Center) {
            Text(initial, fontSize = SMALL, fontWeight = FontWeight.ExtraBold, color = avatarFg)
        }
        Text(name, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, modifier = Modifier.weight(1f))
        Text(tag, fontSize = SMALL, fontWeight = FontWeight.ExtraBold, color = tagFg,
            modifier = Modifier.background(tagBg, RoundedCornerShape(999.dp)).padding(horizontal = 18.dp, vertical = 8.dp))
    }
}

/** R12-3: 서버가 없어 앞차 위치를 버튼으로 옮긴다. 내 내비 경로 요청은 실제 SDK 호출이다(시안). */
@Composable
private fun DemoMovePanel(onMove: (Double) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFFFFFBF4), RoundedCornerShape(28.dp))
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFB98A45), cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Chip("시연용", Palette.Demo, SMALL)
            Text("앞차 위치 옮기기", fontSize = SMALL, color = Color(0xFF3B4552))
        }
        Text("서버가 없어 앞차 위치를 버튼으로 옮겨요. 내 내비 경로 요청은 실제로 나가요.", fontSize = SMALL, color = Palette.Soft, lineHeight = 40.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(100.0, 300.0).forEach { m ->
                Box(
                    Modifier.weight(1f).heightIn(min = 84.dp).background(Palette.Card, ButtonShape)
                        .border(2.dp, Color(0xFFB98A45), ButtonShape).clip(ButtonShape).clickable { onMove(m) },
                    contentAlignment = Alignment.Center,
                ) { Text("${m.toInt()}m 이동", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Palette.Demo) }
            }
        }
    }
}
