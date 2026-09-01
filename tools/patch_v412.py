from pathlib import Path

widget = Path("android/app/src/main/java/com/agitq/android/AgiTQWidget.kt")
s = widget.read_text(encoding="utf-8")

if "val refreshButtonSize = when {" not in s:
    old = '''    Box(
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
'''
    new = '''    val minSideDp = min(glanceSize.width.value, glanceSize.height.value)
    val refreshButtonSize = when {
        minSideDp < 110f -> 44.dp
        minSideDp < 160f -> 50.dp
        else -> 54.dp
    }

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
                .size(refreshButtonSize)
                .padding(2.dp)
                .clickable(
                    actionRunCallback<RefreshWidgetAction>(
                        actionParametersOf(RefreshKindKey to kind.name)
                    )
                )
        )
    }
'''
    if old not in s:
        raise SystemExit("refresh button marker not found")
    s = s.replace(old, new, 1)
    widget.write_text(s, encoding="utf-8")

drawable = Path("android/app/src/main/res/drawable/ic_widget_refresh.xml")
drawable.write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#F21D1D1F"
        android:strokeColor="#70FFFFFF"
        android:strokeWidth="1"
        android:pathData="M12,0.9a11.1,11.1 0,1 0,0 22.2a11.1,11.1 0,1 0,0 -22.2" />
    <path
        android:fillColor="#00000000"
        android:strokeColor="#1FFFFFFF"
        android:strokeWidth="0.7"
        android:pathData="M12,2.7a9.3,9.3 0,1 0,0 18.6a9.3,9.3 0,1 0,0 -18.6" />
    <path
        android:fillColor="#D8FFFFFF"
        android:pathData="M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.09,0 -7.19,3.72 -6.39,7.69L3,9v7h7l-2.76,-2.76C6.37,10.41 8.36,8 12,8c2.21,0 4,1.79 4,4s-1.79,4 -4,4c-1.82,0 -3.33,-1.22 -3.82,-2.88l-3.05,0.8C5.99,17.87 8.65,20 12,20c4.42,0 8,-3.58 8,-8 0,-2.21 -0.9,-4.21 -2.35,-5.65z" />
</vector>
''', encoding="utf-8")

gradle = Path("android/app/build.gradle.kts")
g = gradle.read_text(encoding="utf-8")
g = g.replace("versionCode = 12", "versionCode = 13")
g = g.replace('versionName = "4.11"', 'versionName = "4.12"')
gradle.write_text(g, encoding="utf-8")
