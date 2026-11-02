package com.noir.launcher

import com.noir.launcher.util.FavoritesCodec
import com.noir.launcher.util.WeatherCodes
import com.noir.launcher.util.parseCoords
import com.noir.launcher.util.sectionLetter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PureLogicTest {
    @Test fun sectionLetters() {
        assertEquals('C', sectionLetter("camera"))
        assertEquals('#', sectionLetter("1Password"))
        assertEquals('#', sectionLetter(""))
        assertEquals('Y', sectionLetter("  YouTube"))
    }

    @Test fun favoritesRoundTrip() {
        val keys = listOf("a.b/.Main", "c.d/.Other")
        assertEquals(keys, FavoritesCodec.decode(FavoritesCodec.encode(keys)))
        assertEquals(emptyList<String>(), FavoritesCodec.decode(FavoritesCodec.encode(emptyList())))
    }

    @Test fun coordinateParsing() {
        assertEquals(26.4 to 50.1, parseCoords("26.4, 50.1"))
        assertEquals(-33.9 to 151.2, parseCoords("-33.9 151.2"))
        assertNull(parseCoords("abc"))
        assertNull(parseCoords("91, 0"))
        assertNull(parseCoords("10, 181"))
        assertNull(parseCoords("10"))
    }

    @Test fun weatherCodes() {
        assertEquals("Clear sky", WeatherCodes.describe(0))
        assertEquals("Overcast", WeatherCodes.describe(3))
        assertEquals("Light rain", WeatherCodes.describe(61))
        assertEquals("Unknown", WeatherCodes.describe(1234))
    }
}
