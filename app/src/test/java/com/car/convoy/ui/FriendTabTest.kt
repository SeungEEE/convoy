package com.car.convoy.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R2-5: 화면 위 [친구] [그룹 주행] 탭. */
class FriendTabTest {
    @Test fun tab_labels_in_order() {
        assertEquals(listOf("친구", "그룹 주행"), AppTab.entries.map { it.label })
    }
}
