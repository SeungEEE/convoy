package com.car.convoy.data

import com.car.convoy.domain.Destination
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.port.ConvoyServer
import com.car.convoy.domain.port.LocationPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Codex 리뷰: 가족 상태를 바깥(FamilyController)에서 읽을 때는 여기서 끄지 않는다. 해제는 FamilyController.release()로만 한다. */
class AccidentFamilySourceGuardTest {

    private object NoServer : ConvoyServer {
        override fun sendDestination(memberId: String, destination: Destination) {}
    }

    private object NoLocation : LocationPort {
        override fun requestLocation() {}
    }

    @Test fun disable_is_refused_with_external_source() {
        val c = AccidentController(NoServer, NoLocation, FakeScheduler(), FamilyState.NONE, familySource = { FamilyState.ACTIVE })
        assertThrows(IllegalStateException::class.java) { c.disableFamilySharing() }
        assertEquals(FamilyState.ACTIVE, c.familyState)
    }

    @Test fun disable_still_works_without_source() {
        val c = AccidentController(NoServer, NoLocation, FakeScheduler(), FamilyState.ACTIVE)
        c.disableFamilySharing()
        assertEquals(FamilyState.NONE, c.familyState)
    }
}
