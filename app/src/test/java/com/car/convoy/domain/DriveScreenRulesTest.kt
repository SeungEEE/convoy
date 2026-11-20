package com.car.convoy.domain

import com.car.convoy.domain.InfoCardSection.ARRIVAL
import com.car.convoy.domain.InfoCardSection.FINISHED
import com.car.convoy.domain.InfoCardSection.ME_UNKNOWN
import com.car.convoy.domain.InfoCardSection.MEMBERS
import com.car.convoy.domain.InfoCardSection.REQUEST
import com.car.convoy.domain.InfoCardSection.WAYPOINT
import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R14-18 `driveEntryScreen` 2행, R14-19 `infoCardSections` 13행 (작업 39, AI 작성). */
class DriveScreenRulesTest {

    @Test fun entry_with_destination_opens_map() {
        assertEquals(DriveView.MAP, driveEntryScreen(hasDestination = true))
    }

    @Test fun entry_without_destination_opens_group() {
        assertEquals(DriveView.GROUP, driveEntryScreen(hasDestination = false))
    }

    private fun sections(
        finished: Boolean, request: Boolean, waypoint: Boolean, route: Boolean,
        meUnknown: Boolean, members: Int, arrival: Boolean,
    ) = infoCardSections(
        finished = finished, hasRequest = request, hasWaypointStatus = waypoint, hasRoute = route,
        meUnknown = meUnknown, memberRows = members, hasArrival = arrival,
    )

    @Test fun members_and_arrival() {
        assertEquals(listOf(MEMBERS, ARRIVAL), sections(false, false, false, true, false, 2, true))
    }

    @Test fun nothing_hides_card() {
        assertEquals(emptyList<InfoCardSection>(), sections(false, false, false, true, false, 0, false))
    }

    @Test fun request_first() {
        assertEquals(listOf(REQUEST, MEMBERS, ARRIVAL), sections(false, true, false, true, false, 2, true))
    }

    @Test fun request_drops_waypoint_status() {
        assertEquals(listOf(REQUEST, MEMBERS, ARRIVAL), sections(false, true, true, true, false, 2, true))
    }

    @Test fun waypoint_status_without_request() {
        assertEquals(listOf(WAYPOINT, MEMBERS, ARRIVAL), sections(false, false, true, true, false, 2, true))
    }

    @Test fun me_unknown_replaces_members() {
        assertEquals(listOf(ME_UNKNOWN, ARRIVAL), sections(false, false, false, true, true, 2, true))
    }

    @Test fun me_unknown_without_members() {
        assertEquals(listOf(ME_UNKNOWN), sections(false, false, false, true, true, 0, false))
    }

    @Test fun no_route_no_me_unknown() {
        assertEquals(emptyList<InfoCardSection>(), sections(false, false, false, false, true, 0, false))
    }

    @Test fun no_route_no_members() {
        assertEquals(listOf(ARRIVAL), sections(false, false, false, false, false, 2, true))
    }

    @Test fun alone_only_arrival() {
        assertEquals(listOf(ARRIVAL), sections(false, false, false, true, false, 0, true))
    }

    @Test fun full_order() {
        assertEquals(listOf(WAYPOINT, ME_UNKNOWN, ARRIVAL), sections(false, false, true, true, true, 2, true))
    }

    @Test fun finished_drops_everything_else() {
        assertEquals(listOf(FINISHED), sections(true, true, true, true, true, 2, true))
    }

    @Test fun finished_alone() {
        assertEquals(listOf(FINISHED), sections(true, false, false, false, false, 0, false))
    }
}
