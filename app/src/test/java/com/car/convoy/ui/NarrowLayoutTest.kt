package com.car.convoy.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R11-20: 좁은 창(1,200dp 미만)은 1열. 홈의 1열 순서는 HomeScreenModelTest. */
class NarrowLayoutTest {

    @Test fun width_rule() {
        assertTrue(isSingleColumn(850f))     // 1:1 화면 나누기
        assertTrue(isSingleColumn(1133f))    // 2:1 넓은 쪽
        assertTrue(isSingleColumn(1199.9f))
        assertFalse(isSingleColumn(1200f))   // 경계는 2열
        assertFalse(isSingleColumn(1707f))   // 앱 하나
    }
}
