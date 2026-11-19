package com.car.convoy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.painterResource
import com.car.convoy.R
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
private val BigCard = RoundedCornerShape(32.dp)

/** SPEC R11-20: 그룹 주행 화면(시안: Convoy 그룹 흐름 시안 2-A). [model]만 그리고, 누름은 콜백으로 넘긴다. */
@Composable
fun GroupScreen(
    model: GroupScreenModel,
    onChooseDestination: (Destination) -> Unit,
    onRetry: () -> Unit,
    onDemoDepart: (String) -> Unit,
    onDemoArrive: (String) -> Unit,
    picker: DestinationPickerModel = destinationPickerModel(null, null, null, emptyList(), emptyList()),
    onIgnoreSuggestion: (Destination) -> Unit = {},
    onOpenNavi: (() -> Unit)? = null,
    onHome: () -> Unit = {},
    onEnd: () -> Unit = {},
    // ---- SPEC R13, 작업 32: 경유지 ----
    waypoint: WaypointPanelModel? = null,
    onProposeWaypoint: (Destination) -> Unit = {},
    onApproveWaypoint: () -> Unit = {},
    onRejectWaypoint: () -> Unit = {},
    onResendWaypoint: () -> Unit = {},
    onDemoProposeWaypoint: (String) -> Unit = {},
    onDemoFailWaypoint: (String) -> Unit = {},
    headerExtra: @Composable () -> Unit = {},
    belowMembers: @Composable () -> Unit = {},
    // ---- SPEC R14-16, 작업 36: 함께 주행 지도 ----
    /** 지도. null이면 지도 없이 이전 배치(R11-20)를 쓴다. */
    map: (@Composable (Modifier) -> Unit)? = null,
    /** 멤버 id → 앞뒤 문구(R14-13). 멤버 타일에 한 줄 더한다. */
    memberLabels: Map<String, String?> = emptyMap(),
    /** 시연 버튼 [<이름> 300m 앞으로](R14-3). null이면 보이지 않는다. */
    onDemoMapMove: ((String) -> Unit)? = null,
) {
    val left: @Composable () -> Unit = {
        DestinationCard(model)
        // 시안 S13: 리더 승인 패널은 길 안내 위 작은 패널처럼 맨 위 가까이에 둔다(주행 중에도 응답 가능, R13-11).
        waypoint?.request?.let { WaypointRequestCard(it, onApproveWaypoint, onRejectWaypoint) }
        waypoint?.takeIf { it.waypointNames.isNotEmpty() || it.applyText != null || it.notice != null }?.let { WaypointStatusCard(it, onResendWaypoint) }
        if (model.finished) FinishedBanner()
        MemberTiles(model.members, memberLabels)
        if (model.demoMemberIds.isNotEmpty()) DemoPanel(model, onDemoDepart, onDemoArrive, waypoint != null, onDemoProposeWaypoint, onDemoFailWaypoint, onDemoMapMove)
        belowMembers()
    }
    val right: @Composable () -> Unit = {
        DestinationPicker(model, picker, onChooseDestination, onIgnoreSuggestion, onOpenNavi,
            onPropose = onProposeWaypoint.takeIf { waypoint?.canPropose == true })
        if (model.showRetry) RetryButton(onRetry)
    }
    if (map != null) {
        MapScreenFrame(
            header = { single -> DriveHeader(model, single, onHome, onEnd, headerExtra) },
            map = map,
            panel = { left(); right() },
        )
        return
    }
    ScreenFrame(
        header = { single -> DriveHeader(model, single, onHome, onEnd, headerExtra) },
        // SPEC R11-20: 1열이면 왼쪽 열(상태·멤버) 다음 오른쪽 열(목적지 고르기 등).
        singleColumn = { left(); right() },
        twoColumns = {
            Column(Modifier.weight(1.35f), verticalArrangement = Arrangement.spacedBy(20.dp)) { left() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) { right() }
        },
    )
}

/**
 * 시안의 머리줄: [‹](홈으로), "목적지로 함께 / 그룹 주행 중", 가족 표시, 짙은 "도착 n / m".
 * R11-20의 [주행 끝내기]도 둔다. [‹]와 [주행 끝내기]는 주행 중에도 된다(R10).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriveHeader(model: GroupScreenModel, single: Boolean, onHome: () -> Unit, onEnd: () -> Unit, extra: @Composable () -> Unit) {
    val arrived = model.members.count { it.state == GroupMemberState.ARRIVED }
    val title: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Box(
                Modifier.size(80.dp).background(Card, RoundedCornerShape(24.dp)).border(1.dp, Palette.CardLine, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)).clickable(onClick = onHome),
                contentAlignment = Alignment.Center,
            ) { Icon24(ConvoyIcon.ChevronLeft, Ink, 36.dp, 2.4f) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("목적지로 함께", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Soft)
                Text(model.groupStatusLabel, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = if (model.finished) Done else Ink)
            }
        }
    }
    val actions: @Composable () -> Unit = {
        Box(
            Modifier.heightIn(min = 80.dp).background(Card, RoundedCornerShape(20.dp)).border(2.dp, Color(0xFFE8B4AE), RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)).clickable(onClick = onEnd).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) { Text("주행 끝내기", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Danger) }
        extra()
        Row(
            Modifier.background(Palette.Night, RoundedCornerShape(24.dp)).padding(horizontal = 28.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("도착", fontSize = SMALL, fontWeight = FontWeight.SemiBold, color = Color(0xFFC9D0DA))
            Text("$arrived", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("/ ${model.members.size}", fontSize = BODY, fontWeight = FontWeight.SemiBold, color = Color(0xFFC9D0DA))
        }
    }
    if (single) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            title()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { actions() }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            title()
            Spacer(Modifier.weight(1f))
            actions()
        }
    }
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

/** SPEC R14-16: 좁은 창에서 지도의 높이. */
private val NARROW_MAP_HEIGHT = 480.dp

/**
 * SPEC R14-16: 지도가 있는 그룹 주행 화면. 넓은 창(1,200dp 이상)은 머리줄 아래 왼쪽에 지도(남은 높이를 채우고 스크롤 안 됨),
 * 오른쪽에 그룹 패널(손 세로 스크롤)이다. 좁은 창은 머리줄 아래 지도를 480dp로 두고 그 아래 패널을 쌓아 함께 스크롤한다.
 * 자동 스크롤은 하지 않는다(R11-20).
 */
@Composable
private fun MapScreenFrame(
    header: @Composable (single: Boolean) -> Unit,
    map: @Composable (Modifier) -> Unit,
    panel: @Composable () -> Unit,
) {
    key(LocalDensity.current.density) { BoxWithConstraints(Modifier.fillMaxSize().background(Page)) {
        val single = isSingleColumn(maxWidth.value)
        if (single) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                header(true)
                map(Modifier.fillMaxWidth().height(NARROW_MAP_HEIGHT))
                panel()
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                header(false)
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    map(Modifier.weight(1.2f).fillMaxHeight())
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) { panel() }
                }
            }
        }
    } }
}

/**
 * 머리줄 왼쪽의 앱 아이콘. Pleos 가이드대로 아이콘에 글자를 함께 둔다(홈). 버튼이 많은 주행 화면은 아이콘만 두고
 * 옆에 화면 제목이 온다([wordmark] = false).
 */
@Composable
internal fun BrandMark(wordmark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Image(
            painter = painterResource(R.drawable.convoy_launcher_art),
            contentDescription = if (wordmark) null else "Convoy",
            modifier = Modifier.size(64.dp),
        )
        if (wordmark) Text("Convoy", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
    }
}

/** 머리줄: 앱 아이콘과 [lead], 그 뒤 상태 표시([trail]). 1열이면 두 줄로 나눈다. */
@Composable
internal fun HeaderRows(
    single: Boolean,
    lead: @Composable () -> Unit,
    trail: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    wordmark: Boolean = true,
) {
    val first: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        BrandMark(wordmark)
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

/** 시안의 목적지 카드: 파란 사각형 안의 위치 아이콘, "함께 가는 곳", 큰 이름, 주소. */
@Composable
private fun DestinationCard(model: GroupScreenModel) {
    Row(
        Modifier.fillMaxWidth().background(Card, BigCard).border(1.dp, Palette.CardLine, BigCard).padding(horizontal = 40.dp, vertical = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        Box(Modifier.size(104.dp).background(Palette.Brand, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
            Icon24(ConvoyIcon.Pin, Color.White, 56.dp, 2f)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("함께 가는 곳", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Soft)
            if (model.destinationName == null) {
                Text("목적지를 골라 주세요", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            } else {
                Text(model.destinationName, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Ink, lineHeight = 68.sp)
                model.destinationAddress?.let { Text(it, fontSize = SMALL, color = Soft) }
            }
        }
    }
}

@Composable
private fun FinishedBanner() {
    Box(Modifier.fillMaxWidth().background(Palette.DoneFill, BigCard).padding(28.dp)) {
        Text("모두 도착했어요", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Done)
    }
}

/** 시안의 멤버 타일: 한 줄에 3개씩. */
@Composable
private fun MemberTiles(members: List<MemberRow>, mapLabels: Map<String, String?> = emptyMap()) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        members.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                rowItems.forEach { MemberTile(it, Modifier.weight(1f), mapLabels[it.id]) }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MemberTile(row: MemberRow, modifier: Modifier, mapLabel: String? = null) {
    val color = memberStateColor(row.state)
    Column(
        modifier.background(Card, RoundedCornerShape(28.dp)).border(1.dp, Palette.CardLine, RoundedCornerShape(28.dp)).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(64.dp).background(if (row.isLeader) Palette.MemberFill else color.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (row.isLeader) "나" else row.name.take(1), fontSize = SMALL, fontWeight = FontWeight.ExtraBold, color = if (row.isLeader) Palette.MemberText else color)
            }
            Text(if (row.isLeader) "내 차" else row.name, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier.fillMaxWidth().background(color.copy(alpha = 0.12f), RoundedCornerShape(18.dp)).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (row.state == GroupMemberState.ARRIVED) Icon24(ConvoyIcon.Check, color, 30.dp, 2.6f)
            Text(row.stateLabel, fontSize = BODY, fontWeight = FontWeight.ExtraBold, color = color)
        }
        Text(if (row.isLeader) "리더" else if (row.isVirtual) "가상 멤버" else "", fontSize = SMALL, color = Soft)
        // SPEC R14-13·16: 지도 기준 앞뒤 문구. 이름은 타일 위에 있으므로 "민수 · " 뒤만 보여 준다.
        mapLabel?.let { Text(it.substringAfter(" · "), fontSize = SMALL, fontWeight = FontWeight.Bold, color = Ink) }
    }
}

/** SPEC R11-20, R11-21: 목적지 바꾸기(시안). 내비에서 정한 목적지 카드, 내비 검색 줄, 최근 목적지(또는 예비 목록), 즐겨찾기. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DestinationPicker(
    model: GroupScreenModel,
    picker: DestinationPickerModel,
    onChoose: (Destination) -> Unit,
    onIgnore: (Destination) -> Unit,
    onOpenNavi: (() -> Unit)?,
    /** SPEC R13-1: [경유지로]. null이면 보이지 않는다(정차 중이 아니거나 목적지가 없음). */
    onPropose: ((Destination) -> Unit)? = null,
) {
    val enabled = model.canChooseDestination
    Column(
        Modifier.fillMaxWidth().background(Card, BigCard).border(1.dp, Palette.CardLine, BigCard).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("목적지 바꾸기", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        if (!enabled && !model.finished) Text("정차 중에 리더만 바꿀 수 있어요", fontSize = SMALL, color = Soft)
        picker.suggestion?.let { NaviSuggestionCard(it, enabled, onChoose, onIgnore, onPropose) }
        // R11-21: [내비에서 검색하기]는 현대차 내비 앱을 띄운다(화면 이동이라 R10 제한 없음). 띄울 수 없으면 안내만.
        Row(
            Modifier.fillMaxWidth().heightIn(min = 88.dp)
                .background(Palette.ButtonFill, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .then(if (onOpenNavi != null) Modifier.clickable(onClick = onOpenNavi) else Modifier)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon24(ConvoyIcon.Search, if (onOpenNavi != null) Accent else Soft, 34.dp, 2.4f)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onOpenNavi != null) {
                    Text("내비에서 검색하기", fontSize = BODY, fontWeight = FontWeight.Bold, color = Accent)
                    Text("정하고 돌아오면 여기에 떠요", fontSize = SMALL, color = Soft)
                } else {
                    Text("현대차 내비에서 검색해 목적지를 정하고 돌아오면 여기에 떠요", fontSize = SMALL, color = Palette.Ink)
                }
            }
            if (onOpenNavi != null) Icon24(ConvoyIcon.ChevronRight, Soft, 32.dp, 2.4f)
        }
        Text(if (picker.usingFallback) "추천 목적지" else "최근 목적지", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Soft)
        if (onPropose != null) Text("[경유지로]를 누르면 목적지는 그대로 두고 들를 곳으로 추가해요", fontSize = SMALL, color = Soft)
        picker.recent.forEach { PickLine(it, enabled, onChoose, onPropose) }
        if (picker.bookmarks.isNotEmpty()) {
            Text("즐겨찾기", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Soft)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                picker.bookmarks.forEach { row ->
                    BookmarkButton(row, enabled, onChoose)
                    // SPEC R13-1: 즐겨찾기도 경유지 제안 출처다(Codex 리뷰). 지금 목적지인 곳은 빼고 붙인다.
                    if (onPropose != null && !row.current) WaypointPill { onPropose(row.destination) }
                }
            }
        }
    }
}

/** 시안의 즐겨찾기 버튼(집·회사는 아이콘). 지금 목적지면 파랑. */
@Composable
private fun BookmarkButton(row: PickRow, enabled: Boolean, onChoose: (Destination) -> Unit) {
    val d = row.destination
    val fg = if (row.current) Color.White else if (enabled) Ink else Soft
    Row(
        Modifier.widthIn(min = 220.dp).heightIn(min = 84.dp)
            .background(if (row.current) Palette.Brand else if (enabled) Palette.ButtonFill else Palette.DisabledFill, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled && !row.current) { onChoose(d) }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Icon24(if (d.name.contains("집")) ConvoyIcon.Home else if (d.name.contains("회사")) ConvoyIcon.Building else ConvoyIcon.Pin, fg, 30.dp, 2.2f)
        Text(d.name, fontSize = SMALL, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** R11-21: 리더가 내비에서 정한 목적지를 그룹과 함께할지 묻는다. 두 버튼 모두 R10 "그룹 목적지 변경"의 제한을 받는다(주행 중 잠김). */
@Composable
private fun NaviSuggestionCard(
    d: Destination,
    enabled: Boolean,
    onChoose: (Destination) -> Unit,
    onIgnore: (Destination) -> Unit,
    onPropose: ((Destination) -> Unit)? = null,
) {
    Column(
        // 시안의 짙은 카드.
        Modifier.fillMaxWidth().background(Palette.Night, RoundedCornerShape(24.dp)).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("내비에서 정한 목적지", fontSize = SMALL, color = Palette.OnNightSoft)
        Text(d.name, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(d.address, fontSize = SMALL, color = Palette.OnNightSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.weight(1f).heightIn(min = 84.dp)
                    .background(if (enabled) Color.White else Palette.NightTile, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled) { onChoose(d) },
                contentAlignment = Alignment.Center,
            ) { Text("그룹과 함께 가기", fontSize = BODY, fontWeight = FontWeight.Bold, color = if (enabled) Palette.MemberText else Palette.OnNightSoft) }
            Box(
                Modifier.widthIn(min = 140.dp).heightIn(min = 84.dp)
                    .background(if (enabled) Color.Transparent else Palette.NightTile, RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF3B4552), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled) { onIgnore(d) },
                contentAlignment = Alignment.Center,
            ) { Text("무시", fontSize = BODY, fontWeight = FontWeight.Bold, color = if (enabled) Color.White else Palette.OnNightSoft) }
        }
        // SPEC R13-1: 내비에서 정한 곳을 목적지 대신 경유지로 넣는다(Codex 리뷰). 정차 중에만 보인다(R13-11).
        if (onPropose != null) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 84.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPropose(d) },
                contentAlignment = Alignment.Center,
            ) { Text("경유지로 추가", fontSize = BODY, fontWeight = FontWeight.Bold, color = Color.White) }
        }
    }
}

/** 시안의 목적지 줄: 지금 목적지는 파랑과 ✓, 나머지는 회색 줄과 ›. 비활성은 회색 바탕과 보조 글자색(대비 유지). */
@Composable
private fun PickLine(row: PickRow, enabled: Boolean, onChoose: (Destination) -> Unit, onPropose: ((Destination) -> Unit)? = null) {
    val d = row.destination
    val selected = row.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 104.dp)
            .background(if (selected) Palette.Brand else if (enabled) Palette.ButtonFill else Palette.DisabledFill, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = enabled && !selected) { onChoose(d) }
            .padding(horizontal = 26.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(d.name, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) Color.White else if (enabled) Ink else Soft,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (selected) "지금 목적지 · ${d.address}" else d.address,
                fontSize = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (selected) Palette.OnBrandSoft else Soft,
            )
        }
        if (selected) Icon24(ConvoyIcon.Check, Color.White, 38.dp, 2.6f)
        else if (onPropose != null) WaypointPill { onPropose(d) }
        else Icon24(ConvoyIcon.ChevronRight, Soft, 32.dp, 2.4f)
    }
}

/** SPEC R13-1: 목록 줄 오른쪽의 [경유지로]. 줄 전체를 누르면 목적지 변경, 이 버튼은 경유지 제안이다. */
@Composable
private fun WaypointPill(onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 80.dp).widthIn(min = 160.dp)
            .background(Card, RoundedCornerShape(999.dp))
            .border(2.dp, Accent, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Text("경유지로", fontSize = SMALL, fontWeight = FontWeight.Bold, color = Accent) }
}

/** 시안 S13: 리더 승인 패널. [수락] [거절]은 응답이라 주행 중에도 된다(R13-11). */
@Composable
private fun WaypointRequestCard(request: WaypointRequestUi, onApprove: () -> Unit, onReject: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Card, BigCard).border(3.dp, Accent, BigCard).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(request.title, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon24(ConvoyIcon.Pin, Accent, 40.dp, 2.4f)
            Column {
                Text(request.placeName, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(request.placeAddress, fontSize = SMALL, color = Soft)
            }
        }
        Text("목적지는 그대로 두고 경유지로 추가해요", fontSize = SMALL, color = Soft)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.weight(1f).heightIn(min = 96.dp).background(Palette.Brand, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp)).clickable(enabled = request.canRespond, onClick = onApprove),
                contentAlignment = Alignment.Center,
            ) { Text("수락", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
            Box(
                Modifier.weight(1f).heightIn(min = 96.dp).background(Card, RoundedCornerShape(22.dp))
                    .border(2.dp, Palette.CardLine, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp)).clickable(enabled = request.canRespond, onClick = onReject),
                contentAlignment = Alignment.Center,
            ) { Text("거절", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
        }
    }
}

/** 시안 S14·S15: 경유지 목록과 반영 n/m. 일부만 반영되면 처리 중·실패를 이름과 함께 보여 준다(R13-7). */
@Composable
private fun WaypointStatusCard(model: WaypointPanelModel, onResend: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Card, BigCard).border(1.dp, Palette.CardLine, BigCard).padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        model.waypointNames.forEach { Text(it, fontSize = BODY, fontWeight = FontWeight.Bold, color = Ink) }
        model.applyText?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (model.applyComplete) Icon24(ConvoyIcon.Check, Done, 34.dp, 2.6f)
                Text(it, fontSize = BODY, fontWeight = FontWeight.Bold, color = if (model.applyComplete) Done else if (model.showResend) Danger else Accent)
            }
        }
        if (model.showResend) {
            OutlinedButton(
                onClick = onResend,
                modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text("실패한 차에 다시 보내기", fontSize = BODY, fontWeight = FontWeight.Bold, color = Danger) }
        }
        model.notice?.let { Text(it, fontSize = SMALL, color = Soft) }
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

/** SPEC R11-18, R11-20: 가상 멤버의 출발·도착을 만드는 시연용 패널(시안). 점선 테두리로 실제 기능과 구분한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoPanel(
    model: GroupScreenModel,
    onDepart: (String) -> Unit,
    onArrive: (String) -> Unit,
    showWaypoint: Boolean = false,
    onProposeWaypoint: (String) -> Unit = {},
    onFailWaypoint: (String) -> Unit = {},
    onMapMove: ((String) -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFFFFFBF4), RoundedCornerShape(28.dp))
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFB98A45),
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Chip("시연용", Demo, SMALL)
            Text(if (showWaypoint) "가상 멤버 출발·도착·경유지" else "가상 멤버 출발·도착", fontSize = SMALL, color = Color(0xFF3B4552))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.members.filter { it.id in model.demoMemberIds }.forEach { m ->
                DemoButton("${m.name} 출발") { onDepart(m.id) }
                DemoButton("${m.name} 도착") { onArrive(m.id) }
            }
        }
        // SPEC R13-12: 가상 멤버의 경유지 제안(덕평휴게소)과 다음 반영 실패.
        if (showWaypoint) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.members.filter { it.id in model.demoMemberIds }.forEach { m ->
                DemoButton("${m.name}: 덕평휴게소 제안") { onProposeWaypoint(m.id) }
                DemoButton("${m.name} 반영 실패") { onFailWaypoint(m.id) }
            }
        }
        // SPEC R14-3: 가상 멤버를 경로를 따라 300m 앞으로.
        if (onMapMove != null) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.members.filter { it.id in model.demoMemberIds }.forEach { m ->
                DemoButton("${m.name} 300m 앞으로") { onMapMove(m.id) }
            }
        }
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.widthIn(min = 200.dp).heightIn(min = 80.dp)
            .background(Card, RoundedCornerShape(18.dp))
            .border(2.dp, Color(0xFFB98A45), RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = SMALL, fontWeight = FontWeight.Bold, color = Demo) }
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
