package com.akeshari.splitblind.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ExchangeRateService {
    private val cache = mutableMapOf<String, Double>()

    suspend fun getRate(from: String, to: String): Double? {
        if (from == to) return 1.0
        val key = "${from}_${to}"
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.frankfurter.app/latest?from=$from&to=$to")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val json = conn.inputStream.bufferedReader().readText()
                val rate = JSONObject(json).getJSONObject("rates").getDouble(to)
                cache[key] = rate
                rate
            } catch (e: Exception) {
                null
            }
        }
    }
}
