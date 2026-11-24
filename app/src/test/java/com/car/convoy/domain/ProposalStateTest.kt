package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R13-4·5·14 `nextProposalState` 예시표 9행 + 표에 없는 조합(AI 작성). */
class ProposalStateTest {

    private fun t(state: ProposalState, effect: ProposalEffect? = null) = ProposalTransition(state, effect)

    @Test
    fun pending_approve_sends() {
        assertEquals(t(ProposalState.APPROVED, ProposalEffect.SEND_WAYPOINT), nextProposalState(ProposalState.PENDING, ProposalEvent.LEADER_APPROVE))
    }

    @Test
    fun pending_reject() {
        assertEquals(t(ProposalState.REJECTED), nextProposalState(ProposalState.PENDING, ProposalEvent.LEADER_REJECT))
    }

    @Test
    fun pending_cancel() {
        assertEquals(t(ProposalState.CANCELLED), nextProposalState(ProposalState.PENDING, ProposalEvent.PROPOSER_CANCEL))
    }

    @Test
    fun pending_timeout_expires() {
        assertEquals(t(ProposalState.EXPIRED), nextProposalState(ProposalState.PENDING, ProposalEvent.TIMEOUT))
    }

    @Test
    fun pending_session_ended_cancels() {
        assertEquals(t(ProposalState.CANCELLED), nextProposalState(ProposalState.PENDING, ProposalEvent.SESSION_ENDED))
    }

    @Test
    fun approved_reject_ignored() {
        assertEquals(t(ProposalState.APPROVED), nextProposalState(ProposalState.APPROVED, ProposalEvent.LEADER_REJECT))
    }

    @Test
    fun rejected_approve_ignored() {
        assertEquals(t(ProposalState.REJECTED), nextProposalState(ProposalState.REJECTED, ProposalEvent.LEADER_APPROVE))
    }

    @Test
    fun expired_approve_ignored() {
        assertEquals(t(ProposalState.EXPIRED), nextProposalState(ProposalState.EXPIRED, ProposalEvent.LEADER_APPROVE))
    }

    @Test
    fun cancelled_timeout_ignored() {
        assertEquals(t(ProposalState.CANCELLED), nextProposalState(ProposalState.CANCELLED, ProposalEvent.TIMEOUT))
    }

    // 표에 없는 조합은 그대로(Codex 리뷰 예시)
    @Test
    fun approved_timeout_unchanged() {
        assertEquals(t(ProposalState.APPROVED), nextProposalState(ProposalState.APPROVED, ProposalEvent.TIMEOUT))
    }

    @Test
    fun cancelled_approve_unchanged() {
        assertEquals(t(ProposalState.CANCELLED), nextProposalState(ProposalState.CANCELLED, ProposalEvent.LEADER_APPROVE))
    }
}
