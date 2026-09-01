package com.agitq.android

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val black = ColorProvider(Color.Black)
private val white = ColorProvider(Color.White)
private val gray = ColorProvider(Color(0xFFAAAAAA))
private val red = ColorProvider(Color(0xFFFF4D4D))
private val cyan = ColorProvider(Color(0xFF80DFFF))
private val green = ColorProvider(Color(0xFFAFD485))
private val pink = ColorProvider(Color(0xFFE070C0))

class SpxWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val chart = data?.optJSONObject("SPX")?.let { marketChartBitmap(it, 0.025) }
        provideContent { MarketWidgetContent(context, data, "SPX", "아기티큐 200슨피단", chart) }
    }
}

class SpxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpxWidget()
}

class QqqWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val chart = data?.optJSONObject("QQQ")?.let { marketChartBitmap(it, 0.02) }
        provideContent { MarketWidgetContent(context, data, "QQQ", "아기티큐 200큐큐단", chart) }
    }
}

class QqqWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QqqWidget()
}

class FgiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        val fgi = data?.optJSONObject("FGI")
        val value = fgi?.optDouble("value", Double.NaN) ?: Double.NaN
        val gauge = if (value.isFinite()) fgiGaugeBitmap(value) else null
        val history = fgi?.let { fgiHistoryBitmap(it) }
        provideContent { FgiWidgetContent(context, data, gauge, history) }
    }
}

class FgiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FgiWidget()
}

private suspend fun loadData(): JSONObject? = withContext(Dispatchers.IO) {
    runCatching { AgiTQApi.load() }.getOrNull()
}

@Composable
private fun MarketWidgetContent(
    context: Context,
    data: JSONObject?,
    symbol: String,
    title: String,
    chart: Bitmap?
) {
    val size = LocalSize.current
    val asset = data?.optJSONObject(symbol)
    val sig = asset?.optJSONObject("signal")
    val price = asset?.optDouble("price", 0.0) ?: 0.0
    val sma = sig?.optDouble("sma", 0.0) ?: 0.0
    val compact = size.width < 220.dp
    val shortHeight = size.height < 135.dp

    WidgetShell(context) {
        if (data == null || asset == null) {
            ErrorView()
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                if (!compact) {
                    Text(title, style = TextStyle(color = white, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.height(2.dp))
                }

                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(symbol, style = TextStyle(color = cyan, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(7.dp))
                    Text(formatPrice(price), style = TextStyle(color = white, fontSize = if (compact) 20.sp else 23.sp, fontWeight = FontWeight.Bold))
                }
                Text(
                    "200SMA ${formatPrice(sma)}  ·  ${formatDistance(price, sma)}",
                    style = TextStyle(color = gray, fontSize = 8.sp)
                )

                if (chart != null && !shortHeight) {
                    Spacer(GlanceModifier.height(3.dp))
                    Image(
                        provider = ImageProvider(chart),
                        contentDescription = "$symbol 90일 가격 및 200일 밴드 차트",
                        modifier = GlanceModifier.fillMaxWidth().height(if (compact) 34.dp else 55.dp),
                        contentScale = ContentScale.FillBounds
                    )
                }

                Spacer(GlanceModifier.height(3.dp))
                Text(
                    sig?.optString("name", "-") ?: "-",
                    style = TextStyle(color = signalColor(sig), fontSize = if (compact) 8.sp else 9.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    signalSummary(sig),
                    style = TextStyle(color = white, fontSize = if (compact) 8.sp else 9.sp, fontWeight = FontWeight.Bold)
                )
                if (!shortHeight) {
                    Text(
                        drawdownText(sig),
                        style = TextStyle(color = gray, fontSize = 8.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FgiWidgetContent(context: Context, data: JSONObject?, gauge: Bitmap?, history: Bitmap?) {
    val size = LocalSize.current
    val fgi = data?.optJSONObject("FGI")
    val value = fgi?.optDouble("value", Double.NaN) ?: Double.NaN
    val avg30 = fgi?.optDouble("avg30", Double.NaN) ?: Double.NaN
    val compact = size.width < 220.dp
    val roomy = size.height >= 165.dp

    WidgetShell(context) {
        if (data == null || fgi == null || !value.isFinite()) {
            ErrorView()
        } else {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    "공포·탐욕 지수",
                    style = TextStyle(color = white, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold)
                )
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        value.toInt().toString(),
                        style = TextStyle(color = fgiColor(value), fontSize = if (compact) 27.sp else 32.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        fgiRatingKo(fgi.optString("rating")),
                        style = TextStyle(color = fgiColor(value), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }

                if (gauge != null) {
                    Image(
                        provider = ImageProvider(gauge),
                        contentDescription = "공포 탐욕 지수 게이지 ${value.toInt()}",
                        modifier = GlanceModifier.fillMaxWidth().height(if (compact) 42.dp else 52.dp),
                        contentScale = ContentScale.FillBounds
                    )
                }

                Text(
                    if (avg30.isFinite()) "30일 평균 ${avg30.toInt()}  ·  ${formatFgiDelta(value, avg30)}" else "30일 평균 -",
                    style = TextStyle(color = gray, fontSize = 8.sp)
                )

                if (roomy && history != null) {
                    Spacer(GlanceModifier.height(2.dp))
                    Image(
                        provider = ImageProvider(history),
                        contentDescription = "공포 탐욕 지수 최근 추이",
                        modifier = GlanceModifier.fillMaxWidth().height(28.dp),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
    }
}

private fun dashboardIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
    action = "com.agitq.android.action.OPEN_DASHBOARD"
    data = Uri.parse("agitq://dashboard")
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

@Composable
private fun WidgetShell(context: Context, content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(black)
            .clickable(actionStartActivity(dashboardIntent(context)))
            .padding(8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) { content() }
}

@Composable
private fun ErrorView() {
    Text("AgiTQ\n데이터 로드 실패", style = TextStyle(color = red, fontWeight = FontWeight.Bold, fontSize = 11.sp))
}

private fun signalSummary(sig: JSONObject?): String {
    if (sig == null) return "-"
    val lines = sig.optJSONArray("lines") ?: return "-"
    val parts = mutableListOf<String>()
    for (i in 0 until min(lines.length(), 2)) {
        val row = lines.optJSONArray(i) ?: continue
        parts += "${row.optString(0)} ${row.optString(1)}"
    }
    return parts.joinToString(" · ").ifBlank { "-" }
}

private fun signalColor(sig: JSONObject?): ColorProvider {
    if (sig?.optBoolean("alert") == true) return red
    return when (sig?.optString("position")) {
        "ABOVE" -> green
        "BELOW" -> red
        else -> gray
    }
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
    val pct = (price / sma - 1.0) * 100.0
    return String.format("%+.1f%%", pct)
}

private fun formatFgiDelta(value: Double, avg: Double): String {
    val delta = value - avg
    return String.format("평균대비 %+.0f", delta)
}

private fun fgiRatingKo(rating: String): String = when (rating.lowercase()) {
    "extreme fear" -> "극공포"
    "fear" -> "공포"
    "neutral" -> "중립"
    "greed" -> "탐욕"
    "extreme greed" -> "극탐욕"
    else -> rating.ifBlank { "-" }
}

private fun fgiColor(value: Double): ColorProvider = when {
    value >= 75 -> pink
    value >= 55 -> cyan
    value >= 45 -> gray
    value >= 25 -> ColorProvider(Color(0xFFF0A0A0))
    else -> ColorProvider(Color(0xFFE8714F))
}

private fun marketChartBitmap(asset: JSONObject, bandPct: Double): Bitmap? {
    val arr = asset.optJSONArray("closes") ?: return null
    val closes = ArrayList<Double>(arr.length())
    for (i in 0 until arr.length()) {
        val v = arr.optDouble(i, Double.NaN)
        if (v.isFinite() && v > 0.0) closes += v
    }
    if (closes.size < 200) return null

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
    if (values.isEmpty()) return null

    var minV = values.minOrNull() ?: return null
    var maxV = values.maxOrNull() ?: return null
    if (maxV <= minV) maxV = minV + 1.0
    val padRange = (maxV - minV) * 0.06
    minV -= padRange
    maxV += padRange

    val w = 520
    val h = 120
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.BLACK)

    val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(34, 34, 34)
        strokeWidth = 1.5f
    }
    for (k in 1..3) {
        val y = h * k / 4f
        canvas.drawLine(0f, y, w.toFloat(), y, grid)
    }

    fun x(i: Int): Float = if (prices.size <= 1) 0f else i.toFloat() / (prices.size - 1) * (w - 1)
    fun y(v: Double): Float = ((maxV - v) / (maxV - minV) * (h - 8) + 4).toFloat()

    fun drawSeries(series: List<Double?>, color: Int, stroke: Float) {
        val path = Path()
        var started = false
        series.forEachIndexed { i, v ->
            if (v == null || !v.isFinite()) {
                started = false
            } else if (!started) {
                path.moveTo(x(i), y(v))
                started = true
            } else {
                path.lineTo(x(i), y(v))
            }
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
    }

    drawSeries(lower, AndroidColor.rgb(175, 212, 133), 2.2f)
    drawSeries(upper, AndroidColor.rgb(224, 112, 192), 2.2f)
    drawSeries(prices.map { it }, AndroidColor.rgb(128, 223, 255), 3.2f)

    val last = prices.last()
    canvas.drawCircle(x(prices.lastIndex), y(last), 4.2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    })
    return bitmap
}

private fun fgiGaugeBitmap(value: Double): Bitmap {
    val w = 520
    val h = 150
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.BLACK)

    val cx = w / 2f
    val cy = 126f
    val radius = 106f
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
        strokeWidth = 20f
        strokeCap = Paint.Cap.BUTT
    }
    for (i in 0..4) {
        arcPaint.color = colors[i]
        canvas.drawArc(rect, 180f + i * 36f, 36f, false, arcPaint)
    }

    val clamped = value.coerceIn(0.0, 100.0)
    val angle = Math.toRadians(180.0 + clamped * 1.8)
    val needleRadius = radius - 19f
    val nx = cx + (cos(angle) * needleRadius).toFloat()
    val ny = cy + (sin(angle) * needleRadius).toFloat()
    val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(cx, cy, nx, ny, needle)
    canvas.drawCircle(cx, cy, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE })

    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(150, 150, 150)
        textSize = 19f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("극공포", 80f, 146f, label)
    canvas.drawText("중립", cx, 32f, label)
    canvas.drawText("극탐욕", w - 80f, 146f, label)
    return bitmap
}

private fun fgiHistoryBitmap(fgi: JSONObject): Bitmap? {
    val arr = fgi.optJSONArray("history") ?: return null
    if (arr.length() < 2) return null
    val values = mutableListOf<Double>()
    val start = max(0, arr.length() - 90)
    for (i in start until arr.length()) {
        val v = arr.optJSONObject(i)?.optDouble("y", Double.NaN) ?: Double.NaN
        if (v.isFinite()) values += v.coerceIn(0.0, 100.0)
    }
    if (values.size < 2) return null

    val w = 520
    val h = 80
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.BLACK)

    val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(34, 34, 34)
        strokeWidth = 1.2f
    }
    canvas.drawLine(0f, h / 2f, w.toFloat(), h / 2f, grid)

    val path = Path()
    values.forEachIndexed { i, v ->
        val x = i.toFloat() / (values.size - 1) * (w - 1)
        val y = ((100.0 - v) / 100.0 * (h - 6) + 3).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    })
    return bitmap
}
