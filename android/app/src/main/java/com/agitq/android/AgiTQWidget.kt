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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val CARD_W = 900
private const val CARD_H = 520

private val COLOR_BG = AndroidColor.BLACK
private val COLOR_WHITE = AndroidColor.WHITE
private val COLOR_GRAY = AndroidColor.rgb(165, 165, 165)
private val COLOR_DARK_GRAY = AndroidColor.rgb(55, 55, 55)
private val COLOR_CYAN = AndroidColor.rgb(128, 223, 255)
private val COLOR_GREEN = AndroidColor.rgb(175, 212, 133)
private val COLOR_PINK = AndroidColor.rgb(224, 112, 192)
private val COLOR_RED = AndroidColor.rgb(255, 95, 95)

class SpxWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val card = data?.optJSONObject("SPX")?.let {
            marketCardBitmap(it, "SPX", "아기티큐 200슨피단", 0.025)
        } ?: errorCardBitmap()
        provideContent { FullCard(context, card, "SPX 투자 전략 위젯") }
    }
}

class SpxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpxWidget()
}

class QqqWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val card = data?.optJSONObject("QQQ")?.let {
            marketCardBitmap(it, "QQQ", "아기티큐 200큐큐단", 0.02)
        } ?: errorCardBitmap()
        provideContent { FullCard(context, card, "QQQ 투자 전략 위젯") }
    }
}

class QqqWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QqqWidget()
}

class FgiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val card = data?.optJSONObject("FGI")?.let { fgiCardBitmap(it) } ?: errorCardBitmap()
        provideContent { FullCard(context, card, "공포 탐욕 지수 위젯") }
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

private fun marketCardBitmap(asset: JSONObject, symbol: String, title: String, bandPct: Double): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)

    val sig = asset.optJSONObject("signal")
    val price = asset.optDouble("price", 0.0)
    val sma = sig?.optDouble("sma", 0.0) ?: 0.0

    drawText(canvas, title, CARD_W / 2f, 48f, 28f, COLOR_WHITE, true, Paint.Align.CENTER)
    drawText(canvas, symbol, 64f, 112f, 28f, COLOR_CYAN, true)
    drawText(canvas, formatPrice(price), 150f, 119f, 56f, COLOR_WHITE, true)

    val distance = formatDistance(price, sma)
    drawText(canvas, "200SMA ${formatPrice(sma)}", 64f, 158f, 23f, COLOR_GRAY)
    drawText(
        canvas,
        distance,
        360f,
        158f,
        23f,
        if (price >= sma) COLOR_GREEN else COLOR_RED,
        true
    )

    drawMarketChart(canvas, asset, bandPct, RectF(64f, 182f, 836f, 354f))

    val signalName = sig?.optString("name", "-") ?: "-"
    val signalColor = when {
        sig?.optBoolean("alert") == true -> COLOR_RED
        sig?.optString("position") == "ABOVE" -> COLOR_GREEN
        sig?.optString("position") == "BELOW" -> COLOR_RED
        else -> COLOR_GRAY
    }
    drawText(canvas, signalName, CARD_W / 2f, 402f, 26f, signalColor, true, Paint.Align.CENTER)
    drawText(canvas, signalSummary(sig), CARD_W / 2f, 445f, 25f, COLOR_WHITE, true, Paint.Align.CENTER)
    drawText(canvas, drawdownText(sig), CARD_W / 2f, 486f, 21f, COLOR_GRAY, false, Paint.Align.CENTER)

    return bitmap
}

private fun drawMarketChart(canvas: Canvas, asset: JSONObject, bandPct: Double, area: RectF) {
    val arr = asset.optJSONArray("closes") ?: return
    val closes = ArrayList<Double>(arr.length())
    for (i in 0 until arr.length()) {
        val v = arr.optDouble(i, Double.NaN)
        if (v.isFinite() && v > 0.0) closes += v
    }
    if (closes.size < 200) return

    val sma = arrayOfNulls<Double>(closes.size)
    var sum = 0.0
    for (i in closes.indices) {
        sum += closes[i]
        if (i >= 200) sum -= closes[i - 200]
        if (i >= 199) sma[i] = sum / 200.0
    }

    val start = max(0, closes.size - 90)
    val prices = closes.subList(start, closes.size)
    val upper = (start until closes.size).map { sma[it]?.times(1.0 + bandPct) }
    val lower = (start until closes.size).map { sma[it]?.times(1.0 - bandPct) }

    val values = mutableListOf<Double>()
    values += prices
    upper.filterNotNullTo(values)
    lower.filterNotNullTo(values)
    if (values.isEmpty()) return

    var minV = values.minOrNull() ?: return
    var maxV = values.maxOrNull() ?: return
    if (maxV <= minV) maxV = minV + 1.0
    val rangePad = (maxV - minV) * 0.06
    minV -= rangePad
    maxV += rangePad

    val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_DARK_GRAY
        strokeWidth = 1.4f
    }
    for (k in 0..3) {
        val y = area.top + area.height() * k / 3f
        canvas.drawLine(area.left, y, area.right, y, grid)
    }

    fun x(i: Int): Float = area.left + if (prices.size <= 1) 0f else i.toFloat() / (prices.size - 1) * area.width()
    fun y(v: Double): Float = area.top + ((maxV - v) / (maxV - minV) * area.height()).toFloat()

    fun series(data: List<Double?>, color: Int, width: Float) {
        val p = Path()
        var started = false
        data.forEachIndexed { i, v ->
            if (v == null || !v.isFinite()) {
                started = false
            } else {
                val px = x(i)
                val py = y(v)
                if (!started) {
                    p.moveTo(px, py)
                    started = true
                } else {
                    p.lineTo(px, py)
                }
            }
        }
        canvas.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
    }

    series(lower, COLOR_GREEN, 3.2f)
    series(upper, COLOR_PINK, 3.2f)
    series(prices.map { it }, COLOR_CYAN, 4.8f)

    val lastX = x(prices.lastIndex)
    val lastY = y(prices.last())
    canvas.drawCircle(lastX, lastY, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_WHITE })

    drawText(canvas, "90일", area.left, area.bottom + 23f, 18f, COLOR_GRAY)
    drawText(canvas, "현재", area.right, area.bottom + 23f, 18f, COLOR_GRAY, false, Paint.Align.RIGHT)
}

private fun fgiCardBitmap(fgi: JSONObject): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)

    val value = fgi.optDouble("value", Double.NaN)
    if (!value.isFinite()) return errorCardBitmap()
    val avg30 = fgi.optDouble("avg30", Double.NaN)
    val rating = fgiRatingKo(fgi.optString("rating"))
    val scoreColor = fgiAndroidColor(value)

    drawText(canvas, "공포·탐욕 지수", CARD_W / 2f, 50f, 30f, COLOR_WHITE, true, Paint.Align.CENTER)
    drawText(canvas, value.toInt().toString(), 390f, 132f, 76f, scoreColor, true, Paint.Align.RIGHT)
    drawText(canvas, rating, 420f, 122f, 31f, scoreColor, true)

    drawFgiGauge(canvas, value)

    val avgText = if (avg30.isFinite()) {
        "30일 평균 ${avg30.toInt()}   ·   ${formatFgiDelta(value, avg30)}"
    } else {
        "30일 평균 -"
    }
    drawText(canvas, avgText, CARD_W / 2f, 420f, 23f, COLOR_GRAY, false, Paint.Align.CENTER)

    drawFgiHistory(canvas, fgi, RectF(110f, 447f, 790f, 502f))
    return bitmap
}

private fun drawFgiGauge(canvas: Canvas, value: Double) {
    val cx = CARD_W / 2f
    val cy = 350f
    val radius = 225f
    val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
    val colors = intArrayOf(
        AndroidColor.rgb(232, 113, 79),
        AndroidColor.rgb(240, 160, 160),
        AndroidColor.rgb(170, 170, 170),
        AndroidColor.rgb(91, 184, 232),
        AndroidColor.rgb(176, 124, 192)
    )
    val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 38f
        strokeCap = Paint.Cap.BUTT
    }
    for (i in 0..4) {
        arcPaint.color = colors[i]
        canvas.drawArc(rect, 180f + i * 36f, 36f, false, arcPaint)
    }

    val clamped = value.coerceIn(0.0, 100.0)
    val angle = Math.toRadians(180.0 + clamped * 1.8)
    val needleRadius = radius - 36f
    val nx = cx + (cos(angle) * needleRadius).toFloat()
    val ny = cy + (sin(angle) * needleRadius).toFloat()
    val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_WHITE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(cx, cy, nx, ny, needle)
    canvas.drawCircle(cx, cy, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_WHITE })

    drawText(canvas, "극공포", 94f, 374f, 23f, COLOR_GRAY, false, Paint.Align.CENTER)
    drawText(canvas, "중립", cx, 126f, 23f, COLOR_GRAY, false, Paint.Align.CENTER)
    drawText(canvas, "극탐욕", CARD_W - 94f, 374f, 23f, COLOR_GRAY, false, Paint.Align.CENTER)
}

private fun drawFgiHistory(canvas: Canvas, fgi: JSONObject, area: RectF) {
    val arr = fgi.optJSONArray("history") ?: return
    if (arr.length() < 2) return
    val values = mutableListOf<Double>()
    val start = max(0, arr.length() - 90)
    for (i in start until arr.length()) {
        val v = arr.optJSONObject(i)?.optDouble("y", Double.NaN) ?: Double.NaN
        if (v.isFinite()) values += v.coerceIn(0.0, 100.0)
    }
    if (values.size < 2) return

    val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_DARK_GRAY
        strokeWidth = 1.4f
    }
    val midY = area.top + area.height() / 2f
    canvas.drawLine(area.left, midY, area.right, midY, grid)

    val path = Path()
    values.forEachIndexed { i, v ->
        val x = area.left + i.toFloat() / (values.size - 1) * area.width()
        val y = area.top + ((100.0 - v) / 100.0 * area.height()).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3.2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    })
}

private fun errorCardBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(COLOR_BG)
    drawText(canvas, "AgiTQ", CARD_W / 2f, 235f, 44f, COLOR_WHITE, true, Paint.Align.CENTER)
    drawText(canvas, "데이터 로드 실패", CARD_W / 2f, 290f, 28f, COLOR_RED, true, Paint.Align.CENTER)
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
    canvas.drawText(text, x, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        textAlign = align
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    })
}

private fun signalSummary(sig: JSONObject?): String {
    if (sig == null) return "-"
    val lines = sig.optJSONArray("lines") ?: return "-"
    val parts = mutableListOf<String>()
    for (i in 0 until min(lines.length(), 2)) {
        val row = lines.optJSONArray(i) ?: continue
        parts += "${row.optString(0)} ${row.optString(1)}"
    }
    return parts.joinToString("  ·  ").ifBlank { "-" }
}

private fun drawdownText(sig: JSONObject?): String {
    if (sig == null) return "TQQQ 최고점 대비 -"
    val dd = sig.optDouble("drawdown", Double.NaN)
    return if (dd.isFinite()) "TQQQ 최고점 대비 ${formatPercent(dd)}" else "TQQQ 최고점 대비 N/A"
}

private fun formatPrice(value: Double): String =
    if (value >= 1000.0) String.format("%,.0f", value) else String.format("%.2f", value)

private fun formatPercent(value: Double): String = String.format("%.1f%%", value)

private fun formatDistance(price: Double, sma: Double): String {
    if (price <= 0.0 || sma <= 0.0) return "-"
    return String.format("%+.1f%%", (price / sma - 1.0) * 100.0)
}

private fun formatFgiDelta(value: Double, avg: Double): String =
    String.format("평균대비 %+.0f", value - avg)

private fun fgiRatingKo(rating: String): String = when (rating.lowercase()) {
    "extreme fear" -> "극공포"
    "fear" -> "공포"
    "neutral" -> "중립"
    "greed" -> "탐욕"
    "extreme greed" -> "극탐욕"
    else -> rating.ifBlank { "-" }
}

private fun fgiAndroidColor(value: Double): Int = when {
    value >= 75 -> COLOR_PINK
    value >= 55 -> COLOR_CYAN
    value >= 45 -> COLOR_GRAY
    value >= 25 -> AndroidColor.rgb(240, 160, 160)
    else -> AndroidColor.rgb(232, 113, 79)
}
