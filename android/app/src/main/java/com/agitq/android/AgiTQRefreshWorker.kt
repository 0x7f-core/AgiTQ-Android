package com.agitq.android

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AgiTQRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // 자동 갱신은 미국 정규장(뉴욕시간 09:30~16:00, 주말/정기 휴장일 제외)에서만 실행한다.
        // 수동 새로고침 버튼은 이 Worker를 거치지 않으므로 장외에도 그대로 사용할 수 있다.
        if (!UsMarketHours.isRegularSessionNow()) {
            return Result.success()
        }

        return runCatching {
            SpxWidget().updateAll(applicationContext)
            QqqWidget().updateAll(applicationContext)
            FgiWidget().updateAll(applicationContext)
        }.fold({ Result.success() }, { Result.retry() })
    }
}
