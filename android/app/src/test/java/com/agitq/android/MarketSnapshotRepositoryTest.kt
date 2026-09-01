package com.agitq.android

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test

class MarketSnapshotRepositoryTest {
    @Test
    fun acceptsCompleteSnapshotWithUnavailableFgi() {
        MarketSnapshotRepository.requireValidSnapshot(validSnapshot())
    }

    @Test
    fun rejectsTruncatedHistoryBeforeItCanReplaceCache() {
        val snapshot = validSnapshot()
        snapshot.getJSONObject("SPX").put("closes", JSONArray(listOf(1.0, 2.0)))

        assertThrows(IllegalArgumentException::class.java) {
            MarketSnapshotRepository.requireValidSnapshot(snapshot)
        }
    }

    @Test
    fun rejectsMarketWithoutSignal() {
        val snapshot = validSnapshot()
        snapshot.getJSONObject("QQQ").remove("signal")

        assertThrows(IllegalStateException::class.java) {
            MarketSnapshotRepository.requireValidSnapshot(snapshot)
        }
    }

    private fun validSnapshot(): JSONObject {
        val timestamps = JSONArray((1..200).map { 1_700_000_000L + it * 86_400L })
        val closes = JSONArray((1..200).map { 100.0 + it })
        val signal = JSONObject()
            .put("name", "상단 밴드 위 4일 차 (추세 유지)")
            .put("lines", JSONArray().put(JSONArray().put("TQQQ").put("보유 유지")))

        fun market(includeSignal: Boolean) = JSONObject()
            .put("price", 300.0)
            .put("mTime", 1_800_000_000L)
            .put("closes", JSONArray(closes.toString()))
            .put("timestamps", JSONArray(timestamps.toString()))
            .also { if (includeSignal) it.put("signal", JSONObject(signal.toString())) }

        return JSONObject()
            .put("SPX", market(true))
            .put("QQQ", market(true))
            .put("TQQQ", market(false))
            .put("FGI", JSONObject().put("available", false))
    }
}
