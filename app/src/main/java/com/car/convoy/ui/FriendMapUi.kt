package com.car.convoy.ui

import com.car.convoy.domain.Friend
import com.car.convoy.domain.FriendMarker
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.friendMarkerKind
import com.car.convoy.domain.visibleInFriendList

/**
 * SPEC R14-12: [친구 지도] 탭이 그릴 값. "나" 마커(내 위치가 있을 때)를 먼저, 그 뒤 친구를 [people] 순서로 한 사람에 하나씩 둔다.
 * 선·핀·안내 문구는 없고, 카메라는 마커가 처음 생길 때 마커에 맞춘다([GroupMapUi.fitMarkers]).
 * [positions]는 받은 친구 위치, [groupMembers]는 수락한 그룹 멤버다(수락 대기는 넣지 않는다).
 */
fun friendMapUi(
    people: List<Friend>,
    positions: Map<String, GeoPoint>,
    groupMembers: Collection<String>,
    myPoint: GeoPoint?,
): GroupMapUi {
    val friends = people.mapNotNull { p ->
        val position = positions[p.id]
        val tone = when (friendMarkerKind(visibleInFriendList(p.status), position != null, p.id in groupMembers)) {
            FriendMarker.FRIEND -> MarkerTone.GRAY
            FriendMarker.GROUP_MEMBER -> MarkerTone.GROUP
            FriendMarker.NONE -> return@mapNotNull null
        }
        MapMarker(p.id, p.name.take(1), position!!, tone)
    }
    val mine = myPoint?.let { listOf(MapMarker("me", MY_NAME, it, MarkerTone.ME)) }.orEmpty()
    return GroupMapUi(
        lines = emptyList(),
        markers = mine + friends,
        pins = emptyList(),
        banner = null,
        memberLabels = emptyMap(),
        turnCard = null,
        arrivalCard = null,
        myPoint = myPoint,
        fitMarkers = true,
    )
}
