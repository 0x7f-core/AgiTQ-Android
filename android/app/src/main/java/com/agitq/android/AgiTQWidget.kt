package com.agitq.android

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val SCRIPTABLE_VERSION = "v3.0-γ (26.08.12)"
private const val TARGET_BITMAP_AREA = 500_000.0

private val COLOR_BG = AndroidColor.BLACK
private val COLOR_WHITE = AndroidColor.WHITE
private val COLOR_P2 = AndroidColor.rgb(170, 170, 170)
private val COLOR_DOT = AndroidColor.rgb(102, 102, 102)
private val COLOR_TQQQ = AndroidColor.rgb(232, 113, 79)
private val COLOR_SPYM = AndroidColor.rgb(176, 124, 192)
private val COLOR_SGOV = AndroidColor.rgb(91, 184, 232)
private val COLOR_CP = AndroidColor.rgb(128, 223, 255)
private val COLOR_UPPER = AndroidColor.rgb(224, 112, 192)
private val COLOR_LOWER = AndroidColor.rgb(175, 212, 133)
private val COLOR_ALERT = AndroidColor.rgb(255, 77, 77)

private enum class CardKind { SPX, QQQ, FGI }
private enum class LayoutMode { WIDE, STACKED, TALL }
private val RefreshKindKey = ActionParameters.Key<String>("refresh_kind")

class SpxWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        provideContent {
            ResponsiveFullCard(context, data, CardKind.SPX, "아기티큐 200슨피단 SPX")
        }
    }
}

class SpxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpxWidget()
}

class QqqWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        provideContent {
            ResponsiveFullCard(context, data, CardKind.QQQ, "아기티큐 200큐큐단 QQQ")
        }
    }
}

class QqqWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QqqWidget()
}

class FgiWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        provideContent {
            ResponsiveFullCard(context, data, CardKind.FGI, "공포와 탐욕 지수 CNN FGI")
        }
    }
}

class FgiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FgiWidget()
}

private suspend fun loadData(): JSONObject? = withContext(Dispatchers.IO) {
    runCatching { AgiTQApi.load() }.getOrNull()
}

/** 우측 하단 수동 새로고침 버튼: 현재 누른 위젯만 즉시 다시 로드한다. */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        when (parameters[RefreshKindKey]) {
            CardKind.SPX.name -> SpxWidget().update(context, glanceId)
            CardKind.QQQ.name -> QqqWidget().update(context, glanceId)
            CardKind.FGI.name -> FgiWidget().update(context, glanceId)
        }
    }
}

@Composable
private fun ResponsiveFullCard(
    context: Context,
    data: JSONObject?,
    kind: CardKind,
    description: String
) {
    val glanceSize = LocalSize.current
    val aspect = (glanceSize.width.value / glanceSize.height.value)
        .coerceIn(0.35f, 4.0f)
    val (bitmapW, bitmapH) = bitmapDimensions(aspect)

    val bitmap = when (kind) {
        CardKind.SPX -> data?.optJSONObject("SPX")?.let {
            marketCardBitmap(it, "아기티큐 200슨피단 (SPX)", 0.025, bitmapW, bitmapH)
        }
        CardKind.QQQ -> data?.optJSONObject("QQQ")?.let {
            marketCardBitmap(it, "아기티큐 200큐큐단 (QQQ)", 0.02, bitmapW, bitmapH)
        }
        CardKind.FGI -> data?.optJSONObject("FGI")?.let {
            fgiCardBitmap(it, bitmapW, bitmapH)
        }
    } ?: errorCardBitmap(bitmapW, bitmapH)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = description,
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(dashboardIntent(context))),
            contentScale = ContentScale.FillBounds
        )

        Image(
            provider = ImageProvider(R.drawable.ic_widget_refresh),
            contentDescription = "새로고침",
            modifier = GlanceModifier
                .size(42.dp)
                .padding(8.dp)
                .clickable(
                    actionRunCallback<RefreshWidgetAction>(
                        actionParametersOf(RefreshKindKey to kind.name)
                    )
                )
        )
    }
}

private fun bitmapDimensions(aspect: Float): Pair<Int, Int> {
    val w = sqrt(TARGET_BITMAP_AREA * aspect).roundToInt().coerceAtLeast(320)
    val h = (w / aspect).roundToInt().coerceAtLeast(260)
    return w to h
}

private fun layoutMode(width: Int, height: Int): LayoutMode {
    val aspect = width.toFloat() / height.toFloat()
    return when {
        aspect >= 1.45f -> LayoutMode.WIDE
        aspect >= 0.78f -> LayoutMode.STACKED
        else -> LayoutMode.TALL
    }
}

private fun dashboardIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
    action = "com.agitq.android.action.OPEN_DASHBOARD"
    data = Uri.parse("agitq://dashboard")
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

/**
 * SPX/QQQ 독립 카드. 위젯의 실제 가로/세로 비율에 맞춰 Bitmap 자체의 비율과
 * 내부 레이아웃을 함께 바꾼다. 따라서 One UI에서 자유롭게 리사이즈해도 단순 늘림이 없다.
 */
private fun marketCardBitmap(
    asset: JSONObject,
    title: String,
    bandPct: Double,
    width: Int,
    height: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)

    val mode = layoutMode(width, height)
    val short = min(width, height).toFloat()
    val pad = short * 0.055f
    val titleSize = (short * 0.0615f).coerceIn(22f, 38f)
    val versionSize = (short * 0.0365f).coerceIn(13f, 22f)
    val timeSize = (short * 0.0405f).coerceIn(15f, 25f)
    val titleY = pad + titleSize

    drawText(canvas, title, pad, titleY, titleSize, COLOR_WHITE, true)
    drawText(canvas, SCRIPTABLE_VERSION, width - pad, titleY - 2f, versionSize, COLOR_P2, false, Paint.Align.RIGHT)

    val timeY = titleY + timeSize * 1.45f
    drawText(canvas, formatMarketTime(asset.optLong("mTime", 0L)), pad, timeY, timeSize, COLOR_WHITE)

    val sig = asset.optJSONObject("signal")
    val contentTop = timeY + pad * 0.55f
    val bottom = height - pad * 0.75f

    when (mode) {
        LayoutMode.WIDE -> {
            val split = width * 0.565f
            val chartArea = RectF(pad * 0.78f, contentTop, split, bottom)
            drawMarketChartScriptable(canvas, asset, bandPct, chartArea)

            val signalX = width * 0.575f
            val signalTop = contentTop + (bottom - contentTop) * 0.06f
            val signalWidth = width - signalX - pad
            drawSignalBlockResponsive(
                canvas = canvas,
                sig = sig,
                x = signalX,
                top = signalTop,
                maxWidth = signalWidth,
                short = short,
                densityFactor = 1f
            )
        }

        LayoutMode.STACKED -> {
            val remain = bottom - contentTop
            val chartBottom = contentTop + remain * 0.54f
            drawMarketChartScriptable(
                canvas,
                asset,
                bandPct,
                RectF(pad * 0.75f, contentTop, width - pad * 0.75f, chartBottom)
            )

            drawSignalBlockResponsive(
                canvas = canvas,
                sig = sig,
                x = pad,
                top = chartBottom + pad * 0.30f,
                maxWidth = width - pad * 2f,
                short = short,
                densityFactor = 0.82f
            )
        }

        LayoutMode.TALL -> {
            val remain = bottom - contentTop
            val chartBottom = contentTop + remain * 0.46f
            drawMarketChartScriptable(
                canvas,
                asset,
                bandPct,
                RectF(pad * 0.72f, contentTop, width - pad * 0.72f, chartBottom)
            )

            drawSignalBlockResponsive(
                canvas = canvas,
                sig = sig,
                x = pad,
                top = chartBottom + pad * 0.45f,
                maxWidth = width - pad * 2f,
                short = short,
                densityFactor = 0.75f
            )
        }
    }

    return bitmap
}

/** 원본 Scriptable drawBandChart의 색/형태를 유지하면서 주어진 영역 비율에 맞춰 다시 계산. */
private fun drawMarketChartScriptable(canvas: Canvas, asset: JSONObject, bandPct: Double, area: RectF) {
    val arr = asset.optJSONArray("closes") ?: return
    val closes = ArrayList<Double>(arr.length())
    for (i in 0 until arr.length()) {
        val v = arr.optDouble(i, Double.NaN)
        if (v.isFinite() && v > 0.0) closes += v
    }
    if (closes.size < 200) return

    val sma = arrayOfNulls<Double>(closes.size)
    var rolling = 0.0
    for (i in closes.indices) {
        rolling += closes[i]
        if (i >= 200) rolling -= closes[i - 200]
        if (i >= 199) sma[i] = rolling / 200.0
    }

    val start = max(0, closes.size - 90)
    val prices = closes.subList(start, closes.size)
    val upper = (start until closes.size).map { sma[it]?.times(1.0 + bandPct) }
    val lower = (start until closes.size).map { sma[it]?.times(1.0 - bandPct) }

    val all = mutableListOf<Double>()
    all += prices
    upper.filterNotNullTo(all)
    lower.filterNotNullTo(all)
    if (all.isEmpty()) return

    var minV = all.minOrNull() ?: return
    var maxV = all.maxOrNull() ?: return
    if (maxV <= minV) maxV = minV + 1.0

    val padX = area.width() * 0.03f
    val padY = area.height() * 0.05f
    val plot = RectF(area.left + padX, area.top + padY, area.right - padX, area.bottom - padY)

    fun x(i: Int): Float = plot.left + if (prices.size <= 1) 0f else i.toFloat() / (prices.size - 1) * plot.width()
    fun y(v: Double): Float = plot.bottom - ((v - minV) / (maxV - minV) * plot.height()).toFloat()

    val lineScale = min(area.width() / 456f, area.height() / 284f).coerceIn(0.55f, 1.65f)

    fun series(data: List<Double?>, color: Int, widthPx: Float) {
        val path = Path()
        var started = false
        data.forEachIndexed { i, v ->
            if (v == null || !v.isFinite()) {
                started = false
            } else {
                val px = x(i)
                val py = y(v)
                if (!started) {
                    path.moveTo(px, py)
                    started = true
                } else {
                    path.lineTo(px, py)
                }
            }
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = widthPx * lineScale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
    }

    series(upper, COLOR_UPPER, 3.8f)
    series(lower, COLOR_LOWER, 3.8f)
    series(prices.map { it }, COLOR_CP, 6.0f)
}

private fun drawSignalBlockResponsive(
    canvas: Canvas,
    sig: JSONObject?,
    x: Float,
    top: Float,
    maxWidth: Float,
    short: Float,
    densityFactor: Float
) {
    if (sig == null) return
    val isAlert = sig.optBoolean("alert", false)
    val lines = sig.optJSONArray("lines")
    val rowSize = (short * 0.067f * densityFactor).coerceIn(20f, 38f)
    val rowGap = rowSize * 1.62f
    var y = top + rowSize

    if (lines != null) {
        for (i in 0 until min(lines.length(), 2)) {
            val row = lines.optJSONArray(i) ?: continue
            drawSignalRow(
                canvas,
                row.optString(0),
                row.optString(1),
                x,
                y,
                rowSize,
                isAlert,
                maxWidth
            )
            y += rowGap
        }
    }

    y += rowSize * 0.30f
    drawFittedText(
        canvas,
        sig.optString("name", "-"),
        x,
        y,
        rowSize * 0.66f,
        if (isAlert) COLOR_ALERT else COLOR_P2,
        false,
        maxWidth
    )

    y += rowSize * 1.48f
    drawFittedText(
        canvas,
        drawdownText(sig),
        x,
        y,
        rowSize * 0.78f,
        COLOR_WHITE,
        true,
        maxWidth
    )
}

private fun drawSignalRow(
    canvas: Canvas,
    token: String,
    action: String,
    startX: Float,
    baseline: Float,
    requestedSize: Float,
    isAlert: Boolean,
    maxWidth: Float
) {
    val size = fitSignalRowSize(token, action, requestedSize, isAlert, maxWidth)
    var x = startX
    val boldPaint = textPaint(size, COLOR_WHITE, true)

    if (isAlert) {
        drawText(canvas, "$token $action", x, baseline, size, COLOR_ALERT, true)
        return
    }

    val parts = token.split("·")
    parts.forEachIndexed { index, part ->
        val color = etfAndroidColor(part)
        drawText(canvas, part, x, baseline, size, color, true)
        x += textPaint(size, color, true).measureText(part)
        if (index < parts.lastIndex) {
            drawText(canvas, "·", x, baseline, size, COLOR_DOT, true)
            x += textPaint(size, COLOR_DOT, true).measureText("·")
        }
    }

    x += boldPaint.measureText(" ") * 1.4f
    drawText(canvas, action, x, baseline, size, COLOR_WHITE, true)
}

private fun fitSignalRowSize(
    token: String,
    action: String,
    requestedSize: Float,
    isAlert: Boolean,
    maxWidth: Float
): Float {
    if (maxWidth <= 1f) return requestedSize
    val paint = textPaint(requestedSize, COLOR_WHITE, true)
    val text = if (isAlert) "$token $action" else "$token  $action"
    val measured = paint.measureText(text)
    if (measured <= maxWidth) return requestedSize
    return (requestedSize * maxWidth / measured).coerceAtLeast(requestedSize * 0.64f)
}

/** FGI도 wide/stacked/tall 세 가지 배치로 재구성한다. */
private fun fgiCardBitmap(fgi: JSONObject, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)

    val value = fgi.optDouble("value", Double.NaN)
    if (!value.isFinite()) return errorCardBitmap(width, height)
    val avg30 = fgi.optDouble("avg30", Double.NaN)
    val rating = fgiRatingFull(fgi.optString("rating"))

    val mode = layoutMode(width, height)
    val short = min(width, height).toFloat()
    val pad = short * 0.055f
    val titleSize = (short * 0.0615f).coerceIn(22f, 38f)
    val titleY = pad + titleSize
    drawText(canvas, "공포와 탐욕 지수 (CNN FGI)", pad, titleY, titleSize, COLOR_WHITE, true)

    val contentTop = titleY + pad * 0.75f
    val bottom = height - pad * 0.75f

    when (mode) {
        LayoutMode.WIDE -> {
            val historyRight = width * 0.535f
            drawFgiHistoryScriptable(
                canvas,
                fgi,
                RectF(pad, contentTop + pad * 0.25f, historyRight, bottom)
            )

            val rightCenter = width * 0.775f
            val rightLeft = width * 0.575f
            val rightRight = width - pad
            val gaugeBottom = contentTop + (bottom - contentTop) * 0.56f
            drawFgiGaugeScriptable(
                canvas,
                value,
                RectF(rightLeft, contentTop, rightRight, gaugeBottom)
            )

            val ratingY = contentTop + (bottom - contentTop) * 0.72f
            val ratingSize = (short * 0.059f).coerceIn(22f, 35f)
            drawFittedCenteredText(
                canvas,
                rating,
                rightCenter,
                ratingY,
                ratingSize,
                fgiAndroidColor(value),
                true,
                rightRight - rightLeft
            )

            drawFgiStats(
                canvas,
                value,
                avg30,
                rightCenter,
                contentTop + (bottom - contentTop) * 0.90f,
                (short * 0.050f).coerceIn(18f, 30f),
                rightRight - rightLeft
            )
        }

        LayoutMode.STACKED -> {
            val remain = bottom - contentTop
            val historyBottom = contentTop + remain * 0.43f
            drawFgiHistoryScriptable(
                canvas,
                fgi,
                RectF(pad, contentTop, width - pad, historyBottom)
            )

            val gaugeTop = historyBottom + pad * 0.10f
            val gaugeBottom = gaugeTop + remain * 0.30f
            drawFgiGaugeScriptable(
                canvas,
                value,
                RectF(pad * 1.5f, gaugeTop, width - pad * 1.5f, gaugeBottom)
            )

            val centerX = width / 2f
            val ratingY = gaugeBottom + short * 0.060f
            drawFittedCenteredText(
                canvas,
                rating,
                centerX,
                ratingY,
                (short * 0.055f).coerceIn(20f, 34f),
                fgiAndroidColor(value),
                true,
                width - pad * 2f
            )
            drawFgiStats(
                canvas,
                value,
                avg30,
                centerX,
                min(bottom, ratingY + short * 0.085f),
                (short * 0.044f).coerceIn(17f, 28f),
                width - pad * 2f
            )
        }

        LayoutMode.TALL -> {
            val remain = bottom - contentTop
            val historyBottom = contentTop + remain * 0.38f
            drawFgiHistoryScriptable(
                canvas,
                fgi,
                RectF(pad, contentTop, width - pad, historyBottom)
            )

            val gaugeTop = historyBottom + pad * 0.20f
            val gaugeBottom = gaugeTop + remain * 0.28f
            drawFgiGaugeScriptable(
                canvas,
                value,
                RectF(pad, gaugeTop, width - pad, gaugeBottom)
            )

            val centerX = width / 2f
            val ratingY = gaugeBottom + short * 0.075f
            drawFittedCenteredText(
                canvas,
                rating,
                centerX,
                ratingY,
                (short * 0.055f).coerceIn(20f, 34f),
                fgiAndroidColor(value),
                true,
                width - pad * 1.5f
            )
            drawFgiStats(
                canvas,
                value,
                avg30,
                centerX,
                min(bottom, ratingY + short * 0.095f),
                (short * 0.043f).coerceIn(16f, 27f),
                width - pad * 1.5f
            )
        }
    }

    return bitmap
}

private fun drawFgiHistoryScriptable(canvas: Canvas, fgi: JSONObject, area: RectF) {
    val arr = fgi.optJSONArray("history") ?: return
    if (arr.length() < 2) return

    val values = mutableListOf<Double>()
    val start = max(0, arr.length() - 90)
    for (i in start until arr.length()) {
        val v = arr.optJSONObject(i)?.optDouble("y", Double.NaN) ?: Double.NaN
        if (v.isFinite()) values += v.coerceIn(0.0, 100.0)
    }
    if (values.size < 2) return

    val padL = area.width() * (8f / 480f)
    val padR = area.width() * (8f / 480f)
    val padT = area.height() * (12f / 300f)
    val padB = area.height() * (12f / 300f)
    val plot = RectF(area.left + padL, area.top + padT, area.right - padR, area.bottom - padB)

    fun toX(i: Int): Float = plot.left + i.toFloat() / (values.size - 1) * plot.width()
    fun toY(v: Double): Float = plot.top + ((100.0 - v) / 100.0 * plot.height()).toFloat()

    val scale = min(area.width() / 480f, area.height() / 300f).coerceIn(0.55f, 1.8f)
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(51, 170, 170, 170)
        strokeWidth = 1.5f * scale
    }
    listOf(25.0, 50.0, 75.0).forEach { level ->
        val y = toY(level)
        canvas.drawLine(plot.left, y, plot.right, y, gridPaint)
    }

    val path = Path()
    values.forEachIndexed { i, v ->
        val x = toX(i)
        val y = toY(v)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(102, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2.8f * scale
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    })

    values.forEachIndexed { i, v ->
        canvas.drawCircle(
            toX(i),
            toY(v),
            4.6f * scale,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgiAndroidColor(v) }
        )
    }
}

private fun drawFgiGaugeScriptable(canvas: Canvas, value: Double, area: RectF) {
    val baseW = 320f
    val baseH = 180f
    val scale = min(area.width() / baseW, area.height() / baseH)
    if (scale <= 0f) return
    val ox = area.centerX() - baseW * scale / 2f
    val oy = area.centerY() - baseH * scale / 2f

    fun sx(v: Float): Float = ox + v * scale
    fun sy(v: Float): Float = oy + v * scale

    val cx = 160f
    val cy = 160f
    val radius = 120f
    val arcThick = 10f

    for (i in 0 until 120) {
        val t = i / 119.0
        val angle = Math.PI + t * Math.PI
        val x = cx + cos(angle).toFloat() * radius
        val y = cy + sin(angle).toFloat() * radius
        canvas.drawCircle(
            sx(x),
            sy(y),
            arcThick * scale / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgiAndroidColor(t * 100.0) }
        )
    }

    val clamped = value.coerceIn(0.0, 100.0)
    val needleAngle = Math.PI + (clamped / 100.0) * Math.PI
    val needleLen = radius - 8f
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_WHITE }

    for (i in 0 until 35) {
        val t = i / 34f
        val nx = cx + cos(needleAngle).toFloat() * needleLen * t
        val ny = cy + sin(needleAngle).toFloat() * needleLen * t
        val thick = 12f * (1f - t)
        canvas.drawCircle(sx(nx), sy(ny), max(0.7f, thick * scale / 2f), white)
    }

    canvas.drawCircle(sx(cx), sy(cy), 8f * scale, white)

    val labelDist = radius + 22f
    val lx = cx + cos(needleAngle).toFloat() * labelDist
    val ly = cy + sin(needleAngle).toFloat() * labelDist
    drawText(
        canvas,
        clamped.toInt().toString(),
        sx(lx),
        sy(ly) + 8f * scale,
        23f * scale,
        COLOR_WHITE,
        true,
        Paint.Align.CENTER
    )
}

private fun drawFgiStats(
    canvas: Canvas,
    value: Double,
    avg30: Double,
    centerX: Float,
    baseline: Float,
    requestedSize: Float,
    maxWidth: Float
) {
    val now = "현재 ${value.toInt()}"
    val slash = " / "
    val avg = if (avg30.isFinite()) "30일 평균 ${avg30.toInt()}" else "30일 평균 -"

    var size = requestedSize
    fun totalWidth(s: Float): Float =
        textPaint(s, fgiAndroidColor(value), true).measureText(now) +
            textPaint(s + 2f, COLOR_WHITE, true).measureText(slash) +
            textPaint(s, if (avg30.isFinite()) fgiAndroidColor(avg30) else COLOR_P2, true).measureText(avg)

    val initialWidth = totalWidth(size)
    if (initialWidth > maxWidth && initialWidth > 0f) {
        size = (size * maxWidth / initialWidth).coerceAtLeast(size * 0.65f)
    }

    val nowPaint = textPaint(size, fgiAndroidColor(value), true)
    val slashPaint = textPaint(size + 2f, COLOR_WHITE, true)
    val avgPaint = textPaint(size, if (avg30.isFinite()) fgiAndroidColor(avg30) else COLOR_P2, true)
    val total = nowPaint.measureText(now) + slashPaint.measureText(slash) + avgPaint.measureText(avg)
    var x = centerX - total / 2f

    canvas.drawText(now, x, baseline, nowPaint)
    x += nowPaint.measureText(now)
    canvas.drawText(slash, x, baseline, slashPaint)
    x += slashPaint.measureText(slash)
    canvas.drawText(avg, x, baseline, avgPaint)
}

private fun errorCardBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)
    val short = min(width, height).toFloat()
    drawText(canvas, "AgiTQ", width / 2f, height / 2f - short * 0.03f, short * 0.085f, COLOR_WHITE, true, Paint.Align.CENTER)
    drawText(canvas, "데이터 로드 실패", width / 2f, height / 2f + short * 0.07f, short * 0.055f, COLOR_ALERT, true, Paint.Align.CENTER)
    return bitmap
}

private fun drawFittedText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    requestedSize: Float,
    color: Int,
    bold: Boolean,
    maxWidth: Float
) {
    var size = requestedSize
    val measured = textPaint(size, color, bold).measureText(text)
    if (measured > maxWidth && measured > 0f) {
        size = (size * maxWidth / measured).coerceAtLeast(size * 0.62f)
    }
    drawText(canvas, text, x, y, size, color, bold)
}

private fun drawFittedCenteredText(
    canvas: Canvas,
    text: String,
    centerX: Float,
    y: Float,
    requestedSize: Float,
    color: Int,
    bold: Boolean,
    maxWidth: Float
) {
    var size = requestedSize
    val measured = textPaint(size, color, bold).measureText(text)
    if (measured > maxWidth && measured > 0f) {
        size = (size * maxWidth / measured).coerceAtLeast(size * 0.62f)
    }
    drawText(canvas, text, centerX, y, size, color, bold, Paint.Align.CENTER)
}

private fun drawText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Int,
    bold: Boolean = false,
    align: Paint.Align = Paint.Align.LEFT
) {
    canvas.drawText(text, x, y, textPaint(size, color, bold, align))
}

private fun textPaint(
    size: Float,
    color: Int,
    bold: Boolean,
    align: Paint.Align = Paint.Align.LEFT
): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    textSize = size
    textAlign = align
    typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
}

private fun drawdownText(sig: JSONObject?): String {
    if (sig == null) return "TQQQ 최고점 대비 N/A"
    val position = sig.optString("position", "BELOW")
    val dd = sig.optDouble("drawdown", Double.NaN)
    return if (position == "ABOVE" && dd.isFinite()) {
        "TQQQ 최고점 대비 ${String.format(Locale.US, "%.1f%%", dd)}"
    } else {
        "TQQQ 최고점 대비 N/A"
    }
}

/** 기준시각은 한국 표준시(KST)로 계산하고 화면에는 원본처럼 `기준`만 표시. */
private fun formatMarketTime(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "-"
    return runCatching {
        DateTimeFormatter
            .ofPattern("yy.MM.dd. HH:mm '기준'", Locale.KOREA)
            .withZone(ZoneId.of("Asia/Seoul"))
            .format(Instant.ofEpochSecond(epochSeconds))
    }.getOrDefault("-")
}

private fun etfAndroidColor(token: String): Int = when {
    token.contains("TQQQ") -> COLOR_TQQQ
    token.contains("SPYM") -> COLOR_SPYM
    token.contains("SGOV") -> COLOR_SGOV
    else -> COLOR_WHITE
}

private fun fgiRatingFull(rating: String): String = when (rating.lowercase()) {
    "extreme fear" -> "극공포 (Extreme Fear)"
    "fear" -> "공포 (Fear)"
    "neutral" -> "중립 (Neutral)"
    "greed" -> "탐욕 (Greed)"
    "extreme greed" -> "극탐욕 (Extreme Greed)"
    else -> rating.ifBlank { "-" }
}

private fun fgiAndroidColor(value: Double): Int = when {
    value >= 75 -> AndroidColor.rgb(176, 124, 192)
    value >= 55 -> AndroidColor.rgb(91, 184, 232)
    value >= 45 -> AndroidColor.rgb(170, 170, 170)
    value >= 25 -> AndroidColor.rgb(240, 160, 160)
    else -> AndroidColor.rgb(232, 113, 79)
}
