package com.agitq.android

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.Alignment
import androidx.glance.background
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
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
import androidx.glance.unit.dp
import androidx.glance.unit.sp
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

@Composable
private fun WidgetContent(data: JSONObject?) {
    val black = ColorProvider(Color.Black)
    val white = ColorProvider(Color.White)
    val gray = ColorProvider(Color(0xFFAAAAAA))
    val red = ColorProvider(Color(0xFFFF4D4D))
    val size = LocalSize.current
    val wide = size.width >= 250.dp
    val large = size.height >= 180.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(black)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (data == null) {
            Text(
                text = "AgiTQ\n데이터 로드 실패",
                style = TextStyle(color = red, fontWeight = FontWeight.Bold)
            )
        } else {
            val spx = data.optJSONObject("SPX") ?: JSONObject()
            val qqq = data.optJSONObject("QQQ") ?: JSONObject()
            val fgi = data.optJSONObject("FGI") ?: JSONObject()
            val spxSig = spx.optJSONObject("signal") ?: JSONObject()
            val qqqSig = qqq.optJSONObject("signal") ?: JSONObject()

            if (!wide) {
                Text(
                    text = "공포·탐욕 지수",
                    style = TextStyle(color = white, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = fgi.optInt("value", 0).toString(),
                    style = TextStyle(
                        color = white,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = fgi.optString("rating", "-"),
                    style = TextStyle(color = gray)
                )
            } else if (!large) {
                Text(
                    text = "아기티큐 200큐큐단 (QQQ)",
                    style = TextStyle(color = white, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(5.dp))
                Text(
                    text = "QQQ ${"%.2f".format(qqq.optDouble("price", 0.0))}",
                    style = TextStyle(color = white, fontWeight = FontWeight.Bold)
                )
                SignalLines(qqqSig, gray, red)
            } else {
                Text(
                    text = "아기티큐 200슨피단 (SPX)",
                    style = TextStyle(color = white, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "SPX ${"%.2f".format(spx.optDouble("price", 0.0))}",
                    style = TextStyle(color = white)
                )
                SignalLines(spxSig, gray, red)
                Spacer(GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Column {
                        Text("CNN FGI", style = TextStyle(color = gray))
                        Text(
                            fgi.optInt("value", 0).toString(),
                            style = TextStyle(color = white, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(GlanceModifier.height(1.dp))
                    Column {
                        Text(fgi.optString("rating", "-"), style = TextStyle(color = gray))
                        Text(
                            "30일 ${"%.0f".format(fgi.optDouble("avg30", 0.0))}",
                            style = TextStyle(color = white)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalLines(sig: JSONObject, gray: ColorProvider, red: ColorProvider) {
    Spacer(GlanceModifier.height(5.dp))
    val alert = sig.optBoolean("alert", false)
    Text(
        text = sig.optString("name", "-"),
        style = TextStyle(
            color = if (alert) red else gray,
            fontWeight = if (alert) FontWeight.Bold else FontWeight.Normal
        )
    )

    val lines = sig.optJSONArray("lines")
    if (lines != null) {
        for (i in 0 until lines.length()) {
            val x = lines.optJSONArray(i)
            if (x != null) {
                Text(
                    text = "${x.optString(0)}  ${x.optString(1)}",
                    style = TextStyle(color = gray)
                )
            }
        }
    }

    if (sig.has("drawdown") && !sig.isNull("drawdown")) {
        Text(
            text = "TQQQ 최고점 대비 ${"%.1f".format(sig.optDouble("drawdown"))}%",
            style = TextStyle(color = gray)
        )
    }
}
