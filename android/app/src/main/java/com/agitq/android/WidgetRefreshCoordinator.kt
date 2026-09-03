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

        // 새 스냅샷 또는 최초 설치의 서버 fallback이 있을 때만 다시 그린다.
        // 기존 화면이 있는 실패 경로에서는 마지막 정상 비트맵을 그대로 둔다.
        if (result.shouldRender) {
            updateAll(context)
        }
        return result
    }

    suspend fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val glanceManager = GlanceAppWidgetManager(context)
        var firstError: Throwable? = null
        runCatching {
            updateProvider(context, appWidgetManager, glanceManager, SpxWidget(), SpxWidgetReceiver::class.java)
        }
            .onFailure { firstError = it }
        runCatching {
            updateProvider(context, appWidgetManager, glanceManager, QqqWidget(), QqqWidgetReceiver::class.java)
        }
            .onFailure { if (firstError == null) firstError = it }
        runCatching {
            updateProvider(context, appWidgetManager, glanceManager, FgiWidget(), FgiWidgetReceiver::class.java)
        }
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
        manager: AppWidgetManager,
        glanceManager: GlanceAppWidgetManager,
        widget: GlanceAppWidget,
        receiverClass: Class<*>
    ) {
        val component = ComponentName(context, receiverClass)
        manager.getAppWidgetIds(component).forEach { appWidgetId ->
            widget.update(context, glanceManager.getGlanceIdBy(appWidgetId))
        }
    }
}
