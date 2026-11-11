package com.car.convoy.ui

import androidx.compose.ui.graphics.Color
import com.car.convoy.domain.GroupMemberState
import com.car.convoy.domain.UserStatus

/**
 * 화면 공용 색. Pleos UI 가이드의 대비 4.5:1 이상을 지키는 값이다(PaletteContrastTest, docs/sdk-findings.md).
 * 글자색은 흰 카드, 회색 바탕, 같은 색 12% 칩 위에서 모두 4.5:1 이상이다.
 */
internal object Palette {
    /** 앱 아이콘 색(네이비 → 블루 → 시안). 아이콘과 테마의 보조 색에만 쓰고, 화면 강조색은 시안 파랑(Brand)을 쓴다. */
    val BrandNavy = Color(0xFF071B64)
    val BrandBlue = Color(0xFF086AB5)
    val BrandCyan = Color(0xFF17CFE0)
    val BrandCyanFill = Color(0xFFD7F5F7)

    /** 화면 강조색: 처음 시안(Convoy 그룹 흐름 시안)의 파랑. 채우면 흰 글자 6.6:1, 글자로 써도 바탕 위 6.0:1. */
    val Brand = Color(0xFF2350D6)

    /** 브랜드 위 보조 글자(5.5:1). */
    val OnBrandSoft = Color(0xFFE3EAFD)

    /** 시안의 짙은 카드(앞차 따라가기, 내비 제안). 짙은 칸과 보조 글자도 시안 값. */
    val Night = Color(0xFF0F1720)
    val NightTile = Color(0xFF1B2533)
    val OnNightSoft = Color(0xFFAEB7C4)

    /** 화면 바탕과 카드(시안). */
    val Page = Color(0xFFF4F5F7)
    val Card = Color.White
    val CardLine = Color(0xFFE3E6EA)
    val Ink = Color(0xFF0F1720)
    /** 보조 글자. 이전 값 #5E6978은 회색 바탕 칩에서 4.3:1이었다. */
    val Soft = Color(0xFF56606D)
    /** 글자·테두리 강조색. 시안대로 브랜드 파랑을 그대로 쓴다(칩에서도 5.0:1). */
    val Accent = Brand
    val Danger = Color(0xFFB3261E)
    val DangerSoft = Color(0xFFFBE9E7)

    /** 시연용 표시와 "경로 찾는 중". 이전 값 #B7791F는 칩에서 3.2:1, #8A5A0F는 가족 알림 카드 위 칩에서 4.3:1이었다. */
    val Demo = Color(0xFF805310)
    /** 도착·활동 중. 이전 값 #1D7A46은 회색 바탕 칩에서 4.1:1이었다. */
    val Done = Color(0xFF186B3D)

    /** "주행 중". 이전 값 #1F6FD1은 칩에서 4.2:1이었다. */
    val Driving = Color(0xFF1B5FB8)
    /** 고를 수 있는 줄의 바탕(시안의 회색 줄)과 테두리 버튼 선. */
    val ButtonFill = Color(0xFFF1F3F6)
    val ButtonLine = Color(0xFFC9D3F3)
    val DoneFill = Color(0xFFDFF2E6)
    /** 비활성 버튼 바탕. 투명도로 흐리지 않고 이 바탕과 Soft 글자로 표시해 글자 대비를 지킨다. */
    val DisabledFill = Color(0xFFDDE1E7)
    val FamilyText = Color(0xFF0F5A36)
    val FamilyFill = Color(0xFFE3F4EA)
    val PendingText = Color(0xFF8A4B00)
    val PendingFill = Color(0xFFFFF1DC)
    val MemberText = Color(0xFF1A3FA8)
    val MemberFill = Color(0xFFE3EAFD)
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
