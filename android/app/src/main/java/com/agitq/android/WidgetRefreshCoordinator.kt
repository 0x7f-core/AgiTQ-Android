package com.agitq.android

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager

/** 한 번의 API 응답으로 설치된 세 위젯을 같은 시점의 데이터로 갱신한다. */
object WidgetRefreshCoordinator {
    suspend fun refreshAll(context: Context): MarketSnapshotRepository.RefreshResult {
        val result = MarketSnapshotRepository.refresh(context)

        // 새 스냅샷일 때만 다시 그린다. 실패 시에는 이미 표시 중인 마지막 정상
        // 비트맵을 그대로 두어 동일 데이터의 불필요한 3회 렌더링을 피한다.
        if (result.isFresh) {
            updateAll(context)
        }
        return result
    }

    suspend fun updateAll(context: Context) {
        var firstError: Throwable? = null
        runCatching { updateProvider(context, SpxWidget(), SpxWidgetReceiver::class.java) }
            .onFailure { firstError = it }
        runCatching { updateProvider(context, QqqWidget(), QqqWidgetReceiver::class.java) }
            .onFailure { if (firstError == null) firstError = it }
        runCatching { updateProvider(context, FgiWidget(), FgiWidgetReceiver::class.java) }
            .onFailure { if (firstError == null) firstError = it }
        firstError?.let { throw it }
    }

    /**
     * Resolve IDs from the stable Android receiver component instead of the
     * Glance implementation class. This keeps each installed widget bound to
     * its original type even across R8-optimized in-place app upgrades.
     */
    private suspend fun updateProvider(
        context: Context,
        widget: GlanceAppWidget,
        receiverClass: Class<*>
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val glanceManager = GlanceAppWidgetManager(context)
        val component = ComponentName(context, receiverClass)
        manager.getAppWidgetIds(component).forEach { appWidgetId ->
            widget.update(context, glanceManager.getGlanceIdBy(appWidgetId))
        }
    }
}
