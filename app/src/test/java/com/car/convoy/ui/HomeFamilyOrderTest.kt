package com.car.convoy.ui

import com.car.convoy.data.FamilyController
import com.car.convoy.data.GroupRoster
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R4-5, R9-5, Codex 리뷰: 가족 요청도 시연 패널 칸을 만든다(가족 카드는 화면이 항상 맨 앞에 둔다). */
class HomeFamilyOrderTest {

    private val people = listOf(Friend("p1", "민수", UserStatus.ACTIVE), Friend("p2", "현우", UserStatus.ACTIVE))

    private fun order(familyPending: Boolean, invitePending: Boolean): List<HomeSlot> {
        val family = FamilyController(people).apply { if (familyPending) designate("p1", Gear.PARK) }
        val roster = GroupRoster(people).apply { if (invitePending) invite("p2", Gear.PARK, false) }
        return homeSingleColumnOrder(homeScreenModel(people, UserStatus.ACTIVE, roster, false, Gear.PARK, family))
    }

    @Test fun family_request_only() =
        assertEquals(listOf(HomeSlot.DEPARTURE, HomeSlot.DEMO, HomeSlot.PEOPLE), order(familyPending = true, invitePending = false))

    @Test fun both_requests() =
        assertEquals(listOf(HomeSlot.DEPARTURE, HomeSlot.DEMO, HomeSlot.PEOPLE), order(familyPending = true, invitePending = true))

    @Test fun no_request() =
        assertEquals(listOf(HomeSlot.DEPARTURE, HomeSlot.PEOPLE), order(familyPending = false, invitePending = false))
}
