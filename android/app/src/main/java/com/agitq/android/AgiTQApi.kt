package com.agitq.android

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AgiTQApi {
    fun load(forceRefresh: Boolean = false): JSONObject {
        val suffix = if (forceRefresh) "?refresh=1" else ""
        val c = URL(AgiTQConfig.API_BASE + AgiTQConfig.API_PATH + suffix).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 20000
        c.useCaches = false
        c.setRequestProperty("Accept", "application/json")
        if (forceRefresh) c.setRequestProperty("Cache-Control", "no-cache")
        return try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally { c.disconnect() }
    }
}
