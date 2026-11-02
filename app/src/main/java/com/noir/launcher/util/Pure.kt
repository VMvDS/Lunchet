package com.noir.launcher.util

/** Alphabet-rail section for an app label. Non-letters collapse into #. */
fun sectionLetter(label: String): Char {
    val c = label.trim().firstOrNull() ?: return '#'
    return if (c.isLetter()) c.uppercaseChar() else '#'
}

object FavoritesCodec {
    private const val SEP = "|"
    fun encode(keys: List<String>): String = keys.joinToString(SEP)
    fun decode(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split(SEP).filter { it.isNotBlank() }
}

object WeatherCodes {
    fun describe(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }
}

/** Parses "lat, lon". Returns null when malformed or out of range. */
fun parseCoords(input: String): Pair<Double, Double>? {
    val parts = input.split(",", ";", " ").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size != 2) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lon = parts[1].toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return lat to lon
}

object IntListCodec {
    fun encode(v: List<Int>): String = v.joinToString(",")
    fun decode(raw: String): List<Int> = raw.split(",").mapNotNull { it.trim().toIntOrNull() }
}
