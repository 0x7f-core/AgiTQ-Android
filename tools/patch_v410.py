from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"marker not found: {label}")
    return text.replace(old, new, 1)


widget = Path("android/app/src/main/java/com/agitq/android/AgiTQWidget.kt")
s = widget.read_text(encoding="utf-8")

if "class RefreshWidgetAction : ActionCallback" not in s:
    s = replace_once(
        s,
        "import android.net.Uri\nimport androidx.compose.runtime.Composable\n",
        "import android.net.Uri\nimport androidx.compose.runtime.Composable\nimport androidx.compose.ui.unit.dp\n",
        "compose dp import",
    )
    s = replace_once(
        s,
        "import androidx.glance.LocalSize\nimport androidx.glance.action.clickable\n",
        "import androidx.glance.LocalSize\nimport androidx.glance.action.ActionParameters\nimport androidx.glance.action.actionParametersOf\nimport androidx.glance.action.clickable\n",
        "action parameters imports",
    )
    s = replace_once(
        s,
        "import androidx.glance.appwidget.SizeMode\nimport androidx.glance.appwidget.action.actionStartActivity\n",
        "import androidx.glance.appwidget.SizeMode\nimport androidx.glance.appwidget.action.ActionCallback\nimport androidx.glance.appwidget.action.actionRunCallback\nimport androidx.glance.appwidget.action.actionStartActivity\n",
        "callback imports",
    )
    s = replace_once(
        s,
        "import androidx.glance.layout.ContentScale\nimport androidx.glance.layout.fillMaxSize\n",
        "import androidx.glance.layout.Alignment\nimport androidx.glance.layout.Box\nimport androidx.glance.layout.ContentScale\nimport androidx.glance.layout.fillMaxSize\nimport androidx.glance.layout.padding\nimport androidx.glance.layout.size\n",
        "layout imports",
    )
    s = replace_once(
        s,
        "private enum class CardKind { SPX, QQQ, FGI }\nprivate enum class LayoutMode { WIDE, STACKED, TALL }\n",
        "private enum class CardKind { SPX, QQQ, FGI }\nprivate enum class LayoutMode { WIDE, STACKED, TALL }\nprivate val RefreshKindKey = ActionParameters.Key<String>(\"refresh_kind\")\n",
        "refresh key",
    )

    old_load = """private suspend fun loadData(): JSONObject? = withContext(Dispatchers.IO) {
    runCatching { AgiTQApi.load() }.getOrNull()
}
"""
    new_load = """private suspend fun loadData(): JSONObject? = withContext(Dispatchers.IO) {
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
"""
    s = replace_once(s, old_load, new_load, "loadData")

    old_image = """    Image(
        provider = ImageProvider(bitmap),
        contentDescription = description,
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(dashboardIntent(context))),
        contentScale = ContentScale.FillBounds
    )
"""
    new_image = """    Box(
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
            contentDescription = \"새로고침\",
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
"""
    s = replace_once(s, old_image, new_image, "responsive card image")

s = s.replace(
    ".ofPattern(\"yy.MM.dd. HH:mm '한국시간'\", Locale.KOREA)",
    ".ofPattern(\"yy.MM.dd. HH:mm '기준'\", Locale.KOREA)",
)
s = s.replace(
    "/** 모든 Android 위젯의 기준시각은 한국 표준시(KST)로 표시. */",
    "/** 기준시각은 한국 표준시(KST)로 계산하고 화면에는 원본처럼 `기준`만 표시. */",
)
widget.write_text(s, encoding="utf-8")


drawable = Path("android/app/src/main/res/drawable/ic_widget_refresh.xml")
drawable.parent.mkdir(parents=True, exist_ok=True)
if not drawable.exists():
    drawable.write_text(
        """<?xml version=\"1.0\" encoding=\"utf-8\"?>
<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"
    android:width=\"24dp\"
    android:height=\"24dp\"
    android:viewportWidth=\"24\"
    android:viewportHeight=\"24\">
    <path
        android:fillColor=\"#CCFFFFFF\"
        android:pathData=\"M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.09,0 -7.19,3.72 -6.39,7.69L3,9v7h7l-2.76,-2.76C6.37,10.41 8.36,8 12,8c2.21,0 4,1.79 4,4s-1.79,4 -4,4c-1.82,0 -3.33,-1.22 -3.82,-2.88l-3.05,0.8C5.99,17.87 8.65,20 12,20c4.42,0 8,-3.58 8,-8 0,-2.21 -0.9,-4.21 -2.35,-5.65z\" />
</vector>
""",
        encoding="utf-8",
    )


appjs = Path("web/js/app.js")
j = appjs.read_text(encoding="utf-8")
j = j.replace(
    "return `${obj.year}.${obj.month}.${obj.day}. ${obj.hour}:${obj.minute} 한국시간`;",
    "return `${obj.year}.${obj.month}.${obj.day}. ${obj.hour}:${obj.minute} 기준`;",
)
if "const manualRefreshButton = document.getElementById('manual-refresh');" not in j:
    j += """

const manualRefreshButton = document.getElementById('manual-refresh');
if (manualRefreshButton) {
  manualRefreshButton.addEventListener('click', async () => {
    if (manualRefreshButton.disabled) return;
    manualRefreshButton.disabled = true;
    manualRefreshButton.classList.add('refreshing');
    manualRefreshButton.textContent = '↻ 새로고침 중...';
    try {
      await renderDashboard();
    } finally {
      manualRefreshButton.disabled = false;
      manualRefreshButton.classList.remove('refreshing');
      manualRefreshButton.textContent = '↻ 새로고침';
    }
  });
}
"""
appjs.write_text(j, encoding="utf-8")


index = Path("web/index.html")
h = index.read_text(encoding="utf-8")
if 'id="manual-refresh"' not in h:
    marker = '    <div class="error-text" id="error-text" aria-live="polite"></div>\n'
    replacement = marker + """    <div class="manual-refresh-wrap">
      <button class="manual-refresh" id="manual-refresh" type="button" aria-label="시장 데이터 새로고침">↻ 새로고침</button>
    </div>
"""
    h = replace_once(h, marker, replacement, "web refresh button")
index.write_text(h, encoding="utf-8")


css = Path("web/style.css")
c = css.read_text(encoding="utf-8")
if ".manual-refresh-wrap" not in c:
    c += """

.manual-refresh-wrap{
  width:100%;
  display:flex;
  justify-content:center;
  padding:2px 0 4px;
}
.manual-refresh{
  min-width:150px;
  min-height:48px;
  padding:0 22px;
  border:1px solid var(--divider);
  border-radius:16px;
  background:#0b0b0c;
  color:var(--p1);
  font:700 15px/1 -apple-system,BlinkMacSystemFont,"Apple SD Gothic Neo","SF Pro Display",Roboto,sans-serif;
  cursor:pointer;
}
.manual-refresh:active{transform:scale(.98)}
.manual-refresh:disabled{opacity:.62;cursor:default}
.manual-refresh.refreshing{border-color:#555}
@media(max-width:680px){
  .manual-refresh{min-width:132px;min-height:44px;border-radius:14px;font-size:14px}
}
"""
css.write_text(c, encoding="utf-8")


gradle = Path("android/app/build.gradle.kts")
g = gradle.read_text(encoding="utf-8")
g = g.replace("versionCode = 10", "versionCode = 11")
g = g.replace('versionName = "4.9"', 'versionName = "4.10"')
gradle.write_text(g, encoding="utf-8")
