package com.car.convoy.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles

private const val TAG = "Convoy"

/** SPEC R14-6·7: 선 색. 파랑은 브랜드 파랑, 초록·주황은 지도 위에서 구분되는 진한 색. */
private val ROUTE_BLUE = Palette.Brand
private val SECTION_GREEN = Color(0xFF1E9E57)
private val SECTION_ORANGE = Color(0xFFE8770E)
private val MARKER_GRAY = Color(0xFF7A8492)

/** 카메라를 경로에 맞출 때 가장자리 여백(px). 160dpi라 dp와 같다. */
private const val FIT_PADDING = 120

/**
 * SPEC R14-6·7·15·16·17: 함께 주행 지도. 카카오맵 위에 경로선·마커·핀을 그리고, 회전 안내 카드와 안내 문구를 겹친다.
 * 도착 문구는 주행 화면의 정보 카드(R14-19)가 보여 준다.
 * [ui]가 바뀔 때마다 선과 마커를 모두 다시 그린다(다시 그리는 간격은 부르는 쪽이 R14-8대로 정한다).
 * 카메라는 [routeVersion]이 바뀔 때만 애니메이션 없이 경로 전체에 맞춘다(R14-17). 무엇을 이미 처리했는지는 지도보다 오래 사는
 * [ledger]가 기억한다. 화면을 오가 지도가 다시 만들어지면 경로에 다시 맞추지 않고 떠날 때의 카메라 위치로 되돌린다(Codex 리뷰).
 * [onReadyChange]는 지도가 준비되면 true, 오류가 나거나 지도가 사라지면 false다([내 위치] 잠김, R14-16).
 * [unavailable]이 있으면(카카오 키 없음, 이 기기 ABI에 지도 라이브러리 없음) 지도 대신 그 문구를 보여 주고,
 * 카드는 그대로 겹친다. [locateVersion]이 바뀌면 [내 위치](R14-16)를 누른 것이라 내 위치를 가운데로 옮긴다.
 */
@Composable
fun GroupMap(
    ui: GroupMapUi?,
    drawVersion: Int,
    routeVersion: Int,
    unavailable: String?,
    modifier: Modifier = Modifier,
    locateVersion: Int = 0,
    ledger: MapCameraLedger<CameraPosition>? = null,
    onReadyChange: (Boolean) -> Unit = {},
) {
    val cameraLedger = ledger ?: remember { MapCameraLedger(locateVersion) }
    // 지도를 띄운 뒤 생긴 오류(인증·렌더러·네트워크 등, Codex 리뷰). 생기면 지도 대신 안내를 보여 준다.
    var mapError by remember { mutableStateOf(false) }
    val reason = unavailable ?: if (mapError) "지도를 불러오지 못했어요" else null
    Box(modifier.background(Color(0xFFE9ECF0), RoundedCornerShape(32.dp))) {
        if (reason == null) {
            KakaoMapLayer(ui, drawVersion, routeVersion, locateVersion, cameraLedger, { mapError = true; onReadyChange(false) }, onReadyChange, Modifier.fillMaxSize())
        }

        ui?.turnCard?.let { TurnCard(it.title, it.subtitle, Modifier.align(Alignment.TopStart).padding(20.dp)) }
        // 지도 안내 문구(R14-2·6·16)를 가운데에 둔다. 지도를 못 그리면 그 이유를 위에 함께 둔다.
        val messages = listOfNotNull(reason, ui?.banner)
        if (messages.isNotEmpty()) Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { messages.forEach { Overlay(it, Modifier) } }
    }
}

/**
 * 카카오맵. 지도 내용은 [drawVersion]이 바뀔 때만 다시 그린다(카드만 바뀌면 그대로, Codex 리뷰).
 * 좁은 창(R14-16)에서 지도는 화면 세로 스크롤 안에 있고, 바깥 스크롤을 막지 않는다. 세로로 끄는 손가락은 화면 스크롤이
 * 먼저 가져간다(화면을 함께 스크롤한다는 R14-16을 우선). 지도 확대·이동은 넓은 창에서 온전히 된다.
 */
@Composable
private fun KakaoMapLayer(
    ui: GroupMapUi?,
    drawVersion: Int,
    routeVersion: Int,
    locateVersion: Int,
    ledger: MapCameraLedger<CameraPosition>,
    onError: () -> Unit,
    onReadyChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    // 그린 drawVersion. 지도가 새로 준비되면 처음부터 다시 그린다.
    val done = remember { intArrayOf(-1) }
    // 지도가 방금 준비됐는지. 카메라 장부가 "다시 만든 지도"를 알아보는 데 쓴다.
    val fresh = remember { booleanArrayOf(false) }
    val mapView = remember { MapView(context) }

    // 시작과 생명주기 연결은 효과 안에서 한다(구성이 버려지면 시작하지 않게). 이미 RESUMED면 바로 resume한다(Codex 리뷰).
    DisposableEffect(mapView) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    kakaoMap = null
                    onReadyChange(false)
                }
                override fun onMapError(error: Exception) {
                    Log.i(TAG, "카카오맵 오류: $error")
                    onError()
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    Log.i(TAG, "카카오맵 준비됨")
                    done[0] = -1
                    fresh[0] = true
                    // 사용자가 옮긴 카메라 자리를 기억해 둔다(다시 만든 지도에서 되돌리려고).
                    map.setOnCameraMoveEndListener { _, position, _ -> ledger.save(position) }
                    kakaoMap = map
                    onReadyChange(true)
                }
            },
        )
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) mapView.resume()
        onDispose {
            lifecycle?.removeObserver(observer)
            kakaoMap?.let { map -> runCatching { map.cameraPosition }.getOrNull()?.let(ledger::save) }
            onReadyChange(false)
            mapView.finish()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = {
            val map = kakaoMap ?: return@AndroidView
            if (drawVersion != done[0]) {
                done[0] = drawVersion
                draw(map, ui)
            }
            // R14-17: 새 길찾기 결과가 온 직후 한 번만 맞춘다. 사용자가 움직인 뒤에도 새 경로면 다시 맞춘다.
            // 같은 경로로 지도가 다시 만들어졌으면 맞추지 않고 떠날 때의 자리로 되돌린다(R14-8, Codex 리뷰).
            val route = ui?.lines?.firstOrNull { it.color == LineColor.BLUE }?.points
            val wasFresh = fresh[0]
            fresh[0] = false
            when (ledger.next(routeVersion, hasRoute = route != null, fresh = wasFresh)) {
                CameraAction.FIT_ROUTE -> {
                    val points = (route.orEmpty() + ui?.markers.orEmpty().map { it.position }).map { LatLng.from(it.latitude, it.longitude) }
                    map.moveCamera(CameraUpdateFactory.fitMapPoints(points.toTypedArray(), FIT_PADDING))
                }
                CameraAction.RESTORE -> ledger.saved?.let { map.moveCamera(CameraUpdateFactory.newCameraPosition(it)) }
                CameraAction.NONE -> Unit
            }
            // R14-16 [내 위치]: 사용자가 누를 때만, 애니메이션 없이 가운데로 옮긴다(확대 수준은 그대로).
            if (ledger.locate(locateVersion)) {
                ui?.myPoint?.let { map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(it.latitude, it.longitude))) }
            }
        },
    )
}

/** 선과 마커를 모두 지우고 [ui]대로 다시 그린다. 파랑을 먼저, 초록·주황을 위에, 마커를 맨 위에 둔다. */
private fun draw(map: KakaoMap, ui: GroupMapUi?) {
    val lines = map.routeLineManager?.layer ?: return
    val labels = map.labelManager?.layer ?: return
    lines.removeAll()
    labels.removeAll()
    if (ui == null) return

    ui.lines.forEachIndexed { i, line ->
        val (color, width) = when (line.color) {
            LineColor.BLUE -> ROUTE_BLUE to 16f
            LineColor.GREEN -> SECTION_GREEN to 20f
            LineColor.ORANGE -> SECTION_ORANGE to 20f
        }
        val style = RouteLineStyles.from(RouteLineStyle.from(width, color.toArgb(), 3f, Color.White.toArgb()))
        val segment = RouteLineSegment.from(line.points.map { LatLng.from(it.latitude, it.longitude) }, style)
        lines.addRouteLine(RouteLineOptions.from(segment).setZOrder(i))
    }

    ui.pins.forEach { pin ->
        val bitmap = pinBitmap(pin.name, if (pin.kind == PinKind.DESTINATION) Palette.Ink else Palette.Demo)
        labels.addLabel(
            LabelOptions.from(LatLng.from(pin.position.latitude, pin.position.longitude))
                .setStyles(LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1f)))
                .setRank(1),
        )
    }
    ui.markers.forEach { m ->
        val color = when (m.tone) {
            MarkerTone.ME -> Palette.Brand
            MarkerTone.AHEAD -> SECTION_GREEN
            MarkerTone.BEHIND -> SECTION_ORANGE
            MarkerTone.GRAY -> MARKER_GRAY
        }
        labels.addLabel(
            LabelOptions.from(LatLng.from(m.position.latitude, m.position.longitude))
                .setStyles(LabelStyles.from(LabelStyle.from(markerBitmap(m.text, color)).setAnchorPoint(0.5f, 0.5f)))
                .setRank(if (m.tone == MarkerTone.ME) 3 else 2),
        )
    }
}

/** 사람 마커: 흰 테두리 원 안에 "나" 또는 이름 첫 글자(R14-16). */
private fun markerBitmap(text: String, color: Color): Bitmap {
    val size = 76
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = color.toArgb()
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 34f
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(text, size / 2f, y, paint)
    return bitmap
}

/** 목적지·경유지 핀: 이름이 적힌 말풍선과 아래 꼬리. 꼬리 끝이 좌표다. */
private fun pinBitmap(name: String, color: Color): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
    }
    val textWidth = paint.measureText(name)
    val w = (textWidth + 40f).toInt().coerceAtLeast(80)
    val bodyH = 56
    val tail = 16
    val bitmap = Bitmap.createBitmap(w, bodyH + tail, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    paint.color = color.toArgb()
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), bodyH.toFloat()), 18f, 18f, paint)
    val path = android.graphics.Path().apply {
        moveTo(w / 2f - 12f, bodyH.toFloat()); lineTo(w / 2f + 12f, bodyH.toFloat()); lineTo(w / 2f, (bodyH + tail).toFloat()); close()
    }
    canvas.drawPath(path, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(name, w / 2f, bodyH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
    return bitmap
}

/** SPEC R14-7·15: 회전 안내 카드(지도 왼쪽 위). */
@Composable
private fun TurnCard(title: String, subtitle: String?, modifier: Modifier) {
    Column(
        modifier.widthIn(max = 560.dp).background(Palette.Night, RoundedCornerShape(24.dp)).padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        subtitle?.let { Text(it, fontSize = 28.sp, color = Palette.OnNightSoft, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** 지도 가운데 안내 문구(R14-2·6·16). */
@Composable
private fun Overlay(text: String, modifier: Modifier) {
    Box(modifier.background(Color(0xE60F1720), RoundedCornerShape(20.dp)).padding(horizontal = 32.dp, vertical = 18.dp)) {
        Text(text, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
