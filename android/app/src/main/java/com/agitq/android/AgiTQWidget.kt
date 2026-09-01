package com.agitq.android

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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

private val black = ColorProvider(Color.Black)
private val white = ColorProvider(Color.White)
private val gray = ColorProvider(Color(0xFFAAAAAA))
private val red = ColorProvider(Color(0xFFFF4D4D))
private val cyan = ColorProvider(Color(0xFF80DFFF))

class SpxWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        provideContent { SpxWidgetContent(data) }
    }
}

class SpxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpxWidget()
}

class QqqWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        provideContent { QqqWidgetContent(data) }
    }
}

class QqqWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QqqWidget()
}

class FgiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData()
        provideContent { FgiWidgetContent(data) }
    }
}

class FgiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FgiWidget()
}

private suspend fun loadData(): JSONObject? = withContext(Dispatchers.IO) {
    runCatching { AgiTQApi.load() }.getOrNull()
}

@Composable
private fun SpxWidgetContent(data: JSONObject?) {
    val size = LocalSize.current
    val spx = data?.optJSONObject("SPX")
    val sig = spx?.optJSONObject("signal")
    val price = spx?.optDouble("price", 0.0) ?: 0.0
    val sma = sig?.optDouble("sma", 0.0) ?: 0.0
    val dd = sig?.optDouble("drawdown", 0.0) ?: 0.0
    val compact = size.width < 220.dp

    WidgetShell {
        if (data == null || spx == null) {
            ErrorView()
        } else if (compact) {
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Text("SPX", style = TextStyle(color = gray, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                Text(formatPrice(price), style = TextStyle(color = cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold))
                Text("200SMA ${formatPrice(sma)}", style = TextStyle(color = gray, fontSize = 9.sp))
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text("아기티큐 200슨피단", style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(4.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Column {
                        Text("SPX", style = TextStyle(color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        Text(formatPrice(price), style = TextStyle(color = white, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(GlanceModifier.height(1.dp))
                    Column(horizontalAlignment = Alignment.Horizontal.End) {
                        Text("200SMA", style = TextStyle(color = gray, fontSize = 9.sp))
                        Text(formatPrice(sma), style = TextStyle(color = gray, fontSize = 11.sp))
                    }
                }
                Spacer(GlanceModifier.height(4.dp))
                Text(sig?.optString("name", "-") ?: "-", style = TextStyle(color = if (sig?.optBoolean("alert") == true) red else gray, fontSize = 10.sp))
                Text(signalSummary(sig), style = TextStyle(color = white, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Text("최고점 대비 ${formatPercent(dd)}", style = TextStyle(color = gray, fontSize = 9.sp))
            }
        }
    }
}

@Composable
private fun QqqWidgetContent(data: JSONObject?) {
    val size = LocalSize.current
    val qqq = data?.optJSONObject("QQQ")
    val sig = qqq?.optJSONObject("signal")
    val price = qqq?.optDouble("price", 0.0) ?: 0.0
    val sma = sig?.optDouble("sma", 0.0) ?: 0.0
    val dd = sig?.optDouble("drawdown", 0.0) ?: 0.0
    val compact = size.width < 220.dp

    WidgetShell {
        if (data == null || qqq == null) {
            ErrorView()
        } else if (compact) {
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Text("QQQ", style = TextStyle(color = gray, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                Text(formatPrice(price), style = TextStyle(color = cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold))
                Text("200SMA ${formatPrice(sma)}", style = TextStyle(color = gray, fontSize = 9.sp))
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text("아기티큐 200큐큐단", style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(4.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Column {
                        Text("QQQ", style = TextStyle(color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        Text(formatPrice(price), style = TextStyle(color = white, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.Horizontal.End) {
                        Text("200SMA", style = TextStyle(color = gray, fontSize = 9.sp))
                        Text(formatPrice(sma), style = TextStyle(color = gray, fontSize = 11.sp))
                    }
                }
                Spacer(GlanceModifier.height(4.dp))
                Text(sig?.optString("name", "-") ?: "-", style = TextStyle(color = if (sig?.optBoolean("alert") == true) red else gray, fontSize = 10.sp))
                Text(signalSummary(sig), style = TextStyle(color = white, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Text("최고점 대비 ${formatPercent(dd)}", style = TextStyle(color = gray, fontSize = 9.sp))
            }
        }
    }
}

@Composable
private fun FgiWidgetContent(data: JSONObject?) {
    val size = LocalSize.current
    val fgi = data?.optJSONObject("FGI")
    val value = fgi?.optDouble("value", 0.0) ?: 0.0
    val avg30 = fgi?.optDouble("avg30", 0.0) ?: 0.0
    val compact = size.width < 220.dp

    WidgetShell {
        if (data == null || fgi == null) {
            ErrorView()
        } else {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text("공포·탐욕 지수", style = TextStyle(color = white, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(3.dp))
                Text(value.toInt().toString(), style = TextStyle(color = fgiColor(value), fontSize = if (compact) 30.sp else 34.sp, fontWeight = FontWeight.Bold))
                Text(fgiRatingKo(fgi.optString("rating")), style = TextStyle(color = fgiColor(value), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                if (!compact) {
                    Spacer(GlanceModifier.height(5.dp))
                    Text(fgiBar(value), style = TextStyle(color = fgiColor(value), fontSize = 9.sp))
                    Text("30일 평균 ${avg30.toInt()}", style = TextStyle(color = gray, fontSize = 9.sp))
                }
            }
        }
    }
}

@Composable
private fun WidgetShell(content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(black)
            .clickable(actionStartActivity<MainActivity>())
            .padding(10.dp),
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
    for (i in 0 until lines.length()) {
        val row = lines.optJSONArray(i) ?: continue
        parts += "${row.optString(0)} ${row.optString(1)}"
    }
    return parts.joinToString(" · ").ifBlank { "-" }
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
