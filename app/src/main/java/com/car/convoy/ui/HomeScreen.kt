package com.car.convoy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
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
import com.car.convoy.domain.HomeTab
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

/** SPEC R14-12: 지도 위 카드와 버튼의 가장자리 여백, 넓은 창의 "함께 출발" 카드 너비. */
private val MAP_INSET = 20.dp
private val DEPARTURE_CARD_WIDTH = 1100.dp

/** SPEC R4-5, R14-12: 좁은 창에서 지도 높이(R14-16과 같다). */
private val NARROW_MAP_HEIGHT = 480.dp

/**
 * SPEC R2-5, R4-5, R9-5, R14-12: 첫 화면(시안 01~04). 머리줄에 탭 [친구 지도] [그룹] [가족] [설정]과 가족 표시를 두고,
 * [tab]의 내용을 그린다. [model]만 그리고, 누름은 콜백으로 넘긴다. 탭 바꾸기는 화면 이동이라 R10 제한이 없다.
 */
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
    belowFamily: @Composable () -> Unit = {},
    // ---- SPEC R2-5, R14-12, 작업 40: 탭과 친구 지도 ----
    tab: HomeTab = HomeTab.FRIEND_MAP,
    onTab: (HomeTab) -> Unit = {},
    /** 친구 지도. 지도를 그릴 수 없으면 지도 자리에 이유를 보여 준다(GroupMap이 맡는다). */
    friendMap: @Composable (Modifier) -> Unit = {},
    canLocate: Boolean = false,
    onLocate: () -> Unit = {},
) {
    // 사람마다 같은 아바타 색을 쓴다(친구 목록과 멤버 칩).
    val avatar: (String) -> Pair<Color, Color> = { id ->
        val i = model.people.indexOfFirst { it.id == id }.coerceAtLeast(0)
        Palette.AvatarColors[i % Palette.AvatarColors.size]
    }
    val header: @Composable (Boolean) -> Unit = { single ->
        HeaderRows(
            single,
            lead = { HomeTabs(tab, onTab) },
            trail = {
                if (!single) Spacer(Modifier.weight(1f))
                FamilyChip(model.familyHeader)
            },
        )
    }
    when (tab) {
        HomeTab.FRIEND_MAP -> FriendMapTab(model, header, avatar, friendMap, canLocate, onLocate, onStartDestination, onResume, onFollow) {
            model.pendingFollow?.let { DemoPanel("${it.name}님이 따라가기 요청을 받았어요", onDemoFollowAccept, onDemoFollowDecline) }
        }
        // R4-5 [그룹]: 1열 순서는 멤버, 친구 목록, 시연용 패널(맨 아래).
        HomeTab.GROUP -> {
            val members: @Composable () -> Unit = { GroupMembersCard(model, avatar, onDisband) }
            val friends: @Composable () -> Unit = { PeopleCard(model, avatar, PeopleMode.GROUP, onInvite, onDesignateFamily) }
            val demo: @Composable () -> Unit = {
                model.pendingInvite?.let { DemoPanel("${it.name}님이 그룹 초대를 받았어요", onDemoAccept, onDemoDecline) }
            }
            ScreenFrame(
                header = header,
                singleColumn = { members(); friends(); demo(); Note() },
                twoColumns = {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) { members(); demo(); Note() }
                    Column(Modifier.weight(1.4f)) { friends() }
                },
            )
        }
        // R4-5 [가족]: 1열 순서는 가족 카드(사고 알림 카드가 바로 아래, R8-10), 친구 목록, 시연용 패널(맨 아래).
        HomeTab.FAMILY -> {
            val card: @Composable () -> Unit = { FamilyCardView(model.familyCard, onReleaseFamily); belowFamily() }
            val friends: @Composable () -> Unit = { PeopleCard(model, avatar, PeopleMode.FAMILY, onInvite, onDesignateFamily) }
            val demo: @Composable () -> Unit = {
                model.pendingFamily?.let { DemoPanel("${it.name}님이 가족 요청을 받았어요", onDemoFamilyAccept, onDemoFamilyDecline) }
            }
            ScreenFrame(
                header = header,
                singleColumn = { card(); friends(); demo() },
                twoColumns = {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) { card(); demo() }
                    Column(Modifier.weight(1.4f)) { friends() }
                },
            )
        }
        // R4-5 [설정]: 내 상태(R2-1). 주행 중에도 된다(R10).
        HomeTab.SETTINGS -> {
            val status: @Composable () -> Unit = {
                CardColumn {
                    Text("내 상태", fontSize = TITLE, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("업무 중이면 따라가기 요청을 받지 않고, 오프라인이면 친구 목록에 보이지 않아요", fontSize = SMALL, color = Soft, lineHeight = 40.sp)
                    MyStatusSegment(model.myStatus, onMyStatus)
                }
            }
            ScreenFrame(
                header = header,
                singleColumn = { status() },
                twoColumns = { Column(Modifier.weight(1f)) { status() }; Spacer(Modifier.weight(1f)) },
            )
        }
    }
}

/** SPEC R2-5: 탭 네 개. 지금 탭을 파랑으로 채운다. 주행 화면의 [함께 주행] [그룹 n명](R14-18)과 같은 자리·같은 모양이다. */
@Composable
private fun HomeTabs(selected: HomeTab, onTab: (HomeTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        HomeTab.entries.forEach { t ->
            val (label, icon) = when (t) {
                HomeTab.FRIEND_MAP -> "친구 지도" to ConvoyIcon.Pin
                HomeTab.GROUP -> "그룹" to ConvoyIcon.Group
                HomeTab.FAMILY -> "가족" to ConvoyIcon.Shield
                HomeTab.SETTINGS -> "설정" to null
            }
            ViewTab(label, icon, t == selected) { onTab(t) }
        }
    }
}

/**
 * SPEC R4-5, R14-12 [친구 지도]: 지도가 머리줄 아래를 채우고, 아래쪽에 "함께 출발" 카드를, 왼쪽 아래에 [내 위치]를 띄운다.
 * 좁은 창은 지도를 480dp로 두고 카드를 지도 아래로 내려 함께 손 스크롤한다(R14-16과 같다).
 * 주행 중에도 지도는 보이고 카드의 버튼만 R10을 따른다. [demo]는 따라가기 요청의 시연용 패널이다(카드 곁).
 */
@Composable
private fun FriendMapTab(
    model: HomeScreenModel,
    header: @Composable (Boolean) -> Unit,
    avatar: (String) -> Pair<Color, Color>,
    map: @Composable (Modifier) -> Unit,
    canLocate: Boolean,
    onLocate: () -> Unit,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onFollow: (String) -> Unit,
    demo: @Composable () -> Unit,
) {
    val card: @Composable (Modifier) -> Unit = { m -> DepartureCard(model, avatar, onStart, onResume, onFollow, m) }
    key(LocalDensity.current.density) { BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Page)) {
        val single = isSingleColumn(maxWidth.value)
        if (single) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                header(true)
                Box(Modifier.fillMaxWidth().height(NARROW_MAP_HEIGHT)) {
                    map(Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.BottomStart).padding(MAP_INSET)) { LocateButton(canLocate, onLocate) }
                }
                card(Modifier)
                demo()
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                header(false)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    map(Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.BottomStart).padding(MAP_INSET)) { LocateButton(canLocate, onLocate) }
                    // 카드가 지도보다 길면 카드 안에서 손으로 스크롤한다(자동 스크롤 없음).
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(MAP_INSET)
                            .width(DEPARTURE_CARD_WIDTH).heightIn(max = maxHeight - MAP_INSET * 2)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        demo()
                        card(Modifier.shadow(8.dp, CardShape))
                    }
                }
            }
        }
    } }
}

/** 그룹 탭 아래의 안내. */
@Composable
private fun Note() {
    Text(
        "가족은 항상 연결되고, 그룹은 함께 이동할 때만 꾸려요. 설정은 정차 중에만 돼요.",
        fontSize = SMALL, color = Soft, lineHeight = 40.sp, modifier = Modifier.padding(horizontal = 6.dp),
    )
}

/** SPEC R4-5 [그룹]: 이번 주행 그룹의 멤버(함께 출발 카드와 같은 대상·순서)와 [그룹 해산]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupMembersCard(model: HomeScreenModel, avatar: (String) -> Pair<Color, Color>, onDisband: () -> Unit) {
    CardColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle(ConvoyIcon.Group, Accent, "그룹", "이번 주행 그룹", Accent)
            Spacer(Modifier.weight(1f))
            if (model.departure.size > 1) SoftButton("그룹 해산", enabled = model.canDisband, onClick = onDisband)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.departure.forEach { MemberChip(it, avatar(it.id)) }
        }
        if (model.departure.size == 1) Text("아래 친구 목록에서 [그룹에 초대]를 눌러요", fontSize = SMALL, color = Soft)
    }
}

@Composable
private fun CardColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Card, CardShape).border(1.dp, Palette.CardLine, CardShape).padding(horizontal = 36.dp, vertical = 32.dp),
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
private fun FamilyCardView(card: FamilyCard, onRelease: () -> Unit, modifier: Modifier = Modifier) {
    CardColumn(modifier) {
        CardTitle(ConvoyIcon.Shield, Palette.FamilyText, "가족", "항상 연결", Palette.FamilyText)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            if (card.name != null) {
                Box(Modifier.size(96.dp).background(Palette.FamilyFill, CircleShape), contentAlignment = Alignment.Center) {
                    Text(card.name.take(1), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Palette.FamilyText)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(card.name, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(card.message, fontSize = SMALL, color = Soft)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(card.message, fontSize = BODY, fontWeight = FontWeight.Bold, color = Ink)
                    Text("친구 목록에서 [가족으로 설정]을 눌러요", fontSize = SMALL, color = Soft)
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
 * R4-5, R12-1: 함께 출발(시안, [친구 지도] 탭의 그룹 요약). 멤버 칩과 [목적지로 함께]·[앞차 따라가기] 두 버튼.
 * [앞차 따라가기]를 누르면 따라갈 멤버 버튼이 펼쳐진다(업무 중이면 잠김). 진행 중이면 [진행 중인 주행으로].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepartureCard(
    model: HomeScreenModel,
    avatar: (String) -> Pair<Color, Color>,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onFollow: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosing by remember { mutableStateOf(false) }
    CardColumn(modifier) {
        CardTitle(ConvoyIcon.Group, Accent, "함께 출발", "이번 주행 그룹", Accent)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.departure.forEach { MemberChip(it, avatar(it.id)) }
        }
        if (model.departure.size == 1) Text("그룹 탭에서 친구를 초대해 주세요", fontSize = SMALL, color = Soft)
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

/** 친구 목록을 쓰는 탭. [GROUP]은 그룹 표시, [FAMILY]는 [가족으로 설정]을 둔다(R4-5, R9-5). */
private enum class PeopleMode { GROUP, FAMILY }

/** R4-5 친구 목록(시안): 오프라인은 보이지 않는다(R2-2). 가족은 상태 줄에 "· 가족"으로 붙는다. */
@Composable
private fun PeopleCard(
    model: HomeScreenModel,
    avatar: (String) -> Pair<Color, Color>,
    mode: PeopleMode,
    onInvite: (String) -> Unit,
    onDesignateFamily: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Card, CardShape).border(1.dp, Palette.CardLine, CardShape)
            .padding(horizontal = 36.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("친구", fontSize = TITLE, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text("오프라인인 사람은 보이지 않아요", fontSize = SMALL, color = Soft)
        }
        model.people.forEachIndexed { i, p ->
            PersonLine(p, avatar(p.id), mode, onInvite, onDesignateFamily, last = i == model.people.lastIndex)
        }
    }
}

@Composable
private fun PersonLine(
    p: PersonRow,
    colors: Pair<Color, Color>,
    mode: PeopleMode,
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
        // R9-5 [가족] 탭: 가족(또는 수락 대기)이면 상태 줄에 표시하고, 아니면 [가족으로 설정].
        if (mode == PeopleMode.FAMILY && p.familyTag == null) {
            OutlineButton("가족으로 설정", enabled = p.canDesignateFamily, width = 240.dp) { onDesignateFamily(p.id) }
        }
        // R4-5 [그룹] 탭: 그룹 밖이면 [그룹에 초대], 보낸 뒤 "수락 대기", 수락하면 "그룹 멤버".
        if (mode == PeopleMode.GROUP) when (p.groupLabel) {
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
