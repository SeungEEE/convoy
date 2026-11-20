package com.car.convoy.ui

/** SPEC R14-8·17: 지도가 할 카메라 명령. */
enum class CameraAction { NONE, FIT_ROUTE, RESTORE }

/**
 * SPEC R14-8·16·17: 카메라 명령을 처리했는지 기억하는 장부. 지도(MapView)는 화면을 오갈 때 다시 만들어지므로
 * 장부는 주행 세션 동안 지도보다 오래 산다(작업 39 Codex 리뷰).
 * - 새 길찾기 결과(경로 번호가 바뀜)면 한 번만 경로에 맞춘다.
 * - 같은 경로로 지도를 다시 만들면 맞추지 않고, 떠날 때 저장한 카메라 위치로 되돌린다(사용자가 옮긴 자리 유지).
 * - [내 위치]는 누를 때마다 한 번만 옮긴다. 장부를 만들 때의 번호는 이미 처리한 것으로 본다.
 * [C]는 카메라 위치(SDK의 CameraPosition)다. 모든 메서드는 같은 스레드(메인)에서 부른다.
 */
class MapCameraLedger<C>(initialLocateVersion: Int = 0) {
    private var fittedRoute = -1
    private var handledLocate = initialLocateVersion

    /** 마지막으로 저장한 카메라 위치. */
    var saved: C? = null
        private set

    /** [fresh]는 지도가 방금 준비됐는지(처음 또는 다시 만들어짐)다. */
    fun next(routeVersion: Int, hasRoute: Boolean, fresh: Boolean): CameraAction = when {
        hasRoute && routeVersion != fittedRoute -> {
            fittedRoute = routeVersion
            CameraAction.FIT_ROUTE
        }
        fresh && saved != null -> CameraAction.RESTORE
        else -> CameraAction.NONE
    }

    fun save(camera: C) {
        saved = camera
    }

    /** [내 위치]의 누른 번호가 처음 보는 것이면 true(그때 한 번 옮긴다). */
    fun locate(locateVersion: Int): Boolean {
        if (locateVersion == handledLocate) return false
        handledLocate = locateVersion
        return true
    }
}

/**
 * SPEC R14-16 [내 위치]를 누를 수 있는 때: 이번 주행 세션에서 내 위치를 받았고, 지도가 준비되어 있고(오류 없음), 내 위치 값이 있을 때.
 */
fun locateEnabled(driveStartedAt: Long?, myLocationAt: Long?, mapReady: Boolean, hasMyPoint: Boolean): Boolean =
    driveStartedAt != null && myLocationAt != null && myLocationAt >= driveStartedAt && mapReady && hasMyPoint
