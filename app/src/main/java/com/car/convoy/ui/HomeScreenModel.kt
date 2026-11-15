package com.car.convoy.ui

import com.car.convoy.data.FamilyController
import com.car.convoy.data.GroupRoster
import com.car.convoy.data.RosterState
import com.car.convoy.domain.DriverAction
import com.car.convoy.domain.FamilyState
import com.car.convoy.domain.FollowLink
import com.car.convoy.domain.FollowRequestResult
import com.car.convoy.domain.FollowState
import com.car.convoy.domain.canRequestFollow
import com.car.convoy.domain.canStartFollowRequest
import com.car.convoy.domain.Friend
import com.car.convoy.domain.Gear
import com.car.convoy.domain.UserStatus
import com.car.convoy.domain.canPerform
import com.car.convoy.domain.visibleInFriendList

/** 연결된 사람 한 줄. */
data class PersonRow(
    val id: String,
    val name: String,
    val status: UserStatus,
    val statusLabel: String,
    /** "수락 대기", "그룹 멤버", 그룹 밖이면 null([그룹에 초대]). */
    val groupLabel: String?,
    val canInvite: Boolean,
    /** R9-5 [가족으로 설정]을 누를 수 있는가. */
    val canDesignateFamily: Boolean = false,
    /** "가족", "가족 수락 대기", 아니면 null. */
    val familyTag: String? = null,
)

/** 함께 출발 카드의 멤버 칩. */
data class DepartureChip(val id: String, val name: String, val isLeader: Boolean, val pending: Boolean)

/** 시연용 패널: 그룹 초대를 받은 가상 인물. */
data class PendingInvite(val id: String, val name: String)

/** SPEC R2-5, R4-5: 홈에 보일 값. Compose 화면은 이 값만 그린다. */
data class HomeScreenModel(
    val myStatus: UserStatus,
    val people: List<PersonRow>,
    val departure: List<DepartureChip>,
    val canStartDestination: Boolean,
    val showResume: Boolean,
    val canDisband: Boolean,
    val pendingInvite: PendingInvite?,
    /** R9-5 홈의 가족 카드. */
    val familyCard: FamilyCard = FamilyCard.NONE,
    /** R9-5 머리줄 표시: "가족 민수" 또는 "가족 없음". */
    val familyHeader: String = NO_FAMILY,
    /** 시연용 패널: 가족 요청을 받은 가상 인물. */
    val pendingFamily: PendingInvite? = null,
    /** R12-1 [앞차 따라가기]: 수락한 그룹 멤버 중 따라갈 사람. */
    val followChoices: List<FollowChoice> = emptyList(),
    /** 따라가기 요청 대기 중이면 "민수님 수락 대기". */
    val followWaiting: String? = null,
    /** 시연용 패널: 따라가기 요청을 받은 가상 멤버. */
    val pendingFollow: PendingInvite? = null,
    /** [진행 중인 주행으로] 아래 문구. */
    val resumeLabel: String = "",
)

/** R12-1: 따라갈 사람 한 명. 업무 중이면 고를 수 없다(R2-3). */
data class FollowChoice(val id: String, val name: String, val canFollow: Boolean, val note: String?)

enum class FamilyCardState { NONE, PENDING, ACTIVE }

/** SPEC R9-5: 홈의 가족 카드. */
data class FamilyCard(
    val state: FamilyCardState,
    val name: String?,
    val message: String,
    val showCancel: Boolean,
    val showRelease: Boolean,
) {
    companion object {
        val NONE = FamilyCard(FamilyCardState.NONE, null, "가족을 정하면 사고가 의심될 때 알려요", showCancel = false, showRelease = false)
    }
}

const val NO_FAMILY = "가족 없음"

/** 홈 1열에서 쌓는 칸(가족 카드는 작업 24에서 맨 앞에 붙는다). */
enum class HomeSlot { DEPARTURE, DEMO, PEOPLE }

const val MY_NAME = "나"

/**
 * SPEC R4-5: 오프라인은 숨기고(R2-2), 그룹 밖이면 [그룹에 초대], 보낸 뒤 "수락 대기", 수락하면 "그룹 멤버".
 * 출발은 수락한 멤버가 1명 이상이고 정차 중이며 주행이 "없음"일 때만 된다.
 */
fun homeScreenModel(
    people: List<Friend>,
    myStatus: UserStatus,
    roster: GroupRoster,
    driveActive: Boolean,
    gear: Gear,
    family: FamilyController? = null,
    follow: FollowLink? = null,
): HomeScreenModel {
    // R4-5, R12-5: 주행은 한 번에 하나다. 따라가기 요청 대기(REQUESTED)도 진행 중으로 본다.
    val liveFollow = follow?.takeIf { it.state != FollowState.ENDED }
    val anyDrive = driveActive || liveFollow != null
    val names = people.associate { it.id to it.name }
    val rows = people.filter { visibleInFriendList(it.status) }.map { p ->
        PersonRow(
            id = p.id,
            name = p.name,
            status = p.status,
            statusLabel = userStatusLabel(p.status),
            groupLabel = when (roster.stateOf(p.id)) {
                RosterState.INVITED -> "수락 대기"
                RosterState.MEMBER -> "그룹 멤버"
                null -> null
            },
            canInvite = roster.canInviteNow(p.id, gear, anyDrive),
            canDesignateFamily = family?.canDesignate(p.id, gear) ?: false,
            familyTag = family?.takeIf { it.familyId == p.id }?.let {
                when (it.state) {
                    FamilyState.PENDING -> "가족 수락 대기"
                    FamilyState.ACTIVE -> "가족"
                    FamilyState.NONE -> null
                }
            },
        )
    }
    val chips = listOf(DepartureChip("me", MY_NAME, isLeader = true, pending = false)) +
        roster.members.map { DepartureChip(it, names[it] ?: it, isLeader = false, pending = false) } +
        roster.pendingInOrder.map { DepartureChip(it, names[it] ?: it, isLeader = false, pending = true) }
    return HomeScreenModel(
        myStatus = myStatus,
        people = rows,
        departure = chips,
        canStartDestination = !anyDrive && roster.members.isNotEmpty() &&
            canPerform(DriverAction.SET_GROUP_DESTINATION, gear),
        showResume = driveActive || liveFollow?.state == FollowState.ACTIVE,
        canDisband = roster.canDisbandNow(gear, anyDrive),
        pendingInvite = roster.firstPendingInvite?.let { PendingInvite(it, names[it] ?: it) },
        familyCard = familyCard(family),
        familyHeader = familyHeader(family),
        pendingFamily = family?.takeIf { it.state == FamilyState.PENDING }?.familyId?.let { PendingInvite(it, names[it] ?: it) },
        followChoices = roster.members.map { id ->
            val person = people.first { it.id == id }
            FollowChoice(
                id = id,
                name = person.name,
                canFollow = canStartFollowRequest(person.status, gear, followInProgress = anyDrive),
                note = if (canRequestFollow(person.status) == FollowRequestResult.REJECT_BUSY) "업무 중이라 따라갈 수 없어요" else null,
            )
        },
        followWaiting = liveFollow?.takeIf { it.state == FollowState.REQUESTED }?.let { "${names[it.friendId] ?: it.friendId}님 수락 대기" },
        pendingFollow = liveFollow?.takeIf { it.state == FollowState.REQUESTED }?.let { PendingInvite(it.friendId, names[it.friendId] ?: it.friendId) },
        resumeLabel = when {
            liveFollow?.state == FollowState.ACTIVE -> "${names[liveFollow.friendId] ?: liveFollow.friendId} 차를 따라가는 중이에요"
            driveActive -> "목적지로 함께 가는 중이에요"
            else -> ""
        },
    )
}

/**
 * SPEC R4-5: 1열 순서. 함께 출발, 시연용 패널(응답할 초대나 가족 요청이 있을 때), 연결된 사람.
 * 가족 카드(R9-5)는 항상 있으므로 화면이 이 순서 앞에 둔다.
 */
fun homeSingleColumnOrder(model: HomeScreenModel): List<HomeSlot> = buildList {
    add(HomeSlot.DEPARTURE)
    if (model.pendingInvite != null || model.pendingFamily != null || model.pendingFollow != null) add(HomeSlot.DEMO)
    add(HomeSlot.PEOPLE)
}

/** SPEC R9-5: 가족 카드 문구와 버튼. */
fun familyCard(family: FamilyController?): FamilyCard {
    val name = family?.familyName
    return when (family?.state) {
        FamilyState.PENDING -> FamilyCard(FamilyCardState.PENDING, name, "${name}님 수락 대기", showCancel = true, showRelease = false)
        FamilyState.ACTIVE -> FamilyCard(FamilyCardState.ACTIVE, name, "사고가 의심되면 ${name}님에게 알려요", showCancel = false, showRelease = true)
        else -> FamilyCard.NONE
    }
}

/** SPEC R9-5: 머리줄 가족 표시. 등록된(ACTIVE) 가족만 이름으로 보여 준다. */
fun familyHeader(family: FamilyController?): String =
    family?.takeIf { it.state == FamilyState.ACTIVE }?.familyName?.let { "가족 $it" } ?: NO_FAMILY
