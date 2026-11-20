package com.car.convoy.domain

/** SPEC R14-9·18: 함께 주행의 두 화면. [MAP]은 지도가 다 채우는 주행 화면, [GROUP]은 지도 없는 그룹 화면이다. */
enum class DriveView { MAP, GROUP }

/** SPEC R14-18: 홈에서 함께 주행으로 들어올 때 여는 화면. 그 뒤에는 사용자가 누를 때만 바뀐다. */
fun driveEntryScreen(hasDestination: Boolean): DriveView =
    if (hasDestination) DriveView.MAP else DriveView.GROUP

/** SPEC R14-19: 주행 화면 정보 카드의 칸. 선언 순서가 위에서부터 쌓는 순서다. */
enum class InfoCardSection { FINISHED, REQUEST, WAYPOINT, MEMBERS, ME_UNKNOWN, ARRIVAL }

/**
 * SPEC R14-19: 정보 카드에 쌓을 칸. 종료면 [InfoCardSection.FINISHED]만 둔다. 요청이 있으면 경유지 상태 칸은 뺀다.
 * 경로가 없으면 멤버 줄과 "내 위치 확인 중"을 모두 뺀다. [memberRows]는 위치를 받은 다른 멤버 수다.
 */
fun infoCardSections(
    finished: Boolean,
    hasRequest: Boolean,
    hasWaypointStatus: Boolean,
    hasRoute: Boolean,
    meUnknown: Boolean,
    memberRows: Int,
    hasArrival: Boolean,
): List<InfoCardSection> {
    if (finished) return listOf(InfoCardSection.FINISHED)
    return buildList {
        if (hasRequest) add(InfoCardSection.REQUEST)
        if (hasWaypointStatus && !hasRequest) add(InfoCardSection.WAYPOINT)
        if (hasRoute && meUnknown) add(InfoCardSection.ME_UNKNOWN)
        if (hasRoute && !meUnknown && memberRows > 0) add(InfoCardSection.MEMBERS)
        if (hasArrival) add(InfoCardSection.ARRIVAL)
    }
}
