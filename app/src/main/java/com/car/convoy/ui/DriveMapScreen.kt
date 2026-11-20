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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.convoy.domain.DriveView
import com.car.convoy.domain.InfoCardSection
import com.car.convoy.domain.infoCardSections

private val SMALL = 28.sp
private val BODY = 30.sp

/** SPEC R14-16: 좁은 창에서 지도의 높이. */
private val NARROW_MAP_HEIGHT = 480.dp

/** 정보 카드의 너비(시안 07·08의 오른쪽 아래 카드). */
private val INFO_CARD_WIDTH = 640.dp

/** 지도 위 카드와 버튼의 가장자리 여백. */
private val MAP_INSET = 20.dp

/**
 * SPEC R14-16: 주행 화면(시안 07·08·11·12). 머리줄 아래를 지도가 다 채우고, 그 위에 회전 안내(지도 안), 정보 카드(오른쪽 아래),
 * [내 위치]·리더 칩(왼쪽 아래)만 띄운다. 좁은 창은 지도를 480dp로 두고 정보 카드를 지도 아래로 내려 함께 손 스크롤한다.
 */
@Composable
internal fun DriveMapScreen(
    model: GroupScreenModel,
    mapUi: GroupMapUi?,
    map: @Composable (Modifier) -> Unit,
    waypoint: WaypointPanelModel?,
    headerExtra: @Composable () -> Unit,
    onShowView: (DriveView) -> Unit,
    canLocate: Boolean,
    onLocate: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val labels = mapUi?.memberLabels?.values?.filterNotNull().orEmpty()
    val sections = infoCardSections(
        finished = model.finished,
        hasRequest = waypoint?.request != null,
        hasWaypointStatus = waypoint?.hasStatus == true,
        hasRoute = mapUi?.hasRoute == true,
        meUnknown = mapUi?.meUnknown == true,
        memberRows = labels.size,
        hasArrival = mapUi?.arrivalCard != null,
    )
    val card: (@Composable (Modifier) -> Unit)? = if (sections.isEmpty()) null else { m ->
        InfoCard(sections, waypoint, labels, mapUi?.arrivalCard, onApprove, onReject, m)
    }
    val leaderName = if (model.members.any { it.isLeader }) "리더 · 나" else null

    key(LocalDensity.current.density) { BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Page)) {
        val single = isSingleColumn(maxWidth.value)
        val overlays: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {
            Column(
                Modifier.align(Alignment.BottomStart).padding(MAP_INSET),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LocateButton(canLocate, onLocate)
                leaderName?.let { LeaderChip(it) }
            }
        }
        if (single) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MapHeader(model, single = true, onShowView, headerExtra)
                Box(Modifier.fillMaxWidth().height(NARROW_MAP_HEIGHT)) {
                    map(Modifier.fillMaxSize())
                    overlays()
                }
                card?.invoke(Modifier.fillMaxWidth())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                MapHeader(model, single = false, onShowView, headerExtra)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    map(Modifier.fillMaxSize())
                    overlays()
                    // 카드가 지도보다 길면 카드 안에서 손으로 스크롤한다(R14-19, 자동 스크롤 없음).
                    card?.invoke(
                        Modifier.align(Alignment.BottomEnd).padding(MAP_INSET)
                            .width(INFO_CARD_WIDTH).heightIn(max = maxHeight - MAP_INSET * 2)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
    } }
}

/** SPEC R14-16: 주행 화면 머리줄. 앱 아이콘과 "Convoy", [함께 주행] [그룹 n명], 오른쪽 끝에 가족 표시. */
@Composable
private fun MapHeader(model: GroupScreenModel, single: Boolean, onShowView: (DriveView) -> Unit, extra: @Composable () -> Unit) {
    HeaderRows(
        single = single,
        lead = { ViewTabs(DriveView.MAP, model.members.size, onShowView) },
        trail = {
            if (!single) Spacer(Modifier.weight(1f))
            extra()
        },
    )
}

/** SPEC R14-16·18: [함께 주행] [그룹 n명]. 지금 화면을 파랑으로 채운다. 화면 이동이라 R10 제한이 없다. */
@Composable
internal fun ViewTabs(selected: DriveView, memberCount: Int, onShowView: (DriveView) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ViewTab("함께 주행", null, selected == DriveView.MAP) { onShowView(DriveView.MAP) }
        ViewTab("그룹 ${memberCount}명", ConvoyIcon.Group, selected == DriveView.GROUP) { onShowView(DriveView.GROUP) }
    }
}

@Composable
private fun ViewTab(label: String, icon: ConvoyIcon?, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    val fg = if (selected) Color.White else Palette.Ink
    Row(
        Modifier.heightIn(min = 80.dp)
            .background(if (selected) Palette.Brand else Palette.Card, shape)
            .border(if (selected) 0.dp else 2.dp, Palette.CardLine, shape)
            .clip(shape)
            .clickable(enabled = !selected, onClick = onClick)
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let { Icon24(it, fg, 32.dp, 2.2f) }
        Text(label, fontSize = BODY, fontWeight = FontWeight.Bold, color = fg)
    }
}

/** SPEC R14-16 [내 위치]: 누를 때만 내 위치를 가운데로 옮긴다. 이번 세션에서 위치를 못 받았거나 지도를 못 그리면 잠긴다. */
@Composable
private fun LocateButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(88.dp).shadow(6.dp, CircleShape).background(Palette.Card, CircleShape).clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).border(3.dp, if (enabled) Palette.Brand else Palette.Soft, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(14.dp).background(if (enabled) Palette.Brand else Palette.Soft, CircleShape))
        }
    }
}

/** SPEC R14-16: 리더 칩(시안 07의 "리더 준희"). 이 앱의 차는 언제나 리더라 "리더 · 나"다(R14-19). */
@Composable
private fun LeaderChip(text: String) {
    Box(
        Modifier.shadow(6.dp, RoundedCornerShape(999.dp)).background(Palette.Card, RoundedCornerShape(999.dp))
            .padding(horizontal = 28.dp, vertical = 14.dp),
    ) { Text(text, fontSize = SMALL, fontWeight = FontWeight.Bold, color = Palette.Ink) }
}

/** SPEC R14-19: 정보 카드. [sections] 순서대로 칸을 쌓고, 칸 사이에 가는 선을 둔다. */
@Composable
private fun InfoCard(
    sections: List<InfoCardSection>,
    waypoint: WaypointPanelModel?,
    memberLabels: List<String>,
    arrival: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier.shadow(8.dp, shape).background(Palette.Card, shape).padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        sections.forEachIndexed { i, section ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.CardLine))
            when (section) {
                InfoCardSection.FINISHED -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon24(ConvoyIcon.Check, Palette.Done, 40.dp, 2.6f)
                    Text("모두 도착했어요", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Done)
                }
                InfoCardSection.REQUEST -> waypoint?.request?.let { RequestSection(it, onApprove, onReject) }
                InfoCardSection.WAYPOINT -> waypoint?.let { WaypointSection(it) }
                InfoCardSection.MEMBERS -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    infoCardMemberLines(memberLabels).forEachIndexed { index, line -> MemberLine(line, isMore = index >= 3) }
                }
                InfoCardSection.ME_UNKNOWN -> Text("내 위치 확인 중", fontSize = BODY, fontWeight = FontWeight.Bold, color = Palette.Soft)
                InfoCardSection.ARRIVAL -> arrival?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon24(ConvoyIcon.Pin, Palette.Ink, 36.dp, 2.4f)
                        Text(it, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** 시안 12: "수진님의 경유지 요청", 장소, [수락] [거절]. 주행 중에도 누를 수 있다(R13-11). */
@Composable
private fun RequestSection(request: WaypointRequestUi, onApprove: () -> Unit, onReject: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(request.title, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon24(ConvoyIcon.Pin, Palette.Accent, 36.dp, 2.4f)
            Column {
                Text(request.placeName, fontSize = BODY, fontWeight = FontWeight.Bold, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(request.placeAddress, fontSize = SMALL, color = Palette.Soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.weight(1f).heightIn(min = 88.dp).background(Palette.Brand, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)).clickable(enabled = request.canRespond, onClick = onApprove),
                contentAlignment = Alignment.Center,
            ) { Text("수락", fontSize = BODY, fontWeight = FontWeight.ExtraBold, color = Color.White) }
            Box(
                Modifier.weight(1f).heightIn(min = 88.dp).background(Palette.Card, RoundedCornerShape(20.dp))
                    .border(2.dp, Palette.CardLine, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)).clickable(enabled = request.canRespond, onClick = onReject),
                contentAlignment = Alignment.Center,
            ) { Text("거절", fontSize = BODY, fontWeight = FontWeight.ExtraBold, color = Palette.Ink) }
        }
    }
}

/** R14-19 `WAYPOINT`: 경유지 이름, 반영 문구, 알림만. [다시 보내기]는 그룹 화면에서 정차 중에만 한다. */
@Composable
private fun WaypointSection(model: WaypointPanelModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.waypointNames.forEach { Text(it, fontSize = SMALL, fontWeight = FontWeight.Bold, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        model.applyText?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (model.applyComplete) Icon24(ConvoyIcon.Check, Palette.Done, 30.dp, 2.6f)
                Text(it, fontSize = SMALL, fontWeight = FontWeight.Bold,
                    color = if (model.applyComplete) Palette.Done else if (model.showResend) Palette.Danger else Palette.Accent)
            }
        }
        model.notice?.let { Text(it, fontSize = SMALL, color = Palette.Soft) }
    }
}

/** 멤버 한 줄: 이름 첫 글자 원과 "민수 · 앞 약 650 m". "외 n명" 줄은 원 없이 쓴다. */
@Composable
private fun MemberLine(text: String, isMore: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isMore) {
            Spacer(Modifier.width(56.dp))
            Text(text, fontSize = SMALL, fontWeight = FontWeight.Bold, color = Palette.Soft)
        } else {
            Box(Modifier.size(56.dp).background(Palette.MemberFill, CircleShape), contentAlignment = Alignment.Center) {
                Text(text.take(1), fontSize = SMALL, fontWeight = FontWeight.ExtraBold, color = Palette.MemberText)
            }
            Text(text, fontSize = BODY, fontWeight = FontWeight.Bold, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
