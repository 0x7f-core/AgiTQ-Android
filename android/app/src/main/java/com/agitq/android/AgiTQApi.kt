package com.agitq.android

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AgiTQApi {
    fun load(): JSONObject {
        val c = URL(AgiTQConfig.API_BASE + AgiTQConfig.API_PATH).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 20000
        c.setRequestProperty("Accept", "application/json")
        return try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally { c.disconnect() }
    }
}
