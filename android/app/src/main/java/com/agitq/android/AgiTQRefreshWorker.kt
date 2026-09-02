package com.agitq.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AgiTQRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // 정규장에는 30분 주기로 실행한다. 16:00~17:00에는 WorkManager 주기 오차로
        // 종가를 놓치지 않도록 해당 뉴욕 거래일에 한 번만 최종 동기화한다.
        // 수동 새로고침 버튼은 이 판정을 거치지 않으므로 언제든 사용할 수 있다.
        val decision = UsMarketHours.automaticRefreshDecision(
            FinalCloseSyncStore.completedDate(applicationContext)
        ) ?: return Result.success()

        return runCatching {
            WidgetRefreshCoordinator.refreshAll(applicationContext)
        }.fold(
            onSuccess = { refresh ->
                if (!refresh.isFresh) {
                    Result.retry()
                } else {
                    if (decision.reason == UsMarketHours.AutomaticRefreshReason.FINAL_CLOSE_SYNC) {
                        FinalCloseSyncStore.markCompleted(applicationContext, decision.tradingDate)
                    }
                    Result.success()
                }
            },
            onFailure = { Result.retry() }
        )
    }
}
