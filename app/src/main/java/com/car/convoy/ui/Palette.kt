package com.car.convoy.ui

import androidx.compose.ui.graphics.Color
import com.car.convoy.domain.GroupMemberState
import com.car.convoy.domain.UserStatus

/**
 * 화면 공용 색. Pleos UI 가이드의 대비 4.5:1 이상을 지키는 값이다(PaletteContrastTest, docs/sdk-findings.md).
 * 글자색은 흰 카드, 회색 바탕, 같은 색 12% 칩 위에서 모두 4.5:1 이상이다.
 */
internal object Palette {
    /** 아이콘과 앱의 브랜드 색: 네이비 → 블루 → 시안. */
    val BrandNavy = Color(0xFF071B64)
    val BrandBlue = Color(0xFF086AB5)
    val BrandCyan = Color(0xFF17CFE0)
    val BrandCyanFill = Color(0xFFD7F5F7)

    val Page = Color(0xFFF2F4F7)
    val Card = Color.White
    val Ink = Color(0xFF10233F)
    /** 보조 글자. 이전 값 #5E6978은 회색 바탕 칩에서 4.3:1이었다. */
    val Soft = Color(0xFF526273)
    /**
     * 글자·버튼 강조색. 브랜드 블루(#086AB5)는 회색 바탕 칩에서 4.3:1이라 같은 계열의 더 진한 값을 쓴다(4.9:1, PaletteContrastTest).
     * 아이콘·장식에는 BrandBlue를 그대로 쓴다.
     */
    val Accent = Color(0xFF0761A6)
    val Danger = Color(0xFFB3261E)
    val DangerSoft = Color(0xFFFBE9E7)

    /** 시연용 표시와 "경로 찾는 중". 이전 값 #B7791F는 칩에서 3.2:1, #8A5A0F는 가족 알림 카드 위 칩에서 4.3:1이었다. */
    val Demo = Color(0xFF805310)
    /** 도착·활동 중. 이전 값 #1D7A46은 회색 바탕 칩에서 4.1:1이었다. */
    val Done = Color(0xFF186B3D)

    /** "주행 중". 이전 값 #1F6FD1은 칩에서 4.2:1이었다. */
    val Driving = Color(0xFF006B85)
    val ButtonFill = Color(0xFFE8F3FC)
    val ButtonLine = Color(0xFFB9D3E8)
    val DoneFill = Color(0xFFDFF2E6)
    /** 비활성 버튼 바탕. 투명도로 흐리지 않고 이 바탕과 Soft 글자로 표시해 글자 대비를 지킨다. */
    val DisabledFill = Color(0xFFDDE1E7)
    val FamilyText = Color(0xFF0F5A36)
    val FamilyFill = Color(0xFFE3F4EA)
    val PendingText = Color(0xFF8A4B00)
    val PendingFill = Color(0xFFFFF1DC)
    val MemberText = BrandBlue
    val MemberFill = Color(0xFFE5F2FB)
}

/** R11-20 멤버 상태 색. "출발 전"은 이전 #7D8796(칩 3.2:1) 대신 보조 글자색을 쓴다. */
internal fun memberStateColor(state: GroupMemberState): Color = when (state) {
    GroupMemberState.IDLE -> Palette.Soft
    GroupMemberState.REROUTING -> Palette.Demo
    GroupMemberState.DRIVING -> Palette.Driving
    GroupMemberState.ARRIVED -> Palette.Done
}

/** R2-5 상태 색: 활동 중 초록, 업무 중 빨강, 오프라인(내 상태 버튼에만 나옴) 회색. */
internal fun userStatusColor(status: UserStatus): Color = when (status) {
    UserStatus.ACTIVE -> Palette.Done
    UserStatus.BUSY -> Palette.Danger
    UserStatus.OFFLINE -> Palette.Soft
}
