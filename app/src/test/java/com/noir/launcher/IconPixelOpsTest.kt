package com.noir.launcher

import com.noir.launcher.ui.icons.IconPixelOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPixelOpsTest {
    private val w = 24
    private val h = 24

    private fun solidSquare(): IntArray = IntArray(w * h) { i ->
        val x = i % w
        val y = i / w
        if (x in 6..17 && y in 6..17) 0xFF336699.toInt() else 0
    }

    private fun alphaAt(a: IntArray, x: Int, y: Int) = a[y * w + x] ushr 24

    @Test fun edge_of_transparent_image_is_empty() {
        val out = IconPixelOps.edge(IntArray(w * h), w, h)
        assertTrue(out.all { (it ushr 24) == 0 })
    }

    @Test fun edge_draws_outline_and_keeps_interior_clear() {
        val out = IconPixelOps.edge(solidSquare(), w, h)
        assertEquals("interior must be empty (line-art, not fill)", 0, alphaAt(out, 12, 12))
        assertEquals("far background must be empty", 0, alphaAt(out, 1, 1))
        val border = maxOf(alphaAt(out, 6, 12), alphaAt(out, 5, 12))
        assertTrue("outline must be visible, was $border", border > 100)
    }

    @Test fun edge_output_is_always_soft_white() {
        val out = IconPixelOps.edge(solidSquare(), w, h)
        assertTrue(out.all { (it and 0xFFFFFF) == IconPixelOps.INK })
    }

    @Test fun glyph_preserves_alpha_and_recolours() {
        val out = IconPixelOps.glyph(intArrayOf(0x80FF0000.toInt(), 0))
        assertEquals(0x80, out[0] ushr 24)
        assertEquals(IconPixelOps.INK, out[0] and 0xFFFFFF)
        assertEquals(0, out[1] ushr 24)
    }
}
