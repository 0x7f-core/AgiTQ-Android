package com.agitq.android

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * SPX/QQQ/FGI 위젯이 함께 사용하는 단일 시장 데이터 스냅샷.
 *
 * 정상 응답만 저장하므로 일시적인 서버/네트워크 오류가 기존 위젯 내용을
 * 오류 화면으로 덮어쓰지 않는다. 최초 설치처럼 저장된 값이 없을 때만
 * provideGlance 경로에서 한 번 가져온다.
 */
object MarketSnapshotRepository {
    private const val PREFS_NAME = "agitq_market_snapshot"
    private const val SNAPSHOT_JSON = "snapshot_json"
    private const val FETCHED_AT = "fetched_at"

    private val refreshMutex = Mutex()

    data class RefreshResult(
        val data: JSONObject?,
        val isFresh: Boolean,
        val fetchedAt: Long,
        val error: Throwable? = null
    )

    suspend fun cachedOrRefresh(context: Context): JSONObject? = withContext(Dispatchers.IO) {
        readCached(context) ?: refreshMutex.withLock {
            readCached(context) ?: fetchAndPersist(context, forceRefresh = false).data
        }
    }

    suspend fun refresh(context: Context): RefreshResult = withContext(Dispatchers.IO) {
        // 수동 새로고침과 정규장 30분 갱신은 Worker의 5분 캐시를 우회한다.
        refreshMutex.withLock { fetchAndPersist(context, forceRefresh = true) }
    }

    fun cached(context: Context): JSONObject? = readCached(context)

    private fun fetchAndPersist(context: Context, forceRefresh: Boolean): RefreshResult {
        return try {
            val fresh = AgiTQApi.load(forceRefresh)
            requireValidSnapshot(fresh)
            val fetchedAt = System.currentTimeMillis()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(SNAPSHOT_JSON, fresh.toString())
                .putLong(FETCHED_AT, fetchedAt)
                .apply()
            RefreshResult(JSONObject(fresh.toString()), true, fetchedAt)
        } catch (error: Exception) {
            val cached = readCached(context)
            val fetchedAt = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(FETCHED_AT, 0L)
            RefreshResult(cached, false, fetchedAt, error)
        }
    }

    private fun readCached(context: Context): JSONObject? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SNAPSHOT_JSON, null) ?: return null
        return runCatching {
            JSONObject(raw).also(::requireValidSnapshot)
        }.getOrNull()
    }

    internal fun requireValidSnapshot(root: JSONObject) {
        requireValidMarket(root, "SPX")
        requireValidMarket(root, "QQQ")
        requireValidMarket(root, "TQQQ", requireSignal = false)

        // CNN FGI는 원본 API 특성상 독립적으로 실패할 수 있다. 사용 가능하다고
        // 표시된 경우에만 값과 이력을 검증하고, unavailable 응답은 정상 스냅샷으로 둔다.
        root.optJSONObject("FGI")?.let { fgi ->
            if (fgi.optBoolean("available", true)) {
                require(fgi.hasFiniteNumber("value")) { "FGI value missing" }
                require(fgi.optJSONArray("history") != null) { "FGI history missing" }
            }
        }
    }

    private fun requireValidMarket(root: JSONObject, key: String, requireSignal: Boolean = true) {
        val market = root.optJSONObject(key) ?: error("$key missing")
        require(market.hasFiniteNumber("price")) { "$key price invalid" }
        require(market.optLong("mTime", 0L) > 0L) { "$key market time invalid" }
        require(market.optJSONArray("closes").hasFiniteHistory(200)) { "$key history invalid" }
        require(market.optJSONArray("timestamps").hasFiniteHistory(200)) { "$key timestamps invalid" }
        if (requireSignal) {
            val signal = market.optJSONObject("signal") ?: error("$key signal missing")
            require(signal.optJSONArray("lines") != null) { "$key signal lines missing" }
            require(signal.optString("name").isNotBlank()) { "$key signal name missing" }
        }
    }

    private fun JSONObject.hasFiniteNumber(key: String): Boolean {
        val value = opt(key) as? Number ?: return false
        return value.toDouble().isFinite()
    }

    private fun JSONArray?.hasFiniteHistory(minSize: Int): Boolean {
        if (this == null || length() < minSize) return false
        for (index in 0 until length()) {
            val value = opt(index) as? Number ?: return false
            if (!value.toDouble().isFinite()) return false
        }
        return true
    }
}
