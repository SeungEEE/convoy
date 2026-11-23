package com.car.convoy.domain

/** SPEC R2-5: 첫 화면의 탭. 선언 순서가 머리줄 순서다. */
enum class HomeTab { FRIEND_MAP, GROUP, FAMILY, SETTINGS }

/** SPEC R2-5 `screenAfter`의 이벤트. */
enum class ScreenEvent {
    APP_START,
    /** 백그라운드에서 돌아옴(내비를 띄웠다 돌아옴 포함). */
    RETURN_FROM_BACKGROUND,
    /** [목적지로 함께]. */
    START_DESTINATION,
    /** 함께 주행의 [진행 중인 주행으로]. 앞차 따라가기는 R12 그대로라 여기에 없다. */
    RESUME_DRIVE,
    /** 그룹 화면 [‹](홈으로). */
    GROUP_SCREEN_HOME,
    /** [주행 끝내기]. */
    END_DRIVE,
    /** 앞차 따라가기 [홈으로]. */
    FOLLOW_HOME,
    /** 앞차 따라가기 [끊기]. */
    FOLLOW_STOP,
}

/** SPEC R2-5: 이벤트 뒤에 여는 화면. [KEEP]은 지금 화면을 그대로 둔다. */
enum class AppScreen { KEEP, FRIEND_MAP, DRIVE_MAP, DRIVE_GROUP }

/** SPEC R2-5 `screenAfter`. 함께 주행으로 들어가는 두 이벤트는 R14-18의 [driveEntryScreen]을 따른다. */
fun screenAfter(event: ScreenEvent, hasDestination: Boolean): AppScreen = when (event) {
    ScreenEvent.RETURN_FROM_BACKGROUND -> AppScreen.KEEP
    ScreenEvent.START_DESTINATION, ScreenEvent.RESUME_DRIVE -> when (driveEntryScreen(hasDestination)) {
        DriveView.MAP -> AppScreen.DRIVE_MAP
        DriveView.GROUP -> AppScreen.DRIVE_GROUP
    }
    ScreenEvent.APP_START, ScreenEvent.GROUP_SCREEN_HOME, ScreenEvent.END_DRIVE,
    ScreenEvent.FOLLOW_HOME, ScreenEvent.FOLLOW_STOP -> AppScreen.FRIEND_MAP
}

/** SPEC R14-12: 친구 마커. [FRIEND]는 회색, [GROUP_MEMBER]는 남색, [NONE]은 찍지 않는다. */
enum class FriendMarker { FRIEND, GROUP_MEMBER, NONE }

/** SPEC R14-12 `friendMarkerKind`. 목록에 없거나(오프라인) 위치를 못 받았으면 찍지 않는다. 수락 대기는 그룹 멤버가 아니다. */
fun friendMarkerKind(visible: Boolean, hasLocation: Boolean, isGroupMember: Boolean): FriendMarker = when {
    !visible || !hasLocation -> FriendMarker.NONE
    isGroupMember -> FriendMarker.GROUP_MEMBER
    else -> FriendMarker.FRIEND
}

/**
 * SPEC R14-12 `friendLocateEnabled`: [친구 지도] 탭을 이번에 연 시각 이후(같은 시각 포함)에 내 위치를 받았고,
 * 지도가 준비되어 있고(오류 없음), 내 위치 값이 있을 때만 true다. 주행 세션은 보지 않는다.
 */
fun friendLocateEnabled(tabOpenedAt: Long, myLocationAt: Long?, mapReady: Boolean, hasMyPoint: Boolean): Boolean =
    myLocationAt != null && myLocationAt >= tabOpenedAt && mapReady && hasMyPoint
