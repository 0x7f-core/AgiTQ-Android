package com.agitq.android

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit

class AgiTQApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<AgiTQRefreshWorker>(AgiTQConfig.REFRESH_MINUTES, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("agitq-refresh", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
