package com.car.convoy.domain

/** SPEC R4. 초대 가능 여부. */
enum class InviteResult { OK, GROUP_FULL, NOT_FRIEND }

/** SPEC R4-3. 초대 수락 가능 여부. */
enum class AcceptInviteResult { OK, ALREADY_IN_GROUP }

/** SPEC R4-1 (제안): 리더 포함 최대 인원. */
const val MAX_GROUP_SIZE = 5

/** SPEC R4-1, R4-2: 친구 여부를 먼저 보고, 그다음 인원(리더 포함)이 꽉 찼는지 본다. */
fun canInvite(memberCount: Int, isFriend: Boolean): InviteResult = when {
    !isFriend -> InviteResult.NOT_FRIEND
    memberCount >= MAX_GROUP_SIZE -> InviteResult.GROUP_FULL
    else -> InviteResult.OK
}

/** SPEC R4-3: 한 차는 동시에 한 그룹에만 속한다. */
fun canAcceptInvite(currentGroupId: String?): AcceptInviteResult =
    if (currentGroupId == null) AcceptInviteResult.OK else AcceptInviteResult.ALREADY_IN_GROUP
