package com.agitq.android

import android.content.Context
import androidx.glance.appwidget.updateAll

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
        runCatching { SpxWidget().updateAll(context) }.onFailure { firstError = it }
        runCatching { QqqWidget().updateAll(context) }.onFailure { if (firstError == null) firstError = it }
        runCatching { FgiWidget().updateAll(context) }.onFailure { if (firstError == null) firstError = it }
        firstError?.let { throw it }
    }
}
