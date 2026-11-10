package com.car.convoy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Danger = Color(0xFFB3261E)
private val DangerSoft = Color(0xFFFBE9E7)
private val Ink = Color(0xFF16202E)
private val Soft = Color(0xFF5E6978)
private val Demo = Color(0xFFB7791F)

/**
 * SPEC R8-1~5, 작업 21: "괜찮으세요?" 확인. 그룹 화면 위에 덮고 뒤의 누름은 막는다.
 * 주행 중에도 응답할 수 있게(R10) 버튼 하나를 크게 둔다.
 */
@Composable
fun AccidentPrompt(model: AccidentScreenModel, onOk: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xCC0B0F14))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(1100.dp).background(Color.White, RoundedCornerShape(24.dp)).padding(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text("사고가 의심돼요", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Danger)
            Text("괜찮으세요?", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Ink)
            Box(Modifier.size(200.dp).background(DangerSoft, CircleShape).border(6.dp, Danger, CircleShape), contentAlignment = Alignment.Center) {
                Text(model.remainingLabel, fontSize = 96.sp, fontWeight = FontWeight.Bold, color = Danger)
            }
            Text(model.promptDetail, fontSize = 32.sp, color = Soft, textAlign = TextAlign.Center)
            Button(
                onClick = onOk,
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D7A46)),
            ) { Text("괜찮아요", fontSize = 52.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** SPEC R8-10: 가족에게 보낸 알림 카드. 받는 차가 없어 같은 화면에 보여 준다(시연용 표시). */
@Composable
fun SentFamilyCard(card: SentCard) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(DangerSoft, RoundedCornerShape(16.dp))
            .border(2.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("가족에게 보낸 알림", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Danger)
            Chip("시연용", Demo, 28.sp)
            Text(card.timeLabel, fontSize = 30.sp, color = Soft, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Text("사고가 의심되는데 30초 동안 응답이 없었어요", fontSize = 30.sp, color = Ink)
        Text("경고등  ${card.telltaleLabel}", fontSize = 28.sp, color = Ink)
        Text("위치  ${card.locationLabel}", fontSize = 28.sp, color = Ink)
    }
}

/** SPEC R8-9: 머리에 붙는 가족 표시. */
@Composable
fun FamilyChip(label: String) {
    Chip(label, Demo, 28.sp)
}
