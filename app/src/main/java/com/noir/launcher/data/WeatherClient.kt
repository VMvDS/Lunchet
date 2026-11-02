package com.noir.launcher.data

import com.noir.launcher.model.Place
import com.noir.launcher.model.Weather
import com.noir.launcher.util.WeatherCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/** Opt-in, keyless weather + city search via Open-Meteo. Only called when the user enables it. */
object WeatherClient {
    private fun get(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            if (conn.responseCode != 200) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun fetch(lat: Double, lon: Double): Weather? = withContext(Dispatchers.IO) {
        val body = get(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&wind_speed_unit=kmh"
        ) ?: return@withContext null
        try {
            val cur = JSONObject(body).getJSONObject("current")
            Weather(
                tempC = cur.getDouble("temperature_2m").roundToInt(),
                humidity = cur.getInt("relative_humidity_2m"),
                windKmh = cur.getDouble("wind_speed_10m").roundToInt(),
                description = WeatherCodes.describe(cur.getInt("weather_code")),
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun search(query: String): List<Place> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList<Place>()
        val body = get(
            "https://geocoding-api.open-meteo.com/v1/search?name=" +
                URLEncoder.encode(q, "UTF-8") + "&count=6&language=en&format=json"
        ) ?: return@withContext emptyList<Place>()
        try {
            val arr = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList<Place>()
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Place(
                    name = o.optString("name"),
                    region = listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                    lat = o.getDouble("latitude"),
                    lon = o.getDouble("longitude"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
