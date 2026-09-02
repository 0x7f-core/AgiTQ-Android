package com.agitq.android

import android.content.Context
import java.time.LocalDate

/** 뉴욕 거래일별 장 마감 후 최종 동기화 완료 여부를 보존한다. */
internal object FinalCloseSyncStore {
    private const val PREFS_NAME = "agitq_refresh_state"
    private const val LAST_FINAL_CLOSE_SYNC_DATE = "last_final_close_sync_ny_date"

    fun completedDate(context: Context): LocalDate? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LAST_FINAL_CLOSE_SYNC_DATE, null)
            ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    fun markCompleted(context: Context, tradingDate: LocalDate) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LAST_FINAL_CLOSE_SYNC_DATE, tradingDate.toString())
            .apply()
    }
}
