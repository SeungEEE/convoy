package com.car.convoy.data.navi

import com.car.convoy.domain.Destination

/**
 * SPEC R11-17: 경로 시작 이벤트의 이름이 마지막으로 요청한 목적지 이름과 정확히 같을 때만
 * ROUTE_STARTED로 본다. 다르면 이전 요청의 늦은 이벤트다.
 */
fun isRouteStartedForRequest(startedName: String?, requested: Destination?): Boolean =
    requested != null && startedName == requested.name

/** SPEC R11-4: 도착 이벤트의 이름과 주소가 마지막으로 요청한 목적지와 정확히 같을 때만 NAVI_ARRIVED로 본다. */
fun isArrivalForRequest(name: String?, address: String?, requested: Destination?): Boolean =
    requested != null && name == requested.name && address == requested.address
