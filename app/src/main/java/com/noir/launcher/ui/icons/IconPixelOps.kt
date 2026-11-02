package com.noir.launcher.ui.icons

import kotlin.math.hypot
import kotlin.math.max

/** Pure pixel math (no Android types) so it is unit-testable on the JVM. */
object IconPixelOps {
    const val INK: Int = 0xF4F4F2

    /** Solid silhouette: keeps alpha, paints everything soft white. */
    fun glyph(src: IntArray): IntArray =
        IntArray(src.size) { ((src[it] ushr 24) shl 24) or INK }

    /**
     * Thin-stroke line art: Sobel edges of (a) the alpha silhouette and (b) the
     * alpha-weighted luminance, rendered as soft-white strokes on transparency.
     */
    fun edge(src: IntArray, w: Int, h: Int, gain: Float = 1.8f, floor: Float = 16f): IntArray {
        val n = w * h
        val a = FloatArray(n)
        val l = FloatArray(n)
        for (i in 0 until n) {
            val c = src[i]
            val al = (c ushr 24) / 255f
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            a[i] = al * 255f
            l[i] = al * lum * 255f
        }
        fun at(arr: FloatArray, x: Int, y: Int): Float =
            arr[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)]

        fun sobel(arr: FloatArray, x: Int, y: Int): Float {
            val gx = -at(arr, x - 1, y - 1) - 2 * at(arr, x - 1, y) - at(arr, x - 1, y + 1) +
                at(arr, x + 1, y - 1) + 2 * at(arr, x + 1, y) + at(arr, x + 1, y + 1)
            val gy = -at(arr, x - 1, y - 1) - 2 * at(arr, x, y - 1) - at(arr, x + 1, y - 1) +
                at(arr, x - 1, y + 1) + 2 * at(arr, x, y + 1) + at(arr, x + 1, y + 1)
            return hypot(gx, gy) / 4f
        }

        val out = IntArray(n)
        for (y in 0 until h) for (x in 0 until w) {
            val mag = max(sobel(a, x, y), 0.8f * sobel(l, x, y))
            val alpha = ((mag - floor) * gain).coerceIn(0f, 255f).toInt()
            out[y * w + x] = (alpha shl 24) or INK
        }
        return out
    }
}
