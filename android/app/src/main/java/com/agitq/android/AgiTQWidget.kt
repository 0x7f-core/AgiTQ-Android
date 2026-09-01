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
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
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
import kotlin.math.sin

private const val CARD_W = 900
private const val CARD_H = 520
private const val SCRIPTABLE_VERSION = "v3.0-γ (26.08.12)"

private val COLOR_BG = AndroidColor.BLACK
private val COLOR_WHITE = AndroidColor.WHITE
private val COLOR_P2 = AndroidColor.rgb(170, 170, 170)
private val COLOR_DOT = AndroidColor.rgb(102, 102, 102)
private val COLOR_DIVIDER = AndroidColor.rgb(51, 51, 51)
private val COLOR_TQQQ = AndroidColor.rgb(232, 113, 79)
private val COLOR_SPYM = AndroidColor.rgb(176, 124, 192)
private val COLOR_SGOV = AndroidColor.rgb(91, 184, 232)
private val COLOR_CP = AndroidColor.rgb(128, 223, 255)
private val COLOR_UPPER = AndroidColor.rgb(224, 112, 192)
private val COLOR_LOWER = AndroidColor.rgb(175, 212, 133)
private val COLOR_ALERT = AndroidColor.rgb(255, 77, 77)

class SpxWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val card = data?.optJSONObject("SPX")?.let {
            marketCardBitmap(it, "아기티큐 200슨피단 (SPX)", 0.025)
        } ?: errorCardBitmap()
        provideContent { FullCard(context, card, "아기티큐 200슨피단 SPX") }
    }
}

class SpxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpxWidget()
}

class QqqWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val card = data?.optJSONObject("QQQ")?.let {
            marketCardBitmap(it, "아기티큐 200큐큐단 (QQQ)", 0.02)
        } ?: errorCardBitmap()
        provideContent { FullCard(context, card, "아기티큐 200큐큐단 QQQ") }
    }
}

class QqqWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QqqWidget()
}

class FgiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val card = data?.optJSONObject("FGI")?.let { fgiCardBitmap(it) } ?: errorCardBitmap()
        provideContent { FullCard(context, card, "공포와 탐욕 지수 CNN FGI") }
    }
}

class FgiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FgiWidget()
}

private suspend fun loadData(): JSONObject? = withContext(Dispatchers.IO) {
    runCatching { AgiTQApi.load() }.getOrNull()
}

@Composable
private fun FullCard(context: Context, bitmap: Bitmap, description: String) {
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = description,
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(dashboardIntent(context))),
        contentScale = ContentScale.FillBounds
    )
}

private fun dashboardIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
    action = "com.agitq.android.action.OPEN_DASHBOARD"
    data = Uri.parse("agitq://dashboard")
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

/**
 * SPX/QQQ 독립 카드.
 * 원본 iOS Scriptable Medium/Large 상단의 정보 구조를 그대로 사용한다:
 * 제목 + 버전 / 미국 동부 기준 시각 / 왼쪽 90일 밴드 차트 / 오른쪽 전략 신호.
 */
private fun marketCardBitmap(asset: JSONObject, title: String, bandPct: Double): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)

    val sig = asset.optJSONObject("signal")

    drawText(canvas, title, 30f, 48f, 32f, COLOR_WHITE, true)
    drawText(canvas, SCRIPTABLE_VERSION, CARD_W - 30f, 46f, 17f, COLOR_P2, false, Paint.Align.RIGHT)
    drawText(canvas, formatMarketTime(asset.optLong("mTime", 0L)), 30f, 86f, 21f, COLOR_WHITE)

    drawMarketChartScriptable(canvas, asset, bandPct, RectF(30f, 124f, 462f, 388f))
    drawSignalBlock(canvas, sig, 500f, 150f)

    return bitmap
}

/** 원본 Scriptable drawBandChart(500x300)의 비율/색/선 굵기를 카드 크기에 맞게 재현. */
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

    fun series(data: List<Double?>, color: Int, width: Float) {
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
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
    }

    series(upper, COLOR_UPPER, 3.8f)
    series(lower, COLOR_LOWER, 3.8f)
    series(prices.map { it }, COLOR_CP, 6.0f)
}

private fun drawSignalBlock(canvas: Canvas, sig: JSONObject?, x: Float, top: Float) {
    if (sig == null) return
    val isAlert = sig.optBoolean("alert", false)
    val lines = sig.optJSONArray("lines")
    var y = top + 52f

    if (lines != null) {
        for (i in 0 until min(lines.length(), 2)) {
            val row = lines.optJSONArray(i) ?: continue
            drawSignalRow(canvas, row.optString(0), row.optString(1), x, y, 35f, isAlert)
            y += 58f
        }
    }

    y += 12f
    drawText(
        canvas,
        sig.optString("name", "-"),
        x,
        y,
        23f,
        if (isAlert) COLOR_ALERT else COLOR_P2
    )

    y += 52f
    drawText(canvas, drawdownText(sig), x, y, 27f, COLOR_WHITE, true)
}

private fun drawSignalRow(
    canvas: Canvas,
    token: String,
    action: String,
    startX: Float,
    baseline: Float,
    size: Float,
    isAlert: Boolean
) {
    var x = startX
    val boldPaint = textPaint(size, COLOR_WHITE, true)

    if (isAlert) {
        val line = "$token $action"
        drawText(canvas, line, x, baseline, size, COLOR_ALERT, true)
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

/**
 * FGI 독립 카드.
 * 원본 Scriptable Large 하단 FGI 블록을 그대로 독립 카드로 확장한다:
 * 왼쪽 90일 히스토리 + 오른쪽 게이지/등급/현재값/30일 평균.
 */
private fun fgiCardBitmap(fgi: JSONObject): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)

    val value = fgi.optDouble("value", Double.NaN)
    if (!value.isFinite()) return errorCardBitmap()
    val avg30 = fgi.optDouble("avg30", Double.NaN)
    val rating = fgiRatingFull(fgi.optString("rating"))

    drawText(canvas, "공포와 탐욕 지수 (CNN FGI)", 30f, 50f, 32f, COLOR_WHITE, true)

    drawFgiHistoryScriptable(canvas, fgi, RectF(30f, 118f, 468f, 440f))
    drawFgiGaugeScriptable(canvas, value, RectF(520f, 105f, 870f, 302f))

    drawText(
        canvas,
        rating,
        695f,
        356f,
        31f,
        fgiAndroidColor(value),
        true,
        Paint.Align.CENTER
    )

    drawFgiStats(canvas, value, avg30, 695f, 414f, 26f)
    return bitmap
}

/** 원본 Scriptable drawFGIChart(480x300): 25/50/75 기준선 + 반투명 흰 선 + 값별 컬러 점. */
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

    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(51, 170, 170, 170)
        strokeWidth = 1.5f
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
        strokeWidth = 2.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    })

    values.forEachIndexed { i, v ->
        canvas.drawCircle(
            toX(i),
            toY(v),
            4.6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgiAndroidColor(v) }
        )
    }
}

/** 원본 Scriptable drawGauge(320x180)를 좌표 스케일링해서 동일하게 재현. */
private fun drawFgiGaugeScriptable(canvas: Canvas, value: Double, area: RectF) {
    val baseW = 320f
    val baseH = 180f
    val scale = min(area.width() / baseW, area.height() / baseH)
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

private fun drawFgiStats(canvas: Canvas, value: Double, avg30: Double, centerX: Float, baseline: Float, size: Float) {
    val now = "현재 ${value.toInt()}"
    val slash = " / "
    val avg = if (avg30.isFinite()) "30일 평균 ${avg30.toInt()}" else "30일 평균 -"

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

private fun errorCardBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)
    drawText(canvas, "AgiTQ", CARD_W / 2f, 235f, 44f, COLOR_WHITE, true, Paint.Align.CENTER)
    drawText(canvas, "데이터 로드 실패", CARD_W / 2f, 290f, 28f, COLOR_ALERT, true, Paint.Align.CENTER)
    return bitmap
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

private fun formatMarketTime(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "-"
    return runCatching {
        DateTimeFormatter
            .ofPattern("yy.MM.dd. HH:mm '기준'", Locale.KOREA)
            .withZone(ZoneId.of("America/New_York"))
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
