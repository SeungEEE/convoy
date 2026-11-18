package com.car.convoy.data

import com.car.convoy.domain.ApplyEvent
import com.car.convoy.domain.ApplyState
import com.car.convoy.domain.ApplySummary
import com.car.convoy.domain.Destination
import com.car.convoy.domain.GroupMemberState
import com.car.convoy.domain.GroupSession
import com.car.convoy.domain.ProposalCheck
import com.car.convoy.domain.ProposalEffect
import com.car.convoy.domain.ProposalEvent
import com.car.convoy.domain.ProposalState
import com.car.convoy.domain.applySummary
import com.car.convoy.domain.checkProposal
import com.car.convoy.domain.distanceMeters
import com.car.convoy.domain.nextApplyState
import com.car.convoy.domain.nextProposalState
import com.car.convoy.domain.port.ConvoyServer

/** SPEC R13-4: 제안을 만든 시각부터 60초 이상 지나면 만료한다. (제안) */
private const val PROPOSAL_TIMEOUT_MILLIS = 60_000L

/** SPEC R13-6·9: addWaypoint를 호출한 시각부터 15초가 반영 대기와 취소 무시의 기준이다. (제안) */
private const val APPLY_TIMEOUT_MILLIS = 15_000L

/** 제안 하나. [state]만 바뀐다. */
data class WaypointProposal(val proposerId: String, val place: Destination, val state: ProposalState)

/**
 * 작업 31, SPEC R13: 그룹 세션 하나의 경유지 제안·승인·반영.
 * 판단은 domain의 [checkProposal], [nextProposalState], [nextApplyState], [applySummary]가 하고,
 * 이 클래스는 시간(만료 60초, 반영 15초)과 전달(서버)을 잇는다. 모든 메서드는 같은 스레드에서 부른다.
 * [myId]는 이 앱의 차이고 리더다(R11 그룹 주행과 같다).
 */
class WaypointController(
    private val myId: String,
    private val session: GroupSession,
    private val server: ConvoyServer,
    private val scheduler: Scheduler,
    private val onChange: () -> Unit = {},
) {
    /** 가장 최근 제안(어떤 상태든). 없으면 null. */
    var proposal: WaypointProposal? = null
        private set

    private val approved = mutableListOf<Destination>()

    /** SPEC R13-2·10: 그룹이 승인한 경유지(요청한 값). */
    val waypoints: List<Destination>
        get() = approved.toList()

    private val apply = linkedMapOf<String, ApplyState>()

    /** SPEC R13-6: 가장 최근 경유지의 멤버별 반영 상태. */
    val applyStates: Map<String, ApplyState>
        get() = apply.toMap()

    private val failuresByIndex = mutableMapOf<Int, Set<String>>()

    /**
     * SPEC R13-7: 이전 경유지(번호 0부터)에 반영되지 못한 멤버. 다음 경유지를 보내면 이전 경유지는 다시 보내지 않으므로,
     * 그 실패를 여기에 남겨 화면에서 사라지지 않게 한다(Codex 리뷰).
     */
    val earlierFailures: Map<Int, Set<String>>
        get() = failuresByIndex.toMap()

    /** SPEC R13-7: 반영 n/m. */
    val summary: ApplySummary
        get() = applySummary(apply.values.toList())

    private var proposalTimer: Cancellable? = null
    private val applyTimers = mutableMapOf<String, Cancellable>()
    private var myLastAddAt: Long? = null
    private var closed = false

    /** SPEC R13-1~4: 제안한다. OK가 아니면 아무것도 바꾸지 않는다. 리더의 제안은 바로 보낸다. */
    fun propose(proposerId: String, place: Destination): ProposalCheck {
        if (closed) return ProposalCheck.NOT_ACTIVE
        val destination = session.destination
        val result = checkProposal(
            group = session.status,
            hasDestination = destination != null,
            waypointCount = approved.size,
            toDestination = destination?.let { distanceMeters(it, place) },
            toNearestWaypoint = approved.minOfOrNull { distanceMeters(it, place) },
            hasPending = proposal?.state == ProposalState.PENDING,
            applying = apply.values.any { it == ApplyState.SENT },
        )
        if (result != ProposalCheck.OK) return result
        if (proposerId == myId) {
            proposal = WaypointProposal(proposerId, place, ProposalState.APPROVED)
            send(place)
        } else {
            proposal = WaypointProposal(proposerId, place, ProposalState.PENDING)
            proposalTimer = scheduler.schedule(PROPOSAL_TIMEOUT_MILLIS) { onProposalEvent(ProposalEvent.TIMEOUT) }
        }
        onChange()
        return result
    }

    /** SPEC R13-5: 리더가 승인한다. PENDING이 아니면 무시한다. */
    fun approve() = onProposalEvent(ProposalEvent.LEADER_APPROVE)

    fun reject() = onProposalEvent(ProposalEvent.LEADER_REJECT)

    /** 제안한 사람이 취소한다. */
    fun cancel() = onProposalEvent(ProposalEvent.PROPOSER_CANCEL)

    /** SPEC R13-6: 그 차의 내비에서 onWaypointChanged(ADD)가 왔다. */
    fun onWaypointAdded(memberId: String) = onApplyEvent(memberId, ApplyEvent.WAYPOINT_ADDED)

    /** SPEC R13-6: 그 차의 addWaypoint에서 예외가 났다. */
    fun onAddFailed(memberId: String) = onApplyEvent(memberId, ApplyEvent.ADD_FAILED)

    /** SPEC R13-7: 가장 최근 경유지에 실패한 멤버에게만 다시 보낸다. */
    fun resend() {
        if (closed) return
        val place = approved.lastOrNull() ?: return
        apply.filterValues { it == ApplyState.FAILED }.keys.forEach { memberId ->
            apply[memberId] = nextApplyState(ApplyState.FAILED, ApplyEvent.RESEND)
            sendTo(memberId, place)
        }
        onChange()
    }

    /** SPEC R13-14: 세션이 끝났다. 대기 중 제안은 취소하고, 반영 상태와 예약을 버리고, 이후 이벤트는 무시한다. */
    fun close() {
        if (closed) return
        proposal?.let { proposal = it.copy(state = nextProposalState(it.state, ProposalEvent.SESSION_ENDED).state) }
        proposalTimer?.cancel()
        applyTimers.values.forEach { it.cancel() }
        applyTimers.clear()
        apply.clear()
        closed = true
        onChange()
    }

    /** SPEC R13-8: 경로 시작 이름이 목적지이거나, 요청한 경유지(반영 상태와 상관없이)이면 받는다. */
    fun isRouteStartedName(name: String?): Boolean =
        name != null && (name == session.destination?.name || approved.any { it.name == name })

    /**
     * SPEC R13-9: 내 차에 addWaypoint를 호출한 시각부터 15초 미만이면 onRouteCancelled는 경유지 변경 때문이다.
     * [at]은 SDK 콜백을 받은 시각이다(메인 스레드 처리 지연과 무관하게, Codex 리뷰).
     */
    fun isExpectedRouteCancel(at: Long = scheduler.nowMillis()): Boolean {
        val addAt = myLastAddAt ?: return false
        return at - addAt in 0 until APPLY_TIMEOUT_MILLIS
    }

    private fun onProposalEvent(event: ProposalEvent) {
        if (closed) return
        val current = proposal ?: return
        val transition = nextProposalState(current.state, event)
        if (transition.state == current.state) return
        proposal = current.copy(state = transition.state)
        proposalTimer?.cancel()
        proposalTimer = null
        if (transition.effect == ProposalEffect.SEND_WAYPOINT) send(current.place)
        onChange()
    }

    /** SPEC R13-5·6: ARRIVED가 아닌 멤버(리더 포함)에게 보내고 각자 SENT로 둔다. 이전 경유지의 반영 상태는 버린다. */
    private fun send(place: Destination) {
        // 이전 경유지에 실패한 멤버를 남긴다(R13-7). APPLYING이면 제안이 막히므로 여기서 SENT는 없다.
        val failed = apply.filterValues { it == ApplyState.FAILED }.keys
        if (approved.isNotEmpty() && failed.isNotEmpty()) failuresByIndex[approved.lastIndex] = failed.toSet()
        approved += place
        applyTimers.values.forEach { it.cancel() }
        applyTimers.clear()
        apply.clear()
        session.memberIds.filter { session.memberState(it) != GroupMemberState.ARRIVED }.forEach { memberId ->
            apply[memberId] = ApplyState.SENT
            sendTo(memberId, place)
        }
    }

    private fun sendTo(memberId: String, place: Destination) {
        applyTimers.remove(memberId)?.cancel()
        applyTimers[memberId] = scheduler.schedule(APPLY_TIMEOUT_MILLIS) {
            applyTimers.remove(memberId)
            onApplyEvent(memberId, ApplyEvent.TIMEOUT)
        }
        if (memberId == myId) myLastAddAt = scheduler.nowMillis()
        // 예외로 실패를 바로 알리는 어댑터가 onAddFailed를 부를 수 있으니, 상태를 정한 뒤에 보낸다.
        server.sendWaypoint(memberId, place)
    }

    private fun onApplyEvent(memberId: String, event: ApplyEvent) {
        if (closed) return
        val current = apply[memberId] ?: return
        val next = nextApplyState(current, event)
        if (next == current) return
        apply[memberId] = next
        if (next != ApplyState.SENT) applyTimers.remove(memberId)?.cancel()
        onChange()
    }
}
