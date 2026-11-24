package com.car.convoy.ui

import com.car.convoy.domain.Friend
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC R14-12: [친구 지도] 탭이 그릴 마커(작업 40, AI 작성). */
class FriendMapUiTest {

    private val people = listOf(
        Friend("p1", "민수", UserStatus.ACTIVE),
        Friend("p2", "현우", UserStatus.ACTIVE),
        Friend("p4", "지영", UserStatus.BUSY),
        Friend("p5", "서준", UserStatus.OFFLINE),
    )
    private val me = GeoPoint(37.41, 127.09)
    private val positions = mapOf(
        "p1" to GeoPoint(37.42, 127.10),
        "p4" to GeoPoint(37.40, 127.08),
        "p5" to GeoPoint(37.43, 127.11),
    )

    private fun ui(members: Set<String> = emptySet(), myPoint: GeoPoint? = me) =
        friendMapUi(people, positions, members, myPoint)

    @Test fun me_first_then_located_visible_friends_in_list_order() {
        val markers = ui().markers
        assertEquals(listOf("me", "p1", "p4"), markers.map { it.id })
        assertEquals(listOf("나", "민", "지"), markers.map { it.text })
    }

    @Test fun friend_gray_member_navy() {
        val tones = ui(members = setOf("p1")).markers.associate { it.id to it.tone }
        assertEquals(MarkerTone.ME, tones["me"])
        assertEquals(MarkerTone.GROUP, tones["p1"])
        assertEquals(MarkerTone.GRAY, tones["p4"])
    }

    @Test fun offline_member_not_drawn() =
        assertTrue(ui(members = setOf("p5")).markers.none { it.id == "p5" })

    @Test fun no_my_point_drops_me_marker() {
        val u = ui(myPoint = null)
        assertEquals(listOf("p1", "p4"), u.markers.map { it.id })
        assertNull(u.myPoint)
    }

    @Test fun my_point_kept_for_locate() = assertEquals(me, ui().myPoint)

    @Test fun no_lines_no_pins_no_banner_fits_markers() {
        val u = ui()
        assertTrue(u.lines.isEmpty() && u.pins.isEmpty())
        assertNull(u.banner)
        assertTrue(u.fitMarkers)
    }
}
