package com.car.convoy.data.navi

import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.car.convoy.domain.Destination
import com.car.convoy.domain.port.NaviPort

/**
 * NaviHelper SDK를 [NaviPort]로 감싼다. 시그니처는 NaviHelper 3.0.5 AAR을 javap로 확인했다.
 * 이벤트는 SDK 값 그대로 넘기고, 거르기(R11-4, R11-17)는 GroupController가 한다.
 * 콜백 스레드는 보장되지 않으므로 받는 쪽이 메인 스레드로 옮긴다.
 */
class NaviHelperAdapter(
    context: Context,
    private val routeStarted: (name: String?) -> Unit,
    private val destinationArrived: (name: String?, address: String?) -> Unit,
    private val routeFailed: () -> Unit,
    private val log: (String) -> Unit,
) : NaviPort {

    private val navi = NaviHelper(context)

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            val info = routeStartInfo
            // R11-17 확인용: name에 무엇이 오는지 그대로 남긴다.
            log("SDK onRouteStarted name=[${info.name}] distance=${info.distance} duration=${info.duration}")
            routeStarted(info.name)
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            val info = destinationArrivedInfo
            log("SDK onDestinationArrived name=[${info.name}] address=[${info.address}]")
            destinationArrived(info.name, info.address)
        }

        override fun onRouteCancelled() = log("SDK onRouteCancelled")
    }

    fun start(onReady: () -> Unit) {
        navi.initialize(
            { navi.addListener(listener); log("NaviHelper 초기화 성공"); onReady() },
            { e -> log("NaviHelper 초기화 실패: $e") },
        )
    }

    fun stop() {
        runCatching { navi.removeListener(listener) }
        runCatching { navi.release() }
    }

    override fun requestRoute(destination: Destination) {
        log("requestRoute name=[${destination.name}] address=[${destination.address}]")
        // SDK 예외(퍼미션 누락 등)가 GroupSession의 멤버 순회를 끊지 않도록 여기서 받고,
        // 실패는 routeFailed로 알린다(R11-19).
        runCatching { navi.requestRoute(
            RouteInfo(
                latitude = destination.latitude,
                longitude = destination.longitude,
                displayLatitude = null,
                displayLongitude = null,
                poiName = destination.name,
                poiId = "",
                address = destination.address,
                poiSubId = "0",
            )
        ) }.onFailure { log("requestRoute 실패: $it"); routeFailed() }
    }
}
