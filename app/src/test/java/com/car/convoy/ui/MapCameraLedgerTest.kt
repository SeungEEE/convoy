package com.car.convoy.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC R14-8·16·17, 작업 39 Codex 리뷰 반영(AI 작성): 카메라 명령은 지도(MapView)보다 오래 사는 장부가 정한다.
 * 새 길찾기 결과면 한 번만 맞추고, 화면을 오가 지도가 다시 만들어지면 맞추지 않고 떠날 때의 위치로 되돌린다.
 * [C]는 카메라 위치(SDK 값)이고 여기서는 문자열로 대신한다.
 */
class MapCameraLedgerTest {

    @Test fun first_route_fits_once() {
        val ledger = MapCameraLedger<String>()
        assertEquals(CameraAction.FIT_ROUTE, ledger.next(routeVersion = 1, hasRoute = true, fresh = true))
        assertEquals(CameraAction.NONE, ledger.next(routeVersion = 1, hasRoute = true, fresh = false))
    }

    @Test fun no_route_no_fit() {
        val ledger = MapCameraLedger<String>()
        assertEquals(CameraAction.NONE, ledger.next(routeVersion = 1, hasRoute = false, fresh = true))
    }

    @Test fun new_route_version_fits_again() {
        val ledger = MapCameraLedger<String>()
        ledger.next(1, true, fresh = true)
        assertEquals(CameraAction.FIT_ROUTE, ledger.next(2, true, fresh = false))
    }

    @Test fun recreated_map_with_same_route_restores_saved_camera() {
        val ledger = MapCameraLedger<String>()
        ledger.next(1, true, fresh = true)
        ledger.save("사용자가 옮긴 자리")
        assertEquals(CameraAction.RESTORE, ledger.next(1, true, fresh = true))
        assertEquals("사용자가 옮긴 자리", ledger.saved)
    }

    @Test fun recreated_map_without_saved_camera_does_nothing() {
        val ledger = MapCameraLedger<String>()
        ledger.next(1, true, fresh = true)
        assertEquals(CameraAction.NONE, ledger.next(1, true, fresh = true))
    }

    @Test fun recreated_map_with_new_route_fits_instead_of_restoring() {
        val ledger = MapCameraLedger<String>()
        ledger.next(1, true, fresh = true)
        ledger.save("예전 자리")
        assertEquals(CameraAction.FIT_ROUTE, ledger.next(2, true, fresh = true))
    }

    @Test fun same_map_update_never_restores() {
        val ledger = MapCameraLedger<String>()
        ledger.next(1, true, fresh = true)
        ledger.save("자리")
        assertEquals(CameraAction.NONE, ledger.next(1, true, fresh = false))
    }

    @Test fun locate_once_per_press() {
        val ledger = MapCameraLedger<String>()
        assertFalse(ledger.locate(0))
        assertTrue(ledger.locate(1))
        assertFalse(ledger.locate(1))
        assertTrue(ledger.locate(2))
    }

    @Test fun locate_press_before_ledger_is_not_replayed() {
        // 장부를 만들 때의 번호는 이미 처리한 것으로 본다(지도가 뜰 때 저절로 옮기지 않게, R14-8).
        val ledger = MapCameraLedger<String>(initialLocateVersion = 3)
        assertFalse(ledger.locate(3))
        assertTrue(ledger.locate(4))
    }

    // ---- [내 위치]를 누를 수 있는 때(R14-16) ----

    @Test fun locate_enabled_when_located_this_session_and_map_ready() {
        assertTrue(locateEnabled(driveStartedAt = 100L, myLocationAt = 150L, mapReady = true, hasMyPoint = true))
    }

    @Test fun locate_disabled_when_map_not_ready_or_failed() {
        assertFalse(locateEnabled(100L, 150L, mapReady = false, hasMyPoint = true))
    }

    @Test fun locate_disabled_when_location_is_from_before_session() {
        assertFalse(locateEnabled(100L, 99L, mapReady = true, hasMyPoint = true))
    }

    @Test fun locate_disabled_without_session_or_location() {
        assertFalse(locateEnabled(null, 150L, true, true))
        assertFalse(locateEnabled(100L, null, true, true))
        assertFalse(locateEnabled(100L, 150L, true, hasMyPoint = false))
    }
}
