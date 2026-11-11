package com.car.convoy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.convoy.domain.UserStatus

// 차량 화면 기준: 버튼 높이 80dp 이상, 글자 28sp 이상(R11-20). 시안: Convoy 그룹 흐름 시안(1. 홈).
private val SMALL = 28.sp
private val BODY = 30.sp
private val TITLE = 34.sp
private val Card = Palette.Card
private val Ink = Palette.Ink
private val Soft = Palette.Soft
private val Accent = Palette.Accent
private val Demo = Palette.Demo
private val CardShape = RoundedCornerShape(32.dp)
private val ButtonShape = RoundedCornerShape(20.dp)

/** 시안의 사람별 아바타 색(연한 바탕, 진한 글자). 대비는 4.5:1 이상. */
private val AvatarColors = listOf(
    Color(0xFFE3F4EA) to Color(0xFF0F5A36),
    Color(0xFFFDEFE3) to Color(0xFF8A4B00),
    Color(0xFFEDE7FB) to Color(0xFF4A2F8F),
    Color(0xFFFDECEA) to Color(0xFF8F1D14),
    Color(0xFFE3EAFD) to Color(0xFF1A3FA8),
)

/** SPEC R2-5, R4-5, R9-5: 홈(시안). [model]만 그리고, 누름은 콜백으로 넘긴다. */
@Composable
fun HomeScreen(
    model: HomeScreenModel,
    onMyStatus: (UserStatus) -> Unit,
    onInvite: (String) -> Unit,
    onStartDestination: () -> Unit,
    onResume: () -> Unit,
    onDisband: () -> Unit,
    onDemoAccept: () -> Unit,
    onDemoDecline: () -> Unit,
    onDesignateFamily: (String) -> Unit = {},
    onReleaseFamily: () -> Unit = {},
    onDemoFamilyAccept: () -> Unit = {},
    onDemoFamilyDecline: () -> Unit = {},
    onFollow: (String) -> Unit = {},
    onDemoFollowAccept: () -> Unit = {},
    onDemoFollowDecline: () -> Unit = {},
    headerExtra: @Composable () -> Unit = {},
    belowFamily: @Composable () -> Unit = {},
) {
    // 사람마다 같은 아바타 색을 쓴다(연결된 사람과 함께 출발 칩).
    val avatar: (String) -> Pair<Color, Color> = { id ->
        val i = model.people.indexOfFirst { it.id == id }.coerceAtLeast(0)
        AvatarColors[i % AvatarColors.size]
    }
    // R8-10: 사고 알림 카드는 가족 카드 바로 아래에 붙는다(가족은 그룹 주행과 무관, R9-1).
    val familyCard: @Composable () -> Unit = { FamilyCardView(model.familyCard, onReleaseFamily); belowFamily() }
    val departure: @Composable () -> Unit = { DepartureCard(model, avatar, onStartDestination, onResume, onDisband, onFollow) }
    val demo: @Composable () -> Unit = {
        model.pendingFamily?.let { DemoPanel("${it.name}님이 가족 요청을 받았어요", onDemoFamilyAccept, onDemoFamilyDecline) }
        model.pendingInvite?.let { DemoPanel("${it.name}님이 그룹 초대를 받았어요", onDemoAccept, onDemoDecline) }
        model.pendingFollow?.let { DemoPanel("${it.name}님이 따라가기 요청을 받았어요", onDemoFollowAccept, onDemoFollowDecline) }
    }
    val people: @Composable () -> Unit = { PeopleCard(model, avatar, onInvite, onDesignateFamily) }
    val note: @Composable () -> Unit = {
        Text(
            "가족은 항상 연결되고, 그룹은 함께 이동할 때만 꾸려요. 설정은 정차 중에만 돼요.",
            fontSize = SMALL, color = Soft, lineHeight = 40.sp, modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
    ScreenFrame(
        header = { single ->
            HeaderRows(
                single,
                lead = {},
                trail = {
                    Spacer(Modifier.weight(1f))
                    headerExtra()
                    Text("내 상태", fontSize = SMALL, color = Soft)
                    MyStatusSegment(model.myStatus, onMyStatus)
                },
            )
        },
        // R4-5: 1열 순서는 가족, 함께 출발, 시연용 패널, 연결된 사람. 가족 카드는 항상 있어 맨 앞에 둔다(R9-5).
        singleColumn = {
            familyCard()
            homeSingleColumnOrder(model).forEach { slot ->
                when (slot) {
                    HomeSlot.DEPARTURE -> departure()
                    HomeSlot.DEMO -> demo()
                    HomeSlot.PEOPLE -> people()
                }
            }
            note()
        },
        // 시안: 위에 가족·함께 출발 카드를 나란히, 아래에 연결된 사람과 시연 패널.
        twoColumns = {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) { familyCard() }
                    Column(Modifier.weight(1.3f)) { departure() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column(Modifier.weight(1.6f)) { people() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        demo()
                        note()
                    }
                }
            }
        },
    )
}

@Composable
private fun CardColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Card, CardShape).border(1.dp, Palette.CardLine, CardShape).padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) { content() }
}

@Composable
private fun CardTitle(icon: ConvoyIcon?, iconColor: Color, title: String, tag: String, tagColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Icon24(icon, iconColor, 34.dp, 2.2f)
        Text(title, fontSize = TITLE, fontWeight = FontWeight.ExtraBold, color = Ink)
        Chip(tag, tagColor, SMALL)
    }
}

/** R9-5: 가족 카드(시안). 항상 연결(그룹과 무관). 해제·지정 취소는 주행 중에도 된다(R10). */
@Composable
private fun FamilyCardView(card: FamilyCard, onRelease: () -> Unit) {
    CardColumn {
        CardTitle(ConvoyIcon.Shield, Palette.FamilyText, "가족", "항상 연결", Palette.FamilyText)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            if (card.name != null) {
                Box(Modifier.size(96.dp).background(Palette.FamilyFill, CircleShape), contentAlignment = Alignment.Center) {
                    Text(card.name.take(1), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Palette.FamilyText)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(card.name, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(
                        when (card.state) {
                            FamilyCardState.ACTIVE -> "사고가 의심되면 ${card.name}님에게 알려요"
                            else -> card.message
                        },
                        fontSize = SMALL, color = Soft,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(card.message, fontSize = BODY, fontWeight = FontWeight.Bold, color = Ink)
                    Text("연결된 사람에서 [가족으로 설정]을 눌러요", fontSize = SMALL, color = Soft)
                }
            }
        }
        if (card.showRelease) SoftButton("가족 해제", enabled = true, onClick = onRelease)
        if (card.showCancel) SoftButton("지정 취소", enabled = true, onClick = onRelease)
    }
}

/** R2-1: 내 상태. 주행 중에도 바꿀 수 있다(R10). */
@Composable
private fun MyStatusSegment(current: UserStatus, onChoose: (UserStatus) -> Unit) {
    Row(
        Modifier.background(Color(0xFFE7E9ED), RoundedCornerShape(999.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        UserStatus.entries.forEach { s ->
            val on = s == current
            Row(
                Modifier.heightIn(min = 80.dp)
                    .background(if (on) Card else Color.Transparent, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onChoose(s) }
                    .padding(horizontal = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (on) Box(Modifier.size(14.dp).background(userStatusColor(s), CircleShape))
                Text(userStatusLabel(s), fontSize = BODY, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, color = if (on) Ink else Soft)
            }
        }
    }
}

/**
 * R4-5, R12-1: 함께 출발(시안). 멤버 칩과 [목적지로 함께]·[앞차 따라가기] 두 버튼.
 * [앞차 따라가기]를 누르면 따라갈 멤버 버튼이 펼쳐진다(업무 중이면 잠김). 진행 중이면 [진행 중인 주행으로].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepartureCard(
    model: HomeScreenModel,
    avatar: (String) -> Pair<Color, Color>,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onDisband: () -> Unit,
    onFollow: (String) -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }
    CardColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle(ConvoyIcon.Group, Accent, "함께 출발", "이번 주행 그룹", Accent)
            Spacer(Modifier.weight(1f))
            if (model.departure.size > 1) SoftButton("그룹 해산", enabled = model.canDisband, onClick = onDisband)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.departure.forEach { MemberChip(it, avatar(it.id)) }
        }
        if (model.departure.size == 1) Text("아래 연결된 사람을 초대해 그룹을 꾸려요", fontSize = SMALL, color = Soft)
        when {
            model.showResume -> BigAction(ConvoyIcon.Pin, "진행 중인 주행으로", model.resumeLabel, Palette.Brand, Palette.OnBrandSoft, true, Modifier.fillMaxWidth(), onResume)
            // R12-1: 따라가기 요청을 보내고 수락을 기다리는 중.
            model.followWaiting != null -> Box(
                Modifier.fillMaxWidth().heightIn(min = 112.dp).background(Palette.PendingFill, ButtonShape).padding(horizontal = 28.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(model.followWaiting, fontSize = BODY, fontWeight = FontWeight.Bold, color = Palette.PendingText) }
            else -> {
                val canFollowSomeone = model.followChoices.any { it.canFollow }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BigAction(ConvoyIcon.Pin, "목적지로 함께", "모두의 내비가 함께 바뀌어요", Palette.Brand, Palette.OnBrandSoft,
                        model.canStartDestination, Modifier.weight(1f), onStart)
                    BigAction(ConvoyIcon.Link, "앞차 따라가기", "목적지 없이 멤버를 따라가요", Palette.Night, Palette.OnNightSoft,
                        canFollowSomeone, Modifier.weight(1f)) { choosing = !choosing }
                }
                if (choosing && canFollowSomeone) {
                    Text("누구를 따라갈까요?", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Ink)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        model.followChoices.forEach { c ->
                            OutlineButton("${c.name} 따라가기", enabled = c.canFollow, width = 240.dp) { choosing = false; onFollow(c.id) }
                        }
                    }
                    model.followChoices.firstNotNullOfOrNull { c -> c.note?.let { "${c.name}: $it" } }?.let { Text(it, fontSize = SMALL, color = Soft) }
                }
                if (!model.canStartDestination && model.departure.any { !it.isLeader && !it.pending }) {
                    Text("정차 중에만 출발할 수 있어요", fontSize = SMALL, color = Soft)
                }
            }
        }
    }
}

/** 시안의 큰 출발 버튼(아이콘 + 제목 + 설명). 비활성은 회색 바탕과 보조 글자색(대비 유지). */
@Composable
private fun BigAction(
    icon: ConvoyIcon, title: String, sub: String, bg: Color, subColor: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val fg = if (enabled) Color.White else Soft
    Row(
        modifier.heightIn(min = 124.dp)
            .background(if (enabled) bg else Palette.DisabledFill, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Icon24(icon, fg, 44.dp, 2.2f)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = TITLE, fontWeight = FontWeight.ExtraBold, color = fg)
            Text(sub, fontSize = SMALL, color = if (enabled) subColor else Soft, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 36.sp)
        }
    }
}

@Composable
private fun MemberChip(chip: DepartureChip, colors: Pair<Color, Color>) {
    val shape = RoundedCornerShape(999.dp)
    val base = Modifier.heightIn(min = 64.dp)
    val styled = when {
        chip.isLeader -> base.background(Palette.MemberFill, shape)
        chip.pending -> base.drawBehind {
            drawRoundRect(
                color = Color(0xFFB5BDC8), cornerRadius = CornerRadius(size.height / 2),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            )
        }
        else -> base.background(Palette.ButtonFill, shape)
    }
    Row(
        styled.padding(start = 8.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val (bg, fg) = if (chip.isLeader) Card to Palette.MemberText else if (chip.pending) Palette.ButtonFill to Soft else colors
        Box(Modifier.size(48.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
            Text(chip.name.take(1), fontSize = SMALL, fontWeight = FontWeight.ExtraBold, color = fg)
        }
        Text(
            when {
                chip.isLeader -> "리더"
                chip.pending -> "${chip.name} · 수락 대기"
                else -> chip.name
            },
            fontSize = SMALL, fontWeight = FontWeight.Bold,
            color = if (chip.isLeader) Palette.MemberText else if (chip.pending) Soft else Ink,
        )
    }
}

/** R4-5 연결된 사람(시안): 오프라인은 보이지 않는다(R2-2). 가족은 상태 줄에 "· 가족"으로 붙는다. */
@Composable
private fun PeopleCard(
    model: HomeScreenModel,
    avatar: (String) -> Pair<Color, Color>,
    onInvite: (String) -> Unit,
    onDesignateFamily: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Card, CardShape).border(1.dp, Palette.CardLine, CardShape)
            .padding(horizontal = 36.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("연결된 사람", fontSize = TITLE, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text("오프라인인 사람은 보이지 않아요", fontSize = SMALL, color = Soft)
        }
        model.people.forEachIndexed { i, p ->
            PersonLine(p, avatar(p.id), onInvite, onDesignateFamily, last = i == model.people.lastIndex)
        }
    }
}

@Composable
private fun PersonLine(
    p: PersonRow,
    colors: Pair<Color, Color>,
    onInvite: (String) -> Unit,
    onDesignateFamily: (String) -> Unit,
    last: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(68.dp).background(colors.first, CircleShape), contentAlignment = Alignment.Center) {
            Text(p.name.take(1), fontSize = SMALL, fontWeight = FontWeight.ExtraBold, color = colors.second)
        }
        Column(Modifier.weight(1f)) {
            Text(p.name, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(14.dp).background(userStatusColor(p.status), CircleShape))
                Text(listOfNotNull(p.statusLabel, p.familyTag).joinToString(" · "), fontSize = SMALL, color = Soft)
            }
        }
        // R9-5: 가족(또는 수락 대기)이면 상태 줄에 표시하고, 아니면 [가족으로 설정].
        if (p.familyTag == null) OutlineButton("가족으로 설정", enabled = p.canDesignateFamily, width = 240.dp) { onDesignateFamily(p.id) }
        when (p.groupLabel) {
            null -> OutlineButton("그룹에 초대", enabled = p.canInvite) { onInvite(p.id) }
            "수락 대기" -> StatusTag(p.groupLabel, Palette.PendingText, Palette.PendingFill)
            else -> StatusTag(p.groupLabel, Palette.MemberText, Palette.MemberFill)
        }
    }
    if (!last) Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(Color(0xFFEEF0F3)))
}

@Composable
private fun StatusTag(label: String, fg: Color, bg: Color, width: androidx.compose.ui.unit.Dp = 260.dp) {
    Box(Modifier.width(width).heightIn(min = 80.dp).background(bg, ButtonShape), contentAlignment = Alignment.Center) {
        Text(label, fontSize = SMALL, fontWeight = FontWeight.Bold, color = fg)
    }
}

/** 테두리 버튼. 비활성은 회색 바탕과 보조 글자색. */
@Composable
private fun OutlineButton(label: String, enabled: Boolean, width: androidx.compose.ui.unit.Dp = 260.dp, onClick: () -> Unit) {
    Box(
        Modifier.width(width).heightIn(min = 80.dp)
            .background(if (enabled) Card else Palette.DisabledFill, ButtonShape)
            .border(2.dp, if (enabled) Palette.ButtonLine else Palette.DisabledFill, ButtonShape)
            .clip(ButtonShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = SMALL, fontWeight = FontWeight.Bold, color = if (enabled) Accent else Soft) }
}

@Composable
private fun SoftButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.widthIn(min = 180.dp).heightIn(min = 80.dp)
            .background(if (enabled) Palette.ButtonFill else Palette.DisabledFill, ButtonShape)
            .clip(ButtonShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = SMALL, fontWeight = FontWeight.Bold, color = if (enabled) Color(0xFF3B4552) else Soft) }
}

/** R4-5, R9-5, R12-1: 가상 인물의 초대·가족·따라가기 요청 응답(시안). 점선 테두리로 실제 기능과 구분한다. */
@Composable
private fun DemoPanel(message: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFFFFFBF4), RoundedCornerShape(28.dp))
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFB98A45), cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Chip("시연용", Demo, SMALL)
            Text("가상 인물 응답", fontSize = SMALL, color = Color(0xFF3B4552))
        }
        Text(message, fontSize = BODY, fontWeight = FontWeight.Bold, color = Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DemoButton("수락", Modifier.weight(1f), onAccept)
            DemoButton("거절", Modifier.weight(1f), onDecline)
        }
    }
}

@Composable
private fun DemoButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 84.dp)
            .background(Card, ButtonShape)
            .border(2.dp, Color(0xFFB98A45), ButtonShape)
            .clip(ButtonShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = BODY, fontWeight = FontWeight.Bold, color = Demo) }
}
