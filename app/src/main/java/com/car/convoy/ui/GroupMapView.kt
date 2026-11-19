package com.car.convoy.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * SPEC R14-6·7·15·16·17: 함께 주행 지도. 카카오맵 위에 경로선·마커·핀을 그리고, 회전 안내·도착 카드와 안내 문구를 겹친다.
 * [ui]가 바뀔 때마다 선과 마커를 모두 다시 그린다(다시 그리는 간격은 부르는 쪽이 R14-8대로 정한다).
 * 카메라는 [routeVersion]이 바뀔 때만 애니메이션 없이 경로 전체에 맞춘다(R14-17).
 * [unavailable]이 있으면(카카오 키 없음, 이 기기 ABI에 지도 라이브러리 없음) 지도 대신 그 문구를 보여 주고,
 * 카드는 그대로 겹친다.
 */
@Composable
fun GroupMap(ui: GroupMapUi?, routeVersion: Int, unavailable: String?, modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFFE9ECF0), RoundedCornerShape(32.dp))) {
        if (unavailable == null) KakaoMapLayer(ui, routeVersion, Modifier.fillMaxSize())

        ui?.turnCard?.let { TurnCard(it.title, it.subtitle, Modifier.align(Alignment.TopStart).padding(20.dp)) }
        // 지도 안내 문구(R14-2·6·16)를 가운데에 둔다. 지도를 못 그리면 그 이유를 위에 함께 둔다.
        val messages = listOfNotNull(unavailable, ui?.banner)
        if (messages.isNotEmpty()) Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { messages.forEach { Overlay(it, Modifier) } }
        ui?.arrivalCard?.let { ArrivalCard(it, Modifier.align(Alignment.BottomCenter).padding(20.dp)) }
    }
}

@Composable
private fun KakaoMapLayer(ui: GroupMapUi?, routeVersion: Int, modifier: Modifier) {
    val context = LocalContext.current
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    val fitted = remember { intArrayOf(-1) }
    val mapView = remember {
        MapView(context).apply {
            start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() { kakaoMap = null }
                    override fun onMapError(error: Exception) { Log.i(TAG, "카카오맵 오류: $error") }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(map: KakaoMap) {
                        Log.i(TAG, "카카오맵 준비됨")
                        kakaoMap = map
                    }
                },
            )
        }
    }

    // 액티비티 생명주기에 맞춰 resume·pause하고, 화면에서 빠지면 끝낸다(docs/sdk-findings.md).
    DisposableEffect(mapView) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            mapView.finish()
        }
    }

    AndroidView(
        factory = { ctx -> TouchKeeper(ctx).apply { addView(mapView) } },
        modifier = modifier,
        update = {
            val map = kakaoMap ?: return@AndroidView
            draw(map, ui)
            // R14-17: 새 길찾기 결과가 온 직후 한 번만 맞춘다. 사용자가 움직인 뒤에도 새 경로면 다시 맞춘다.
            val route = ui?.lines?.firstOrNull { it.color == LineColor.BLUE }?.points
            if (route != null && routeVersion != fitted[0]) {
                fitted[0] = routeVersion
                val points = (route + ui.markers.map { it.position }).map { LatLng.from(it.latitude, it.longitude) }
                map.moveCamera(CameraUpdateFactory.fitMapPoints(points.toTypedArray(), FIT_PADDING))
            }
        },
    )
}

/**
 * 좁은 창(R14-16)에서는 지도가 세로 스크롤 안에 있다. 지도를 누르는 동안에는 바깥 스크롤이 손가락을 가져가지 않게 한다.
 */
@SuppressLint("ViewConstructor")
private class TouchKeeper(context: Context) : FrameLayout(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(event)
    }
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

/** SPEC R14-7·15: 도착 카드(지도 아래쪽). */
@Composable
private fun ArrivalCard(text: String, modifier: Modifier) {
    Row(
        modifier.background(Palette.Card, RoundedCornerShape(24.dp)).padding(horizontal = 32.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Text(text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/** 지도 가운데 안내 문구(R14-2·6·16). */
@Composable
private fun Overlay(text: String, modifier: Modifier) {
    Box(modifier.background(Color(0xE60F1720), RoundedCornerShape(20.dp)).padding(horizontal = 32.dp, vertical = 18.dp)) {
        Text(text, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
