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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.convoy.data.MeetingRow
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.meetingRowText
import java.time.ZoneId

private val SMALL = 28.sp
private val BODY = 30.sp
private val CardShape = RoundedCornerShape(32.dp)

/** SPEC R14-20: 좁은 창에서 지도 높이(R14-16과 같다). */
private val NARROW_MAP_HEIGHT = 480.dp

/** 비교 표 한 줄. [detour]가 null이면 우회 칸이 없다(목적지 없음). */
data class MeetingRowUi(val id: String, val name: String, val arrival: String, val detour: String?)

/** SPEC R14-20: 합류 장소 비교 화면이 그릴 값. */
data class MeetingCompareModel(
    /** R11-21 목록(내비 목적지, 최근, 즐겨찾기 또는 예비 목록). 같은 곳은 한 번만. */
    val candidates: List<Destination>,
    val selected: Destination?,
    val rows: List<MeetingRowUi>,
    /** 목적지가 있을 때만 "우회" 칸을 보인다(R14-10). */
    val showDetour: Boolean,
    /** 후보 고르기를 누를 수 있다(정차 중, R13-11). */
    val canChoose: Boolean,
    /** [경유지로 제안]을 누를 수 있다. */
    val canPropose: Boolean,
    /** [경유지로 제안]이 잠긴 이유(proposalNotice). */
    val notice: String?,
)

/** SPEC R11-21의 목록을 후보 목록으로 펼친다. 같은 곳(10m 미만)은 처음 것만 둔다. */
fun meetingCandidates(picker: DestinationPickerModel): List<Destination> =
    (listOfNotNull(picker.suggestion) + picker.recent.map { it.destination } + picker.bookmarks.map { it.destination })
        .fold(emptyList()) { acc, d -> if (acc.any { samePlace(it, d) }) acc else acc + d }

/** SPEC R14-20: 표의 줄 문구. 지금 시각은 후보를 고른 시각이다. 이름은 나면 "나". */
fun meetingRowsUi(rows: List<MeetingRow>, names: Map<String, String>, selectedAt: Long, zone: ZoneId, hasDestination: Boolean): List<MeetingRowUi> =
    rows.map { r ->
        val text = meetingRowText(selectedAt, zone, r.located, r.loading, r.toCandidate?.durationSeconds, r.detourSeconds, hasDestination)
        MeetingRowUi(r.id, names[r.id] ?: r.id, text.arrival, text.detour)
    }

/**
 * SPEC R14-11·20: 비교 지도. 그룹 경로는 파랑, 멤버마다 후보까지 경로는 노랑(파랑 위). 마커는 "나" 파랑, 다른 멤버 회색.
 * 카메라는 [cameraReady]일 때만 노랑 경로와 후보에 맞춘다(노랑이 없으면 후보를 가운데로).
 */
fun meetingMapUi(
    groupRoute: List<GeoPoint>,
    starts: List<Pair<String, GeoPoint?>>,
    myId: String,
    names: Map<String, String>,
    rows: List<MeetingRow>,
    candidate: Destination?,
    destination: Destination?,
    cameraReady: Boolean,
): GroupMapUi {
    val yellow = rows.mapNotNull { it.toCandidate?.points?.takeIf { p -> p.size >= 2 } }
    val lines = listOfNotNull(groupRoute.takeIf { it.size >= 2 }?.let { MapLine(it, LineColor.BLUE) }) + yellow.map { MapLine(it, LineColor.YELLOW) }
    val markers = starts.mapNotNull { (id, p) ->
        p?.let { MapMarker(id, if (id == myId) MY_NAME else (names[id] ?: id).take(1), it, if (id == myId) MarkerTone.ME else MarkerTone.GRAY) }
    }
    val pins = listOfNotNull(
        destination?.let { MapPin(it.name, GeoPoint(it.latitude, it.longitude), PinKind.DESTINATION) },
        candidate?.let { MapPin(it.name, GeoPoint(it.latitude, it.longitude), PinKind.CANDIDATE) },
    )
    val candidatePoint = candidate?.let { GeoPoint(it.latitude, it.longitude) }
    return GroupMapUi(
        lines = lines,
        markers = markers,
        pins = pins,
        banner = null,
        memberLabels = emptyMap(),
        turnCard = null,
        arrivalCard = null,
        fitPoints = if (cameraReady && candidatePoint != null) yellow.flatten() + candidatePoint else null,
        explicitFit = true,
    )
}

/**
 * SPEC R14-20: 합류 장소 비교(그룹 화면 위에 덮는 화면). 넓은 창은 왼쪽 지도, 오른쪽 비교 표·[경유지로 제안]·후보 목록.
 * 좁은 창은 지도 480dp 아래에 표, 후보 목록 순으로 쌓아 손으로 스크롤한다.
 */
@Composable
fun MeetingCompareScreen(
    model: MeetingCompareModel,
    map: @Composable (Modifier) -> Unit,
    onClose: () -> Unit,
    onChoose: (Destination) -> Unit,
    onPropose: () -> Unit,
    headerExtra: @Composable () -> Unit = {},
) {
    key(LocalDensity.current.density) { BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Page)) {
        val single = isSingleColumn(maxWidth.value)
        val header: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                BrandMark(wordmark = false)
                Text("합류 장소 비교", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Spacer(Modifier.weight(1f))
                headerExtra()
                // [닫기]는 조작이 아니라 R10 제한이 없다(R14-20).
                Box(
                    Modifier.heightIn(min = 80.dp).background(Palette.Card, RoundedCornerShape(20.dp))
                        .border(2.dp, Palette.CardLine, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onClose).padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("닫기", fontSize = BODY, fontWeight = FontWeight.Bold, color = Palette.Ink) }
            }
        }
        val table: @Composable () -> Unit = { CompareTable(model, onPropose) }
        val list: @Composable () -> Unit = { CandidateList(model, onChoose) }
        if (single) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                header()
                Box(Modifier.fillMaxWidth().height(NARROW_MAP_HEIGHT)) { map(Modifier.fillMaxSize()) }
                table()
                list()
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                header()
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Box(Modifier.weight(1.2f).fillMaxHeight()) { map(Modifier.fillMaxSize()) }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        table()
                        list()
                    }
                }
            }
        }
    } }
}

/** 비교 표와 [경유지로 제안]. 후보를 고르기 전에는 안내만 보여 준다. */
@Composable
private fun CompareTable(model: MeetingCompareModel, onPropose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Palette.Card, CardShape).border(1.dp, Palette.CardLine, CardShape).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val selected = model.selected
        if (selected == null) {
            Text("아래에서 후보를 고르면 멤버마다 언제 닿는지 보여 줘요", fontSize = BODY, fontWeight = FontWeight.Bold, color = Palette.Ink)
            return@Column
        }
        Text(selected.name, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(selected.address, fontSize = SMALL, color = Palette.Soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        TableLine("멤버", "도착", if (model.showDetour) "우회" else null, header = true)
        model.rows.forEach { TableLine(it.name, it.arrival, if (model.showDetour) it.detour else null, header = false) }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 96.dp)
                .background(if (model.canPropose) Palette.Brand else Palette.DisabledFill, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .clickable(enabled = model.canPropose, onClick = onPropose),
            contentAlignment = Alignment.Center,
        ) { Text("경유지로 제안", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = if (model.canPropose) Color.White else Palette.Soft) }
        model.notice?.let { Text(it, fontSize = SMALL, color = Palette.Soft) }
    }
}

@Composable
private fun TableLine(name: String, arrival: String, detour: String?, header: Boolean) {
    val color = if (header) Palette.Soft else Palette.Ink
    val weight = if (header) FontWeight.SemiBold else FontWeight.Bold
    val size = if (header) SMALL else BODY
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.width(140.dp), fontSize = size, fontWeight = weight, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(arrival, Modifier.weight(1f), fontSize = size, fontWeight = weight, color = color)
        if (detour != null) Text(detour, Modifier.weight(1f), fontSize = size, fontWeight = weight, color = color)
    }
}

/** R11-21 목록에서 한 곳을 고른다. 고른 후보는 파랑. 주행 중이면 잠긴다. "추천"이라는 말은 쓰지 않는다(R14-11). */
@Composable
private fun CandidateList(model: MeetingCompareModel, onChoose: (Destination) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Palette.Card, CardShape).border(1.dp, Palette.CardLine, CardShape).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("후보", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        if (!model.canChoose) Text("정차 중에만 고를 수 있어요", fontSize = SMALL, color = Palette.Soft)
        model.candidates.forEach { d ->
            val on = model.selected?.let { samePlace(it, d) } == true
            val enabled = model.canChoose
            Row(
                Modifier.fillMaxWidth().heightIn(min = 104.dp)
                    .background(if (on) Palette.Brand else if (enabled) Palette.ButtonFill else Palette.DisabledFill, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(enabled = enabled) { onChoose(d) }
                    .padding(horizontal = 26.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(d.name, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else if (enabled) Palette.Ink else Palette.Soft,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(d.address, fontSize = SMALL, color = if (on) Palette.OnBrandSoft else Palette.Soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (on) Icon24(ConvoyIcon.Check, Color.White, 38.dp, 2.6f)
            }
        }
    }
}
