package com.car.convoy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupMemberState

// 차량 화면은 160dpi라 1dp = 1px이다. 버튼 높이 80dp 이상, 본문 28sp 이상(R11-20, docs/sdk-findings.md).
private val BUTTON_HEIGHT = 96.dp
private val BODY = 30.sp
private val Ink = Color(0xFF16202E)
private val Soft = Color(0xFF5A6575)
private val Line = Color(0xFFDDE1E7)
private val Accent = Color(0xFF1F4E8C)

private fun stateColor(state: GroupMemberState): Color = when (state) {
    GroupMemberState.IDLE -> Color(0xFF8A94A3)
    GroupMemberState.REROUTING -> Color(0xFFC98A00)
    GroupMemberState.DRIVING -> Color(0xFF1F6FD1)
    GroupMemberState.ARRIVED -> Color(0xFF1D8A4F)
}

/** SPEC R11-20: 그룹 주행 화면. [model]만 그리고, 누름은 콜백으로 넘긴다. */
@Composable
fun GroupScreen(
    model: GroupScreenModel,
    onChooseDestination: (Destination) -> Unit,
    onRetry: () -> Unit,
    onDemoDepart: (String) -> Unit,
    onDemoArrive: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(40.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(model.groupStatusLabel, fontSize = BODY, color = Soft)
            Text(
                model.destinationName?.let { "목적지  $it" } ?: "목적지를 골라 주세요",
                fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Ink,
            )
            if (model.finished) {
                Box(
                    Modifier.fillMaxWidth().background(Color(0xFFDFF2E6), RoundedCornerShape(8.dp)).padding(24.dp),
                ) { Text("모두 도착했어요", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D6B3F)) }
            }
            model.members.forEach { MemberCard(it) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("목적지", fontSize = BODY, fontWeight = FontWeight.Bold, color = Ink)
            if (!model.canChooseDestination && !model.finished) {
                Text("정차 중에 리더만 바꿀 수 있어요", fontSize = BODY, color = Soft)
            }
            PRESET_DESTINATIONS.forEach { d ->
                Button(
                    onClick = { onChooseDestination(d) },
                    enabled = model.canChooseDestination,
                    modifier = Modifier.fillMaxWidth().heightIn(min = BUTTON_HEIGHT),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) { Text(d.name, fontSize = BODY) }
            }
            if (model.showRetry) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().heightIn(min = BUTTON_HEIGHT),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("경로 다시 요청", fontSize = BODY, color = Color(0xFFA2332A)) }
            }
            if (model.demoMemberIds.isNotEmpty()) DemoPanel(model, onDemoDepart, onDemoArrive)
        }
    }
}

@Composable
private fun MemberCard(row: MemberRow) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(8.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(Modifier.size(22.dp).background(stateColor(row.state), CircleShape))
        Text(row.name, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
        Text(row.stateLabel, fontSize = 34.sp, color = stateColor(row.state), fontWeight = FontWeight.Bold)
    }
}

/** SPEC R11-18, R11-20: 가상 멤버의 출발·도착을 만드는 시연용 패널. 실제 기능과 구분해 보여 준다. */
@Composable
private fun DemoPanel(model: GroupScreenModel, onDepart: (String) -> Unit, onArrive: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFFF5F6F8), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFFC98A00), RoundedCornerShape(8.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("시연용 · 가상 일행 조작", fontSize = BODY, fontWeight = FontWeight.Bold, color = Color(0xFF8A5A00))
        model.members.filter { it.id in model.demoMemberIds }.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(m.name, fontSize = BODY, color = Ink, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { onDepart(m.id) }, modifier = Modifier.heightIn(min = 80.dp)) {
                    Text("출발", fontSize = BODY)
                }
                OutlinedButton(onClick = { onArrive(m.id) }, modifier = Modifier.heightIn(min = 80.dp)) {
                    Text("도착", fontSize = BODY)
                }
            }
        }
    }
}
