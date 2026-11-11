package com.car.convoy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.convoy.domain.UserStatus

// 그룹 주행 화면과 같은 기준: 버튼 높이 80dp 이상, 글자 28sp 이상(R11-20).
private val SMALL = 28.sp
private val BODY = 30.sp
private val Card = Palette.Card
private val Ink = Palette.Ink
private val Soft = Palette.Soft
private val Accent = Palette.Accent
private val Danger = Palette.Danger
private val Demo = Palette.Demo
private val CardShape = RoundedCornerShape(16.dp)
private fun statusColor(status: UserStatus): Color = userStatusColor(status)

/** SPEC R2-5, 작업 22: 친구 목록 화면. [model]만 그리고, 누름은 콜백으로 넘긴다. */
@Composable
fun FriendScreen(
    model: FriendScreenModel,
    onMyStatus: (UserStatus) -> Unit,
    onRequestFollow: (String) -> Unit,
    onCancelFollow: () -> Unit,
    onDemoAccept: () -> Unit,
    onDemoDecline: () -> Unit,
    headerExtra: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit = {},
) {
    val empty: @Composable () -> Unit = {
        if (model.rows.isEmpty()) Text("지금 볼 수 있는 친구가 없어요", fontSize = BODY, color = Soft)
    }
    ScreenFrame(
        header = { single ->
            HeaderRows(single, lead = { tabs() }, trail = { Spacer(Modifier.weight(1f)); headerExtra() })
        },
        // SPEC R2-5: 1열이면 주행 중에도 하는 조작(진행 중 친구 줄, 시연 패널, 내 상태)을 먼저 둔다.
        singleColumn = {
            friendSingleColumnOrder(model).forEach { slot ->
                when (slot) {
                    is FriendSlot.FriendItem -> FriendCard(slot.row, onRequestFollow, onCancelFollow)
                    FriendSlot.DemoPanel -> model.pendingRequest?.let { DemoFollowPanel(it, onDemoAccept, onDemoDecline) }
                    FriendSlot.MyStatus -> MyStatusCard(model.myStatus, onMyStatus)
                }
            }
            empty()
        },
        twoColumns = {
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                empty()
                model.rows.forEach { FriendCard(it, onRequestFollow, onCancelFollow) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MyStatusCard(model.myStatus, onMyStatus)
                model.pendingRequest?.let { DemoFollowPanel(it, onDemoAccept, onDemoDecline) }
            }
        },
    )
}

@Composable
private fun FriendCard(row: FriendRow, onRequest: (String) -> Unit, onCancel: () -> Unit) {
    val color = statusColor(row.status)
    Column(
        Modifier.fillMaxWidth().background(Card, CardShape).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(64.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Text(row.name.take(1), fontSize = BODY, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(row.name, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
            Chip(row.statusLabel, color, SMALL)
            Spacer(Modifier.weight(1f))
            when {
                row.showCancel -> CancelButton(onCancel)
                row.followLabel == null -> RequestButton(row.canRequest) { onRequest(row.id) }
            }
        }
        row.followLabel?.let { Chip(it, Accent, BODY) }
        row.note?.let { Text(it, fontSize = SMALL, color = Soft) }
    }
}

@Composable
private fun RequestButton(enabled: Boolean, onClick: () -> Unit) {
    // 그룹 화면 목적지 버튼과 같은 순서: alpha → background(shape) → border → clip → clickable.
    Box(
        Modifier.widthIn(min = 260.dp).heightIn(min = 88.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .background(Palette.ButtonFill, RoundedCornerShape(12.dp))
            .border(2.dp, Palette.ButtonLine, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) { Text("따라가기 요청", fontSize = BODY, fontWeight = FontWeight.Bold, color = Accent) }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 200.dp).heightIn(min = 88.dp),
        shape = RoundedCornerShape(12.dp),
    ) { Text("끊기", fontSize = BODY, fontWeight = FontWeight.Bold, color = Danger) }
}

@Composable
private fun MyStatusCard(current: UserStatus, onChoose: (UserStatus) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Card, CardShape).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("내 상태", fontSize = BODY, fontWeight = FontWeight.Bold, color = Ink)
        UserStatus.entries.forEach { s ->
            val selected = s == current
            val color = statusColor(s)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 88.dp)
                    .background(if (selected) color else Color.Transparent, RoundedCornerShape(12.dp))
                    .border(2.dp, color, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onChoose(s) }
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    userStatusLabel(s), fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else color, modifier = Modifier.weight(1f),
                )
                if (selected) Text("✓", fontSize = BODY, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/** SPEC R2-5: 가상 친구의 응답을 만드는 시연용 패널. 그룹 화면처럼 점선 테두리로 구분한다. */
@Composable
private fun DemoFollowPanel(pending: PendingRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = Demo,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                )
            }
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Chip("시연용", Demo, SMALL)
            Text("가상 친구 응답", fontSize = BODY, fontWeight = FontWeight.Bold, color = Demo)
        }
        Text("${pending.name}님이 따라가기 요청을 받았어요", fontSize = SMALL, color = Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DemoButton("수락", onAccept)
            DemoButton("거절", onDecline)
        }
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(180.dp).heightIn(min = 80.dp),
        shape = RoundedCornerShape(12.dp),
    ) { Text(label, fontSize = BODY, color = Demo) }
}

/** SPEC R2-5: [친구] [그룹 주행] 탭. 각 화면 머리줄의 "Convoy" 옆에 놓는다(Pleos 가이드 Tabs의 Box 형). */
@Composable
fun AppTabs(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTab.entries.forEach { tab ->
            val on = tab == selected
            Box(
                Modifier.widthIn(min = 200.dp).heightIn(min = 80.dp)
                    .background(if (on) Accent else Card, RoundedCornerShape(12.dp))
                    .border(2.dp, if (on) Accent else Palette.ButtonLine, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) { Text(tab.label, fontSize = BODY, fontWeight = FontWeight.Bold, color = if (on) Color.White else Accent) }
        }
    }
}
