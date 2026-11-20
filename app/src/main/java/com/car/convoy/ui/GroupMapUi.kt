package com.car.convoy.ui

import com.car.convoy.data.MapSnapshot
import com.car.convoy.domain.Destination
import com.car.convoy.domain.DrivingRemain
import com.car.convoy.domain.GeoPoint
import com.car.convoy.domain.MemberRelation
import com.car.convoy.domain.TurnCardText
import com.car.convoy.domain.TurnGuide
import com.car.convoy.domain.arrivalCardText
import com.car.convoy.domain.memberMapLabel
import com.car.convoy.domain.routeSection
import com.car.convoy.domain.turnCardText
import java.time.ZoneId

/** SPEC R14-6·7: 내 경로 전체는 파랑, 앞 멤버까지는 초록, 뒤 멤버부터 나까지는 주황. */
enum class LineColor { BLUE, GREEN, ORANGE }

/** SPEC R14-6: 마커 색. 나란히·다른 길·위치 오래됨·내 위치 모름은 회색(R14-16). */
enum class MarkerTone { ME, AHEAD, BEHIND, GRAY }

enum class PinKind { DESTINATION, WAYPOINT }

data class MapLine(val points: List<GeoPoint>, val color: LineColor)

/** [text]는 "나" 또는 이름 첫 글자(R14-16). */
data class MapMarker(val id: String, val text: String, val position: GeoPoint, val tone: MarkerTone)

data class MapPin(val name: String, val position: GeoPoint, val kind: PinKind)

/** SPEC R14-6·7·13·15·16: 지도 화면이 그릴 값. Compose와 카카오맵은 이 값만 그린다. */
data class GroupMapUi(
    val lines: List<MapLine>,
    val markers: List<MapMarker>,
    val pins: List<MapPin>,
    val banner: String?,
    /** 멤버 id → 앞뒤 문구. 내 위치를 모르면 null(R14-13). 멤버 타일에 한 줄 더한다(R14-16). */
    val memberLabels: Map<String, String?>,
    val turnCard: TurnCardText?,
    val arrivalCard: String?,
    /** SPEC R14-19: 경로가 있다(목적지 있음, 요청 중 아님, 실패 아님, 경로 점 있음). 없으면 정보 카드에 멤버 줄·내 위치 칸이 없다. */
    val hasRoute: Boolean = false,
    /** SPEC R14-5·19: 내 위치를 모름(오래됐거나 경로 밖). */
    val meUnknown: Boolean = false,
    /** SPEC R14-16 [내 위치]: 마지막으로 받은 내 위치. ME_UNKNOWN이어도 있다. */
    val myPoint: GeoPoint? = null,
)

/**
 * SPEC R14-6·7·13·15·16을 그릴 값으로 바꾼다.
 * 안내 문구는 목적지 없음 → 경로 찾는 중(요청 중) → 실패 → 경로 찾는 중(아직 경로 없음, 내 위치를 기다리는 중) →
 * 내 위치 확인 중 순으로 하나만 보인다.
 */
fun groupMapUi(
    snapshot: MapSnapshot,
    loading: Boolean,
    failed: Boolean,
    destination: Destination?,
    waypoints: List<Destination>,
    names: Map<String, String>,
    turn: TurnGuide?,
    remain: DrivingRemain?,
    nowMillis: Long,
    zone: ZoneId,
): GroupMapUi {
    val route = snapshot.route
    val lines = mutableListOf<MapLine>()
    if (route.size >= 2) lines += MapLine(route, LineColor.BLUE)
    val myProgress = snapshot.myProgress
    if (!snapshot.meUnknown && myProgress != null) {
        snapshot.members.forEach { m ->
            val line = when (m.relation) {
                MemberRelation.AHEAD -> MapLine(routeSection(route, myProgress, m.progress), LineColor.GREEN)
                MemberRelation.BEHIND -> MapLine(routeSection(route, m.progress, myProgress), LineColor.ORANGE)
                else -> null
            }
            if (line != null && line.points.size >= 2) lines += line
        }
    }

    val markers = buildList {
        snapshot.myPoint?.let { add(MapMarker("me", "나", it, MarkerTone.ME)) }
        snapshot.members.forEach { m ->
            val tone = when (m.relation) {
                MemberRelation.AHEAD -> MarkerTone.AHEAD
                MemberRelation.BEHIND -> MarkerTone.BEHIND
                else -> MarkerTone.GRAY
            }
            add(MapMarker(m.id, (names[m.id] ?: m.id).take(1), m.position, tone))
        }
    }

    val pins = buildList {
        destination?.let { add(MapPin(it.name, GeoPoint(it.latitude, it.longitude), PinKind.DESTINATION)) }
        waypoints.forEach { add(MapPin(it.name, GeoPoint(it.latitude, it.longitude), PinKind.WAYPOINT)) }
    }

    val banner = when {
        destination == null -> "목적지를 정하면 경로가 보여요"
        loading -> "경로 찾는 중"
        failed -> "경로를 불러오지 못했어요"
        route.isEmpty() -> "경로 찾는 중"
        snapshot.meUnknown -> "내 위치 확인 중"
        else -> null
    }

    return GroupMapUi(
        lines = lines,
        markers = markers,
        pins = pins,
        banner = banner,
        memberLabels = snapshot.members.associate { m ->
            m.id to memberMapLabel(names[m.id] ?: m.id, m.relation, m.gapMeters, m.ageSeconds)
        },
        turnCard = if (destination == null) null else turn?.let { turnCardText(it.turnTypeName, it.distanceMeters, it.description) },
        arrivalCard = if (destination == null) null else remain?.let { arrivalCardText(nowMillis, zone, it.seconds, it.meters) },
        hasRoute = destination != null && !loading && !failed && route.isNotEmpty(),
        meUnknown = snapshot.meUnknown,
        myPoint = snapshot.myPoint,
    )
}
