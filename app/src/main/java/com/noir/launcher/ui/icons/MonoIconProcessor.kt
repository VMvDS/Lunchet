package com.noir.launcher.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.noir.launcher.model.IconStyle

/** Turns any app icon into the uniform monochrome family. Call off the main thread. */
object MonoIconProcessor {
    const val SIZE = 96

    fun render(drawable: Drawable, style: IconStyle): Bitmap {
        if (style == IconStyle.ORIGINAL) return drawFull(drawable)
        if (style == IconStyle.TILE) return wrapTile(renderMono(drawable, IconStyle.LINE))
        return renderMono(drawable, style)
    }

    /** Dark glass squircle with a hairline rim; the line-art icon sits inside it. */
    private fun wrapTile(inner: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val s = SIZE.toFloat()
        val rect = RectF(1.5f, 1.5f, s - 1.5f, s - 1.5f)
        val r = s * 0.30f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, s, 0xFF1D1D1F.toInt(), 0xFF060607.toInt(), Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, r, r, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.shader = LinearGradient(0f, 0f, s, s, 0x66FFFFFF, 0x14FFFFFF, Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, r, r, p)
        val inset = s * 0.17f
        c.drawBitmap(inner, null, RectF(inset, inset, s - inset, s - inset), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun renderMono(drawable: Drawable, style: IconStyle): Bitmap {
        val bmp = drawLayer(drawable, preferMono = style == IconStyle.GLYPH)
        val px = IntArray(SIZE * SIZE)
        bmp.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        val out = when (style) {
            IconStyle.GLYPH -> IconPixelOps.glyph(px)
            else -> IconPixelOps.edge(px, SIZE, SIZE)
        }
        bmp.setPixels(out, 0, SIZE, 0, 0, SIZE, SIZE)
        return bmp
    }

    private fun drawFull(d: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, SIZE, SIZE)
        d.draw(Canvas(bmp))
        return bmp
    }

    /** Draws only the foreground (or monochrome layer) so the background plate is dropped. */
    private fun drawLayer(d: Drawable, preferMono: Boolean): Bitmap {
        val adaptive = d as? AdaptiveIconDrawable
        val src: Drawable = if (adaptive != null) {
            val mono = if (preferMono && Build.VERSION.SDK_INT >= 33) adaptive.monochrome else null
            mono ?: adaptive.foreground ?: d
        } else d

        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (adaptive != null) {
            // Adaptive layers are 108dp with a 72dp safe zone: draw oversize and centre-crop.
            val full = SIZE * 3 / 2
            val off = (full - SIZE) / 2
            canvas.translate(-off.toFloat(), -off.toFloat())
            src.setBounds(0, 0, full, full)
        } else {
            val inset = SIZE / 8
            src.setBounds(inset, inset, SIZE - inset, SIZE - inset)
        }
        src.draw(canvas)
        return bmp
    }
}
