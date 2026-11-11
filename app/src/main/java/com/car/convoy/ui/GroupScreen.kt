package com.car.convoy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupMemberState

// 차량 화면은 160dpi라 1dp = 1px이다. 버튼 높이 80dp 이상, 글자 28sp 이상(R11-20, docs/sdk-findings.md).
private val SMALL = 28.sp
private val BODY = 30.sp
private val Page = Palette.Page
private val Card = Palette.Card
private val Ink = Palette.Ink
private val Soft = Palette.Soft
private val Accent = Palette.Accent
private val Danger = Palette.Danger
private val Demo = Palette.Demo
private val Done = Palette.Done
private val CardShape = RoundedCornerShape(16.dp)

/** SPEC R11-20: 그룹 주행 화면. [model]만 그리고, 누름은 콜백으로 넘긴다. */
@Composable
fun GroupScreen(
    model: GroupScreenModel,
    onChooseDestination: (Destination) -> Unit,
    onRetry: () -> Unit,
    onDemoDepart: (String) -> Unit,
    onDemoArrive: (String) -> Unit,
    headerExtra: @Composable () -> Unit = {},
    belowMembers: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit = {},
) {
    val left: @Composable () -> Unit = {
        DestinationCard(model)
        if (model.finished) FinishedBanner()
        model.members.forEach { MemberCard(it) }
        belowMembers()
    }
    val right: @Composable () -> Unit = {
        DestinationPicker(model, onChooseDestination)
        if (model.showRetry) RetryButton(onRetry)
        if (model.demoMemberIds.isNotEmpty()) DemoPanel(model, onDemoDepart, onDemoArrive)
    }
    ScreenFrame(
        header = { single ->
            HeaderRows(
                single,
                lead = { tabs() },
                trail = {
                    Chip(model.groupStatusLabel, if (model.finished) Done else Accent, BODY)
                    Spacer(Modifier.weight(1f))
                    headerExtra()
                    Text(model.arrivedSummary, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
                },
            )
        },
        // SPEC R11-20: 1열이면 왼쪽 열(상태·멤버) 다음 오른쪽 열(목적지 고르기 등).
        singleColumn = { left(); right() },
        twoColumns = {
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) { left() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { right() }
        },
    )
}

/**
 * SPEC R11-20, R2-5: 화면 루트의 maxWidth가 1,200dp 미만이면 1열로 쌓는다. 넘치면 손 스크롤(자동 스크롤 없음).
 * 그룹·친구 화면이 같이 쓴다.
 */
@Composable
internal fun ScreenFrame(
    header: @Composable (single: Boolean) -> Unit,
    singleColumn: @Composable () -> Unit,
    twoColumns: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    // BoxWithConstraints는 픽셀 제약이 바뀔 때만 다시 계산하므로, 밀도만 바뀌는 경우(configChanges로 액티비티를 유지할 때)도 key로 다시 잰다.
    key(LocalDensity.current.density) { BoxWithConstraints(Modifier.fillMaxSize().background(Page)) {
        val single = isSingleColumn(maxWidth.value)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (single) 24.dp else 36.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            header(single)
            if (single) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { singleColumn() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) { twoColumns() }
            }
        }
    } }
}

/** 머리줄: "Convoy"와 탭([lead]), 그 뒤 상태 표시([trail]). 1열이면 두 줄로 나눈다. */
@Composable
internal fun HeaderRows(
    single: Boolean,
    lead: @Composable () -> Unit,
    trail: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val first: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        Text("Convoy", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Accent)
        lead()
    }
    if (single) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp), content = first)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp), content = trail)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            first()
            trail()
        }
    }
}

@Composable
private fun DestinationCard(model: GroupScreenModel) {
    Column(
        Modifier.fillMaxWidth().background(Card, CardShape).padding(horizontal = 32.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("함께 가는 곳", fontSize = SMALL, color = Soft)
        if (model.destinationName == null) {
            Text("목적지를 골라 주세요", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Ink)
        } else {
            Text(model.destinationName, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Ink)
            model.destinationAddress?.let { Text(it, fontSize = SMALL, color = Soft) }
        }
    }
}

@Composable
private fun FinishedBanner() {
    Box(Modifier.fillMaxWidth().background(Palette.DoneFill, CardShape).padding(26.dp)) {
        Text("모두 도착했어요", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Done)
    }
}

@Composable
private fun MemberCard(row: MemberRow) {
    val color = memberStateColor(row.state)
    Row(
        modifier = Modifier.fillMaxWidth().background(Card, CardShape).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(64.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            Text(if (row.isLeader) "나" else row.name.takeLast(1), fontSize = BODY, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text(row.name, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
        if (row.isLeader) Chip("리더", Accent, SMALL)
        if (row.isVirtual) Chip("가상", Soft, SMALL)
        Spacer(Modifier.weight(1f))
        Chip(row.stateLabel, color, BODY)
    }
}

@Composable
private fun DestinationPicker(model: GroupScreenModel, onChoose: (Destination) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Card, CardShape).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("목적지 바꾸기", fontSize = BODY, fontWeight = FontWeight.Bold, color = Ink)
        if (!model.canChooseDestination && !model.finished) {
            Text("정차 중에 리더만 바꿀 수 있어요", fontSize = SMALL, color = Soft)
        }
        PRESET_DESTINATIONS.forEach { d ->
            val selected = d.name == model.destinationName
            Row(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .alpha(if (model.canChooseDestination) 1f else 0.45f)
                    .background(if (selected) Accent else Palette.ButtonFill, RoundedCornerShape(12.dp))
                    .border(2.dp, if (selected) Accent else Palette.ButtonLine, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = model.canChooseDestination) { onChoose(d) }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(d.name, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else Accent)
                    Text(
                        d.address, fontSize = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (selected) Color.White.copy(alpha = 0.85f) else Soft,
                    )
                }
                Text(
                    if (selected) "✓ 지금 목적지" else "선택",
                    fontSize = BODY, fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else Accent,
                )
            }
        }
    }
}

@Composable
private fun RetryButton(onRetry: () -> Unit) {
    OutlinedButton(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
        shape = RoundedCornerShape(12.dp),
    ) { Text("내 차 경로 다시 요청", fontSize = BODY, fontWeight = FontWeight.Bold, color = Danger) }
}

/** SPEC R11-18, R11-20: 가상 멤버의 출발·도착을 만드는 시연용 패널. 점선 테두리로 실제 기능과 구분한다. */
@Composable
private fun DemoPanel(model: GroupScreenModel, onDepart: (String) -> Unit, onArrive: (String) -> Unit) {
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
            Text("가상 일행 조작", fontSize = BODY, fontWeight = FontWeight.Bold, color = Demo)
        }
        model.members.filter { it.id in model.demoMemberIds }.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(m.name, fontSize = BODY, color = Ink, modifier = Modifier.weight(1f))
                DemoButton("출발") { onDepart(m.id) }
                DemoButton("도착") { onArrive(m.id) }
            }
        }
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(150.dp).heightIn(min = 80.dp),
        shape = RoundedCornerShape(12.dp),
    ) { Text(label, fontSize = BODY, color = Demo) }
}

@Composable
internal fun Chip(label: String, color: Color, size: TextUnit) {
    Text(
        label,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 6.dp),
    )
}
