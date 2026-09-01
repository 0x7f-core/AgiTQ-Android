package com.agitq.android

import android.content.Context
import androidx.glance.appwidget.updateAll

/** 한 번의 API 응답으로 설치된 세 위젯을 같은 시점의 데이터로 갱신한다. */
object WidgetRefreshCoordinator {
    suspend fun refreshAll(context: Context): MarketSnapshotRepository.RefreshResult {
        val result = MarketSnapshotRepository.refresh(context)

        // 정상 데이터나 마지막 정상 데이터가 있을 때만 다시 렌더링한다.
        // 데이터가 전혀 없는 실패는 현재 표시를 그대로 보존한다.
        if (result.data != null) {
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
