package com.car.convoy.ui

import com.car.convoy.domain.Destination
import com.car.convoy.domain.distanceMeters

/** SPEC R5-3, R11-21: 같은 목적지는 이름이 아니라 좌표로 판단한다. 10m 미만이면 같다. */
private const val SAME_PLACE_METERS = 10.0

private const val MAX_RECENT = 5

/** 목적지 목록의 한 줄. [current]는 지금 그룹 목적지와 같은 곳(10m 미만)이다. */
data class PickRow(val destination: Destination, val current: Boolean)

/** SPEC R11-20, R11-21: 그룹 주행 화면의 "목적지 바꾸기"에 보일 값. */
data class DestinationPickerModel(
    /** "내비에서 정한 목적지" 카드. 없으면 null. */
    val suggestion: Destination?,
    /** 최근 목적지. 최근·즐겨찾기가 모두 비면 미리 정한 목록이 들어간다([usingFallback]). */
    val recent: List<PickRow>,
    val bookmarks: List<PickRow>,
    val usingFallback: Boolean,
)

fun samePlace(a: Destination, b: Destination): Boolean = distanceMeters(a, b) < SAME_PLACE_METERS

/** R11-21: 이름이 비어 있거나 좌표가 (0, 0)이면 쓸 수 없는 값(빈 목적지, 비어 있는 즐겨찾기 칸)이다. */
private fun Destination.usable(): Boolean = name.isNotBlank() && !(latitude == 0.0 && longitude == 0.0)

/**
 * SPEC R11-21.
 * - 내비 목적지 카드: 그룹 목적지가 없거나 10m 이상 다르고, [무시]한 곳(10m 미만)이 아니면 보여 준다.
 *   주소는 최근 목적지 중 이름이 같고 10m 미만인 것이 있으면 그 주소를 쓴다(내비 목적지의 주소는 짧게 온다).
 * - 최근 목적지는 쓸 수 있는 것 최대 5개, 즐겨찾기는 쓸 수 있는 것만. 둘 다 비면 미리 정한 목록.
 */
fun destinationPickerModel(
    navi: Destination?,
    group: Destination?,
    ignored: Destination?,
    recent: List<Destination>,
    bookmarks: List<Destination>,
): DestinationPickerModel {
    val usableRecent = recent.filter { it.usable() }
    val suggestion = navi?.takeIf { it.usable() }
        ?.takeIf { n -> group == null || !samePlace(n, group) }
        ?.takeIf { n -> ignored == null || !samePlace(n, ignored) }
        ?.let { n ->
            val full = usableRecent.firstOrNull { it.name == n.name && samePlace(it, n) }
            if (full != null) n.copy(address = full.address) else n
        }
    val marks: (Destination) -> PickRow = { PickRow(it, group != null && samePlace(it, group)) }
    val shownRecent = usableRecent.take(MAX_RECENT)
    val shownBookmarks = bookmarks.filter { it.usable() }
    val fallback = shownRecent.isEmpty() && shownBookmarks.isEmpty()
    return DestinationPickerModel(
        suggestion = suggestion,
        recent = (if (fallback) PRESET_DESTINATIONS else shownRecent).map(marks),
        bookmarks = shownBookmarks.map(marks),
        usingFallback = fallback,
    )
}
