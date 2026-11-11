package com.car.convoy.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.car.convoy.data.GroupRoster
import com.car.convoy.domain.FollowLink
import com.car.convoy.domain.FollowState
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Codex 리뷰: 따라가기 요청 대기 중 초대·해산 잠금(R4-5), 홈 아바타 색 대비(Pleos 가이드 4.5:1). */
class HomeFollowLockAndAvatarTest {

    private val people = listOf(Friend("p1", "민수", UserStatus.ACTIVE), Friend("p2", "현우", UserStatus.ACTIVE))

    @Test fun follow_waiting_locks_invite_and_disband() {
        val roster = GroupRoster(people).apply { invite("p1", Gear.PARK, false); respond("p1", accept = true) }
        val m = homeScreenModel(people, UserStatus.ACTIVE, roster, driveActive = false, gear = Gear.PARK, follow = FollowLink("p1", FollowState.REQUESTED))
        assertFalse(m.people.first { it.id == "p2" }.canInvite)
        assertFalse(m.canDisband)
    }

    @Test fun avatar_colors_contrast() {
        fun ratio(a: Color, b: Color): Double {
            val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
            return (hi + 0.05) / (lo + 0.05)
        }
        Palette.AvatarColors.forEach { (bg, fg) -> assertTrue("아바타 $fg/$bg", ratio(fg, bg) >= 4.5) }
    }
}
