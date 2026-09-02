# Glance groups installed widgets by the concrete GlanceAppWidget class name.
# R8 horizontal class merging must not collapse the three independent widget
# implementations into one runtime class, otherwise updateAll can cross-update
# SPX, QQQ and FGI instances.
-keep class com.agitq.android.SpxWidget { *; }
-keep class com.agitq.android.QqqWidget { *; }
-keep class com.agitq.android.FgiWidget { *; }

# AppWidget provider component names are persistent identities held by the
# launcher across in-place APK upgrades.
-keep class com.agitq.android.SpxWidgetReceiver { *; }
-keep class com.agitq.android.QqqWidgetReceiver { *; }
-keep class com.agitq.android.FgiWidgetReceiver { *; }
