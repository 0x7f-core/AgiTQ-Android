package com.agitq.android

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AgiTQWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) {
            runCatching { AgiTQApi.load() }.getOrNull()
        }
        provideContent { WidgetContent(data) }
    }
}

class AgiTQWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgiTQWidget()
}

private val black = ColorProvider(Color.Black)
private val white = ColorProvider(Color.White)
private val gray = ColorProvider(Color(0xFFAAAAAA))
private val red = ColorProvider(Color(0xFFFF4D4D))
private val cyan = ColorProvider(Color(0xFF80DFFF))

@Composable
private fun WidgetContent(data: JSONObject?) {
    val size = LocalSize.current
    val small = size.width < 180.dp
    val medium = size.width < 310.dp

    Column(
        modifier = GlanceModifier.fillMaxSize().background(black).padding(10.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        when {
            data == null -> ErrorView()
            small -> SmallView(data)
            medium -> MediumView(data)
            else -> LargeView(data)
        }
    }
}

@Composable
private fun ErrorView() {
    Text(
        "AgiTQ\n데이터 로드 실패",
        style = TextStyle(color = red, fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun SmallView(data: JSONObject) {
    val fgi = data.optJSONObject("FGI") ?: JSONObject()
    val value = fgi.optDouble("value", 0.0)

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text("공포·탐욕 지수", style = TextStyle(color = white, fontWeight = FontWeight.Bold, fontSize = 12.sp))
        Spacer(GlanceModifier.height(3.dp))
        Text("${value.toInt()}", style = TextStyle(color = fgiColor(value), fontWeight = FontWeight.Bold, fontSize = 30.sp))
        Text(fgiRatingKo(fgi.optString("rating")), style = TextStyle(color = fgiColor(value), fontWeight = FontWeight.Bold, fontSize = 11.sp))
    }
}

@Composable
private fun MediumView(data: JSONObject) {
    val qqq = data.optJSONObject("QQQ") ?: JSONObject()
    val sig = qqq.optJSONObject("signal") ?: JSONObject()
    val price = qqq.optDouble("price", 0.0)
    val sma = sig.optDouble("sma", 0.0)
    val dd = sig.optDouble("drawdown", 0.0)

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("아기티큐 200큐큐단", style = TextStyle(color = white, fontWeight = FontWeight.Bold, fontSize = 13.sp))
                Text("QQQ", style = TextStyle(color = cyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
            }
            Column(horizontalAlignment = Alignment.Horizontal.End) {
                Text(formatPrice(price), style = TextStyle(color = white, fontWeight = FontWeight.Bold, fontSize = 19.sp))
                Text("200SMA ${formatPrice(sma)}", style = TextStyle(color = gray, fontSize = 8.sp))
            }
        }
        Spacer(GlanceModifier.height(5.dp))
        Text(sig.optString("name", "-"), style = TextStyle(color = if (sig.optBoolean("alert")) red else gray, fontSize = 10.sp))
        Spacer(GlanceModifier.height(2.dp))
        Text(signalSummary(sig), style = TextStyle(color = white, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Text("TQQQ 최고점 대비 ${formatPercent(dd)}", style = TextStyle(color = gray, fontSize = 9.sp))
    }
}

@Composable
private fun LargeView(data: JSONObject) {
    val spx = data.optJSONObject("SPX") ?: JSONObject()
    val qqq = data.optJSONObject("QQQ") ?: JSONObject()
    val fgi = data.optJSONObject("FGI") ?: JSONObject()
    val spxSig = spx.optJSONObject("signal") ?: JSONObject()
    val qqqSig = qqq.optJSONObject("signal") ?: JSONObject()
    val value = fgi.optDouble("value", 0.0)

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text("아기티큐 200슨피단 (SPX)", style = TextStyle(color = white, fontWeight = FontWeight.Bold, fontSize = 14.sp))
        Spacer(GlanceModifier.height(2.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(formatPrice(spx.optDouble("price", 0.0)), style = TextStyle(color = cyan, fontWeight = FontWeight.Bold, fontSize = 23.sp))
                Text("200SMA ${formatPrice(spxSig.optDouble("sma", 0.0))}", style = TextStyle(color = gray, fontSize = 8.sp))
            }
            Column(horizontalAlignment = Alignment.Horizontal.End) {
                Text(value.toInt().toString(), style = TextStyle(color = fgiColor(value), fontWeight = FontWeight.Bold, fontSize = 21.sp))
                Text("FGI ${fgiRatingKo(fgi.optString("rating"))}", style = TextStyle(color = fgiColor(value), fontSize = 8.sp))
            }
        }
        Spacer(GlanceModifier.height(5.dp))
        Text(spxSig.optString("name", "-"), style = TextStyle(color = if (spxSig.optBoolean("alert")) red else gray, fontSize = 9.sp))
        Text(signalSummary(spxSig), style = TextStyle(color = white, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Text("SPX TQQQ DD ${formatPercent(spxSig.optDouble("drawdown", 0.0))} · QQQ ${formatPrice(qqq.optDouble("price", 0.0))}", style = TextStyle(color = gray, fontSize = 8.sp))
        Spacer(GlanceModifier.height(2.dp))
        Text(fgiBar(value), style = TextStyle(color = fgiColor(value), fontSize = 8.sp))
        Text("FGI 30일 평균 ${fgi.optDouble("avg30", 0.0).toInt()}", style = TextStyle(color = gray, fontSize = 8.sp))

        // QQQ signal remains available in the payload for future expanded layouts.
        qqqSig.optString("name", "")
    }
}

private fun signalSummary(sig: JSONObject): String {
    val lines = sig.optJSONArray("lines") ?: return ""
    val parts = mutableListOf<String>()
    for (i in 0 until lines.length()) {
        val row = lines.optJSONArray(i) ?: continue
        parts += "${row.optString(0)} ${row.optString(1)}"
    }
    return parts.joinToString(" · ")
}

private fun formatPrice(value: Double): String =
    if (value >= 1000.0) String.format("%,.0f", value) else String.format("%.2f", value)

private fun formatPercent(value: Double): String = String.format("%.1f%%", value)

private fun fgiRatingKo(rating: String): String = when (rating.lowercase()) {
    "extreme fear" -> "극공포"
    "fear" -> "공포"
    "neutral" -> "중립"
    "greed" -> "탐욕"
    "extreme greed" -> "극탐욕"
    else -> rating.ifBlank { "-" }
}

private fun fgiColor(value: Double): ColorProvider = when {
    value >= 75 -> ColorProvider(Color(0xFFB07CC0))
    value >= 55 -> ColorProvider(Color(0xFF5BB8E8))
    value >= 45 -> ColorProvider(Color(0xFFAAAAAA))
    value >= 25 -> ColorProvider(Color(0xFFF0A0A0))
    else -> ColorProvider(Color(0xFFE8714F))
}

private fun fgiBar(value: Double): String {
    val total = 18
    val filled = kotlin.math.round(value.coerceIn(0.0, 100.0) / 100.0 * total).toInt()
    return "▰".repeat(filled) + "▱".repeat(total - filled) + "  ${value.toInt()}/100"
}
