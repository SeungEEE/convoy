package com.car.convoy.domain

import com.car.convoy.domain.AppScreen.DRIVE_GROUP
import com.car.convoy.domain.AppScreen.DRIVE_MAP
import com.car.convoy.domain.AppScreen.FRIEND_MAP
import com.car.convoy.domain.AppScreen.KEEP
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R2-5 `screenAfter` 9행 (작업 40, AI 작성). "상관없음"인 행은 목적지 있음·없음을 모두 본다. */
class ScreenAfterTest {

    private fun both(event: ScreenEvent, expected: AppScreen) {
        assertEquals(expected, screenAfter(event, hasDestination = true))
        assertEquals(expected, screenAfter(event, hasDestination = false))
    }

    @Test fun app_start_opens_friend_map() = both(ScreenEvent.APP_START, FRIEND_MAP)

    @Test fun back_from_background_keeps_screen() = both(ScreenEvent.RETURN_FROM_BACKGROUND, KEEP)

    @Test fun start_destination_without_destination_opens_group() =
        assertEquals(DRIVE_GROUP, screenAfter(ScreenEvent.START_DESTINATION, hasDestination = false))

    @Test fun resume_with_destination_opens_map() =
        assertEquals(DRIVE_MAP, screenAfter(ScreenEvent.RESUME_DRIVE, hasDestination = true))

    @Test fun resume_without_destination_opens_group() =
        assertEquals(DRIVE_GROUP, screenAfter(ScreenEvent.RESUME_DRIVE, hasDestination = false))

    @Test fun group_screen_home_opens_friend_map() = both(ScreenEvent.GROUP_SCREEN_HOME, FRIEND_MAP)

    @Test fun end_drive_opens_friend_map() = both(ScreenEvent.END_DRIVE, FRIEND_MAP)

    @Test fun follow_home_opens_friend_map() = both(ScreenEvent.FOLLOW_HOME, FRIEND_MAP)

    @Test fun follow_stop_opens_friend_map() = both(ScreenEvent.FOLLOW_STOP, FRIEND_MAP)
}
