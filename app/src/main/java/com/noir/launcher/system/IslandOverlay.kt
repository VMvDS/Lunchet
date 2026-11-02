package com.noir.launcher.system

import android.animation.ValueAnimator
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.noir.launcher.model.IslandEvent
import com.noir.launcher.model.LauncherSettings
import com.noir.launcher.model.MediaState
import com.noir.launcher.model.NoirBus

/** Small hand-drawn glyph (no icon fonts needed). */
class GlyphView(ctx: Context, var kind: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private var phase = 0f
    private var anim: ValueAnimator? = null

    fun setTint(c: Int) { p.color = c; invalidate() }

    fun animateBars(on: Boolean) {
        anim?.cancel()
        anim = null
        if (!on) { phase = 0f; invalidate(); return }
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        path.reset()
        when (kind) {
            BOLT -> {
                path.moveTo(w * 0.58f, h * 0.06f)
                path.lineTo(w * 0.22f, h * 0.56f)
                path.lineTo(w * 0.47f, h * 0.56f)
                path.lineTo(w * 0.40f, h * 0.94f)
                path.lineTo(w * 0.78f, h * 0.40f)
                path.lineTo(w * 0.53f, h * 0.40f)
                path.close()
                p.style = Paint.Style.FILL
                c.drawPath(path, p)
            }
            PLAY -> {
                path.moveTo(w * 0.28f, h * 0.15f)
                path.lineTo(w * 0.86f, h * 0.5f)
                path.lineTo(w * 0.28f, h * 0.85f)
                path.close()
                p.style = Paint.Style.FILL
                c.drawPath(path, p)
            }
            PAUSE -> {
                p.style = Paint.Style.FILL
                c.drawRoundRect(RectF(w * 0.22f, h * 0.15f, w * 0.42f, h * 0.85f), 3f, 3f, p)
                c.drawRoundRect(RectF(w * 0.58f, h * 0.15f, w * 0.78f, h * 0.85f), 3f, 3f, p)
            }
            PREV -> {
                p.style = Paint.Style.FILL
                path.moveTo(w * 0.82f, h * 0.2f); path.lineTo(w * 0.32f, h * 0.5f); path.lineTo(w * 0.82f, h * 0.8f); path.close()
                c.drawPath(path, p)
                c.drawRoundRect(RectF(w * 0.14f, h * 0.2f, w * 0.26f, h * 0.8f), 3f, 3f, p)
            }
            NEXT -> {
                p.style = Paint.Style.FILL
                path.moveTo(w * 0.18f, h * 0.2f); path.lineTo(w * 0.68f, h * 0.5f); path.lineTo(w * 0.18f, h * 0.8f); path.close()
                c.drawPath(path, p)
                c.drawRoundRect(RectF(w * 0.74f, h * 0.2f, w * 0.86f, h * 0.8f), 3f, 3f, p)
            }
            BARS -> {
                p.style = Paint.Style.FILL
                val bw = w * 0.16f
                for (i in 0..3) {
                    val t = if (phase == 0f) 0.45f else 0.25f + 0.75f * Math.abs(Math.sin((phase * 2.0 + i * 0.37) * Math.PI)).toFloat()
                    val bh = h * t
                    val x = w * 0.08f + i * (bw + w * 0.07f)
                    c.drawRoundRect(RectF(x, (h - bh) / 2f, x + bw, (h + bh) / 2f), bw / 2f, bw / 2f, p)
                }
            }
            NOTE -> {
                p.style = Paint.Style.FILL
                c.drawCircle(w * 0.34f, h * 0.76f, w * 0.17f, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = w * 0.1f
                c.drawLine(w * 0.5f, h * 0.76f, w * 0.5f, h * 0.18f, p)
                c.drawLine(w * 0.5f, h * 0.18f, w * 0.82f, h * 0.3f, p)
                p.style = Paint.Style.FILL
            }
        }
    }

    companion object {
        const val BOLT = 0
        const val PLAY = 1
        const val PAUSE = 2
        const val PREV = 3
        const val NEXT = 4
        const val BARS = 5
        const val NOTE = 6
    }
}

/**
 * Dynamic-Island style pill drawn around the punch-hole camera, system wide
 * (TYPE_ACCESSIBILITY_OVERLAY, so no "draw over other apps" permission is needed).
 */
class IslandOverlay(private val ctx: Context) {
    private enum class Mode { HIDDEN, IDLE, COMPACT, EXPANDED }

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = ctx.resources.displayMetrics.density
    private val screenW get() = ctx.resources.displayMetrics.widthPixels
    private val handler = Handler(Looper.getMainLooper())

    private fun dp(v: Float): Int = (v * density).toInt()

    private var s = LauncherSettings()
    private var mode = Mode.HIDDEN
    private var attached = false
    private var preview = false
    private var current: IslandEvent? = null
    private var media: MediaState? = null
    private var animator: ValueAnimator? = null
    private var curW = 0
    private var curH = 0

    private val bg = GradientDrawable().apply { setColor(Color.BLACK) }

    private val leftIcon = ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
    private val leftGlyph = GlyphView(ctx, GlyphView.BOLT)
    private val rightText = TextView(ctx).apply {
        setTextColor(Color.WHITE); textSize = 12f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; gravity = Gravity.END or Gravity.CENTER_VERTICAL
    }
    private val rightGlyph = GlyphView(ctx, GlyphView.BARS)
    private val gap = View(ctx)

    private val compactRow = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val leftBox = FrameLayout(ctx)
    private val rightBox = FrameLayout(ctx)

    private val expTitle = TextView(ctx).apply {
        setTextColor(Color.WHITE); textSize = 15f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val expSub = TextView(ctx).apply {
        setTextColor(Color.parseColor("#9A9A9A")); textSize = 12f; maxLines = 2; ellipsize = TextUtils.TruncateAt.END
    }
    private val prev = GlyphView(ctx, GlyphView.PREV)
    private val play = GlyphView(ctx, GlyphView.PAUSE)
    private val next = GlyphView(ctx, GlyphView.NEXT)
    private val controls = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }
    private val expanded = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

    private val pill = FrameLayout(ctx)

    private val root = object : FrameLayout(ctx) {
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_OUTSIDE && mode == Mode.EXPANDED) settle()
            return super.onTouchEvent(event)
        }
    }

    private val lp = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    init {
        pill.background = bg
        pill.clipChildren = true
        leftBox.addView(leftIcon, FrameLayout.LayoutParams(dp(22f), dp(22f), Gravity.CENTER_VERTICAL or Gravity.START))
        leftBox.addView(leftGlyph, FrameLayout.LayoutParams(dp(16f), dp(16f), Gravity.CENTER_VERTICAL or Gravity.START))
        rightBox.addView(rightText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        rightBox.addView(rightGlyph, FrameLayout.LayoutParams(dp(18f), dp(14f), Gravity.CENTER_VERTICAL or Gravity.END))
        compactRow.addView(leftBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        compactRow.addView(gap, LinearLayout.LayoutParams(dp(32f), 1))
        compactRow.addView(rightBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        pill.addView(compactRow, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32f), Gravity.TOP))

        for (g in listOf(prev, play, next)) {
            controls.addView(g, LinearLayout.LayoutParams(dp(26f), dp(26f)).apply { setMargins(dp(18f), 0, dp(18f), 0) })
        }
        expanded.addView(expTitle)
        expanded.addView(expSub)
        expanded.addView(controls, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40f)))
        expanded.visibility = View.GONE
        pill.addView(expanded, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))

        root.addView(pill, FrameLayout.LayoutParams(dp(34f), dp(34f)))

        pill.setOnClickListener { onPillClick() }
        prev.setOnClickListener { NoirBus.mediaController?.transportControls?.skipToPrevious(); touch() }
        next.setOnClickListener { NoirBus.mediaController?.transportControls?.skipToNext(); touch() }
        play.setOnClickListener {
            val tc = NoirBus.mediaController?.transportControls
            if (media?.playing == true) tc?.pause() else tc?.play()
            touch()
        }
    }

    // ---- public API (all called on the main thread) ----

    fun applySettings(n: LauncherSettings) {
        s = n
        if (!n.islandEnabled) { setMode(Mode.HIDDEN, false); return }
        settle(animate = false)
    }

    fun setPreview(on: Boolean) {
        preview = on
        if (!s.islandEnabled) return
        if (on) { current = IslandEvent("charge", "Charging", "79%") }
        else if (current?.title == "Charging" && current?.sub == "79%") current = null
        settle()
    }

    fun onMedia(m: MediaState?) {
        media = m
        if (mode == Mode.EXPANDED && current?.kind == "media") bind()
        if (mode != Mode.EXPANDED) settle()
    }

    fun onEvent(e: IslandEvent) {
        if (!s.islandEnabled) return
        when (e.kind) {
            "charge", "unplug" -> if (!s.islandCharging) return
            "notif" -> if (!s.islandNotifs) return
            "media" -> if (!s.islandMedia) return
        }
        if (mode == Mode.EXPANDED && current?.kind == "media" && e.kind != "media") return
        current = e
        setMode(Mode.COMPACT, true)
        scheduleSettle(4000)
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        animator?.cancel()
        detach()
    }

    // ---- state ----

    private val settleRunnable = Runnable { current = if (preview) current else null; settle() }

    private fun scheduleSettle(ms: Long) {
        handler.removeCallbacks(settleRunnable)
        handler.postDelayed(settleRunnable, ms)
    }

    private fun touch() = scheduleSettle(6000)

    private fun settle(animate: Boolean = true) {
        handler.removeCallbacks(settleRunnable)
        val m = media
        val target = when {
            !s.islandEnabled -> Mode.HIDDEN
            preview -> Mode.COMPACT
            m != null && m.playing && s.islandMedia -> Mode.COMPACT
            s.islandAlways -> Mode.IDLE
            else -> Mode.HIDDEN
        }
        if (target == Mode.COMPACT && current == null && m != null) {
            current = IslandEvent("media", m.title, m.artist, m.pkg)
        }
        if (target != Mode.COMPACT && !preview) current = null
        setMode(target, animate)
    }

    private fun onPillClick() {
        val c = current
        when (mode) {
            Mode.COMPACT -> {
                if (c == null) return
                setMode(Mode.EXPANDED, true)
                scheduleSettle(6000)
            }
            Mode.EXPANDED -> {
                openSource()
                settle()
            }
            else -> Unit
        }
    }

    private fun openSource() {
        val c = current ?: return
        try {
            if (c.intent != null) {
                c.intent.send()
            } else if (c.pkg != null) {
                ctx.packageManager.getLaunchIntentForPackage(c.pkg)?.let {
                    ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        } catch (e: Exception) { }
    }

    private fun setMode(m: Mode, animate: Boolean) {
        mode = m
        if (m == Mode.HIDDEN) {
            animator?.cancel()
            detach()
            return
        }
        attach()
        bind()
        val idle = dp(s.islandSize)
        val maxW = screenW - dp(24f)
        val (tw, th) = when (m) {
            Mode.IDLE -> idle to idle
            Mode.COMPACT -> minOf(maxW, dp(190f * s.islandScale + s.islandSize)) to idle + dp(4f * s.islandScale)
            else -> minOf(maxW, dp(330f * s.islandScale)) to (idle + dp(if (current?.kind == "media") 96f else 56f) )
        }
        // vertical position: top of pill sits at the user-tuned offset
        lp.y = dp(s.islandTop)
        try { if (attached) wm.updateViewLayout(root, lp) } catch (e: Exception) { }
        (compactRow.layoutParams as FrameLayout.LayoutParams).height = idle
        (gap.layoutParams as LinearLayout.LayoutParams).width = idle + dp(6f)
        expanded.setPadding(dp(20f), idle + dp(2f), dp(20f), 0)
        expanded.visibility = if (m == Mode.EXPANDED) View.VISIBLE else View.GONE
        compactRow.visibility = if (m == Mode.IDLE) View.INVISIBLE else View.VISIBLE
        val pad = dp(14f)
        leftBox.setPadding(pad, 0, 0, 0)
        rightBox.setPadding(0, 0, pad, 0)

        val fromW = if (curW == 0) idle else curW
        val fromH = if (curH == 0) idle else curH
        animator?.cancel()
        if (!animate) {
            applySize(tw, th)
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 340
            interpolator = if (m == Mode.EXPANDED || m == Mode.COMPACT) OvershootInterpolator(0.7f) else DecelerateInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                applySize((fromW + (tw - fromW) * f).toInt(), (fromH + (th - fromH) * f).toInt())
            }
            start()
        }
    }

    private fun applySize(w: Int, h: Int) {
        curW = w
        curH = h
        val params = pill.layoutParams
        params.width = w.coerceAtLeast(1)
        params.height = h.coerceAtLeast(1)
        pill.layoutParams = params
        bg.cornerRadius = minOf(h / 2f, dp(30f).toFloat())
    }

    private fun bind() {
        val c = current
        val m = media
        leftIcon.visibility = View.GONE
        leftGlyph.visibility = View.GONE
        rightText.visibility = View.GONE
        rightGlyph.visibility = View.GONE
        controls.visibility = View.GONE
        if (c != null) when (c.kind) {
            "charge", "unplug" -> {
                leftGlyph.kind = GlyphView.BOLT
                leftGlyph.setTint(if (c.kind == "charge") Color.parseColor("#7CF29A") else Color.parseColor("#9A9A9A"))
                leftGlyph.visibility = View.VISIBLE
                rightText.text = c.sub
                rightText.visibility = View.VISIBLE
                expTitle.text = c.title
                expSub.text = c.sub
            }
            "notif" -> {
                val d = try { c.pkg?.let { ctx.packageManager.getApplicationIcon(it) } } catch (e: Exception) { null }
                if (d != null) { leftIcon.setImageDrawable(d); leftIcon.visibility = View.VISIBLE }
                else { leftGlyph.kind = GlyphView.NOTE; leftGlyph.setTint(Color.WHITE); leftGlyph.visibility = View.VISIBLE }
                rightText.text = c.title
                rightText.visibility = View.VISIBLE
                expTitle.text = c.title
                expSub.text = c.sub
            }
            "media" -> {
                val art = m?.art
                if (art != null) { leftIcon.setImageBitmap(art); leftIcon.visibility = View.VISIBLE }
                else { leftGlyph.kind = GlyphView.NOTE; leftGlyph.setTint(Color.WHITE); leftGlyph.visibility = View.VISIBLE }
                rightGlyph.kind = GlyphView.BARS
                rightGlyph.animateBars(m?.playing == true)
                rightGlyph.visibility = View.VISIBLE
                expTitle.text = m?.title ?: c.title
                expSub.text = m?.artist ?: c.sub
                controls.visibility = View.VISIBLE
                play.kind = if (m?.playing == true) GlyphView.PAUSE else GlyphView.PLAY
                play.invalidate()
            }
            else -> Unit
        }
    }

    private fun attach() {
        if (attached) return
        try {
            wm.addView(root, lp)
            attached = true
        } catch (e: Exception) {
            attached = false
        }
    }

    private fun detach() {
        if (!attached) return
        try { wm.removeView(root) } catch (e: Exception) { }
        attached = false
        curW = 0
        curH = 0
    }
}
