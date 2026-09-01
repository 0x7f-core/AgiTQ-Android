package com.agitq.android

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AgiTQRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        AgiTQApi.load()
        SpxWidget().updateAll(applicationContext)
        QqqWidget().updateAll(applicationContext)
        FgiWidget().updateAll(applicationContext)
    }.fold({ Result.success() }, { Result.retry() })
}
