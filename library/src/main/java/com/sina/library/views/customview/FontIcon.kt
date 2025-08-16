package com.sina.library.views.customview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import com.sina.library.utility.R
import android.animation.TimeInterpolator
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import kotlin.collections.forEachIndexed
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class FontIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    enum class BackgroundShape { OVAL, RECTANGLE, ROUNDED }
    data class RadialItem(
        val hex: String,                 // icon code like "E001"
        val onClick: (() -> Unit)? = null
    )
    // ---------- NEW: radial overlay state ----------
    private var overlay: RadialIconsOverlay? = null
    private var showingOverlay = false
    private var surroundingIconCodes: List<String> = emptyList()
    private val overlayInterpolator: TimeInterpolator = OvershootInterpolator(1.6f)
    private val overlayRadiusDp = 64f          // distance from main icon center
    private val overlayItemSizeDp = 28f        // diameter/side of preview icons
    private val overlayAnimDuration = 200L

    private var radialItems: List<RadialItem> = emptyList()
    private var externalClick: ((FontIcon) -> Unit)? = null
    fun setOnMainClickListener(block: (FontIcon) -> Unit) {
        externalClick = block
    }
    private var onPreviewShown: (() -> Unit)? = null
    private var onPreviewHidden: (() -> Unit)? = null
    // ------------------------------------------------
    fun setPreviewVisibilityCallbacks(onShown: () -> Unit, onHidden: () -> Unit) {
        onPreviewShown = onShown
        onPreviewHidden = onHidden
    }
    fun setRadialMenu(items: List<RadialItem>) {
        radialItems = items
        // optional: keep compatibility with your old setter
        setSurroundingIcons(items.map { it.hex })
    }
    fun isRadialOpen(): Boolean = showingOverlay
    // inside class FontIcon : AppCompatTextView { ... }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // While the radial menu is visible, forward MOVE/UP to the overlay
        if (showingOverlay && overlay != null) {
            val screen = IntArray(2)
            overlay!!.getLocationOnScreen(screen)

            // convert this event to overlay coordinates
            val xInOverlay = event.rawX - screen[0]
            val yInOverlay = event.rawY - screen[1]

            val forwarded = MotionEvent.obtain(event)
            forwarded.offsetLocation(xInOverlay - event.x, yInOverlay - event.y)

            overlay!!.dispatchTouchEvent(forwarded)
            forwarded.recycle()

            // consume so the movable touch listener can’t move the button
            return true
        }

        // normal path (allows click/long-press when overlay is NOT showing)
        return super.onTouchEvent(event)
    }

    init {
        // Load the custom font
        typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")

        // Default values
        var desiredShape = BackgroundShape.RECTANGLE
        var bgColor = Color.TRANSPARENT
        var enableRipple = false
        var shouldCreateCustomBackground = background == null

        var strokeColor = Color.TRANSPARENT
        var strokeWidth = 0
        attrs?.let { attributeSet ->
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.FontIcon)
            try {
                val iconCode = typedArray.getString(R.styleable.FontIcon_setIcon)
                if (!iconCode.isNullOrEmpty()) setIcon(iconCode)

                val tintColor = typedArray.getColor(R.styleable.FontIcon_tint, currentTextColor)
                setTextColor(tintColor)

                if (shouldCreateCustomBackground) {
                    val shapeValue = typedArray.getInt(R.styleable.FontIcon_backgroundShape, 1)
                    desiredShape = when (shapeValue) {
                        0 -> BackgroundShape.OVAL
                        1 -> BackgroundShape.RECTANGLE
                        2 -> BackgroundShape.ROUNDED
                        else -> BackgroundShape.RECTANGLE
                    }
                    bgColor =
                        typedArray.getColor(R.styleable.FontIcon_backgroundColor, Color.TRANSPARENT)
                    enableRipple = typedArray.getBoolean(R.styleable.FontIcon_ripple, false)
                } else {
                    enableRipple = typedArray.getBoolean(R.styleable.FontIcon_ripple, false)
                    if (enableRipple && background != null && background !is RippleDrawable &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    ) {
                        val rippleColorStateList =
                            ColorStateList.valueOf(Color.parseColor("#33000000"))
                        background = RippleDrawable(rippleColorStateList, background, null)
                    }
                }
                strokeColor =
                    typedArray.getColor(R.styleable.FontIcon_strokeColor, Color.TRANSPARENT)
                strokeWidth = typedArray.getDimensionPixelSize(R.styleable.FontIcon_strokeWidth, 0)

                gravity = Gravity.CENTER
            } finally {
                typedArray.recycle()
            }
        }

        if (shouldCreateCustomBackground) {
            background = createBackgroundDrawable(
                desiredShape,
                bgColor,
                enableRipple,
                strokeColor,
                strokeWidth
            )
        }
        isClickable = true
        isFocusable = true

        // ---------- NEW: interactions ----------
        setOnLongClickListener {
            if (radialItems.isNotEmpty() && !showingOverlay) {
                showInteractiveOverlay()
                true
            } else false
        }
        setOnClickListener {
            if (showingOverlay) {
                hideInteractiveOverlay()
            } else {
                externalClick?.invoke(this)
            }
        }
        // --------------------------------------
    }
    private fun showInteractiveOverlay() {
        val parent = parent as? ViewGroup ?: return
        onPreviewShown?.invoke()

        // Create once; attach only if not already attached
        if (overlay == null) {
            overlay = RadialIconsOverlay(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }

        // Attach if needed
        if (overlay?.parent == null) {
            parent.addView(overlay)
        } else {
            (overlay?.parent as? ViewGroup)?.bringChildToFront(overlay)
        }

        parent.requestDisallowInterceptTouchEvent(true)

        overlay?.showInteractive(
            anchor = this,
            items = radialItems,
            radiusPx = dp(overlayRadiusDp),
            itemSizePx = dp(overlayItemSizeDp),
            duration = overlayAnimDuration,
            interpolator = overlayInterpolator,
            onFinished = {
                // finished closing animation
                (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
                onPreviewHidden?.invoke()
                showingOverlay = false
                // Optionally keep overlay attached but empty; or remove now:
                // (overlay?.parent as? ViewGroup)?.removeView(overlay)
                // overlay = null
            }
        )
        showingOverlay = true
    }


    private fun hideInteractiveOverlay() {
        val parent = parent as? ViewGroup
        overlay?.hideIcons(anchor = this, duration = overlayAnimDuration) {
            parent?.requestDisallowInterceptTouchEvent(false)
            onPreviewHidden?.invoke()
            showingOverlay = false

            // SAFELY remove overlay **here**, not in onDetachedFromWindow
            (overlay?.parent as? ViewGroup)?.removeView(overlay)
            overlay = null
        }
    }


    fun setIcon(iconCode: String) {
        try {
            val iconChar = String(Character.toChars(iconCode.toInt(16)))
            text = iconChar
        } catch (_: Exception) { /* ignore */
        }
    }

    // ---------- NEW: API to set surrounding icon hex codes (no 0x) ----------
    fun setSurroundingIcons(hexCodes: List<String>) {
        surroundingIconCodes = hexCodes
    }
    // -----------------------------------------------------------------------

    private fun createBackgroundDrawable(
        shape: BackgroundShape,
        color: Int,
        ripple: Boolean,
        strokeColor: Int,
        strokeWidth: Int
    ): Drawable {
        val shapeDrawable = GradientDrawable().apply {
            this.shape = when (shape) {
                BackgroundShape.OVAL -> GradientDrawable.OVAL
                else -> GradientDrawable.RECTANGLE
            }
            cornerRadius = if (shape == BackgroundShape.ROUNDED) 16f else 0f
            setColor(color)
            if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
        }

        return if (ripple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val rippleColor = ColorStateList.valueOf(Color.parseColor("#33000000"))
            RippleDrawable(rippleColor, shapeDrawable, null)
        } else {
            shapeDrawable
        }
    }

    private fun showRadialOverlay() {
        val parent = parent as? ViewGroup ?: return

        if (overlay == null) {
            overlay = RadialIconsOverlay(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = true
                isFocusable = true
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            parent.addView(overlay)
        } else {
            (overlay?.parent as? ViewGroup)?.bringChildToFront(overlay)
        }

        val items: List<RadialItem> =
            if (radialItems.isNotEmpty()) radialItems
            else surroundingIconCodes.map { RadialItem(it) }

        if (items.isEmpty()) return

        parent.requestDisallowInterceptTouchEvent(true)

        overlay?.showInteractive(
            anchor = this,
            items = items,
            radiusPx = dp(overlayRadiusDp),
            itemSizePx = dp(overlayItemSizeDp),
            duration = overlayAnimDuration,
            interpolator = overlayInterpolator,
            onFinished = {
                (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
                showingOverlay = false
            }
        )

        showingOverlay = true
    }


    // Call this when you want to dismiss programmatically
    private fun hideRadialOverlay() {
        overlay?.hideIcons(anchor = this, duration = overlayAnimDuration) {
            (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
            showingOverlay = false
        }
    }



    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        overlay = null
        showingOverlay = false
    }

    private fun dp(v: Float): Int =
        (v * resources.displayMetrics.density).toInt()

    // A simple overlay that lays out tiny FontIcon views around the anchor.
    private class RadialIconsOverlay(context: Context) : FrameLayout(context) {

        private val views = mutableListOf<AppCompatTextView>()
        private var highlighted = -1
        private var centerX = 0f
        private var centerY = 0f
        private var onFinished: (() -> Unit)? = null
        private var items: List<RadialItem> = emptyList()

        fun showInteractive(
            anchor: View,
            items: List<RadialItem>,
            radiusPx: Int,
            itemSizePx: Int,
            duration: Long,
            interpolator: TimeInterpolator,
            onFinished: (() -> Unit)? = null
        ) {
            this.items = items
            this.onFinished = onFinished
            removeAllViews()
            views.clear()
            highlighted = -1

            // anchor center
            val a = IntArray(2)
            val o = IntArray(2)
            anchor.getLocationOnScreen(a); getLocationOnScreen(o)
            centerX = (a[0] - o[0] + anchor.width / 2f)
            centerY = (a[1] - o[1] + anchor.height / 2f)

            val count = items.size
            val step = (2 * Math.PI / count)

            items.forEachIndexed { i, item ->
                val tv = AppCompatTextView(context).apply {
                    typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")
                    text = runCatching { String(Character.toChars(item.hex.toInt(16))) }.getOrDefault("?")
                    textSize = 16f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    layoutParams = LayoutParams(itemSizePx, itemSizePx)
                    background = newBubbleBackground(false)
                    isClickable = false
                    isLongClickable = false
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    x = centerX - itemSizePx / 2f
                    y = centerY - itemSizePx / 2f
                    scaleX = 0.6f; scaleY = 0.6f; alpha = 0f
                }
                addView(tv)
                views += tv

                val angle = i * step - Math.PI / 2
                val tx = (centerX + radiusPx * cos(angle)).toFloat() - itemSizePx / 2f
                val ty = (centerY + radiusPx * sin(angle)).toFloat() - itemSizePx / 2f

                tv.animate()
                    .x(tx).y(ty).alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(duration).setInterpolator(interpolator).start()
            }

            // This overlay is interactive while visible
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        updateHighlight(ev.x, ev.y)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val picked = indexAt(ev.x, ev.y)
                        if (picked != -1) items[picked].onClick?.invoke()
                        dismiss(duration)
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        dismiss(duration)
                        true
                    }
                    else -> true // consume to block underlying views while active
                }
            }
        }
        fun showIconsAround(
            anchor: View,
            items: List<FontIcon.RadialItem>,
            radiusPx: Int,
            itemSizePx: Int,
            duration: Long,
            interpolator: TimeInterpolator
        ) {
            removeAllViews()
            val views = mutableListOf<AppCompatTextView>()
            var highlightedIndex = -1

            // Anchor center in overlay coords
            val anchorLoc = IntArray(2)
            val overlayLoc = IntArray(2)
            anchor.getLocationOnScreen(anchorLoc)
            getLocationOnScreen(overlayLoc)
            val centerX = (anchorLoc[0] - overlayLoc[0] + anchor.width / 2f)
            val centerY = (anchorLoc[1] - overlayLoc[1] + anchor.height / 2f)

            val count = items.size
            val angleStep = (2 * Math.PI / count)

            // Create each icon view
            items.forEachIndexed { index, item ->
                val tv = AppCompatTextView(context).apply {
                    typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")
                    text = runCatching { String(Character.toChars(item.hex.toInt(16))) }.getOrDefault("?")
                    textSize = 16f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    layoutParams = LayoutParams(itemSizePx, itemSizePx)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#F2F2F2"))
                    }
                    x = centerX - itemSizePx / 2f
                    y = centerY - itemSizePx / 2f
                    scaleX = 0.6f
                    scaleY = 0.6f
                    alpha = 0f
                }
                addView(tv)
                views += tv

                // Animate to circle position
                val angle = index * angleStep - Math.PI / 2
                val tx = (centerX + radiusPx * cos(angle)).toFloat() - itemSizePx / 2f
                val ty = (centerY + radiusPx * sin(angle)).toFloat() - itemSizePx / 2f
                tv.animate()
                    .x(tx).y(ty)
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .start()
            }

            // Make the overlay consume touches while menu is open
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        val idx = indexAt(ev.x, ev.y, views, itemSizePx)
                        if (idx != highlightedIndex) {
                            // remove old highlight
                            if (highlightedIndex in views.indices) {
                                (views[highlightedIndex].background as? GradientDrawable)?.setColor(Color.parseColor("#F2F2F2"))
                            }
                            // add new highlight
                            if (idx in views.indices) {
                                (views[idx].background as? GradientDrawable)?.setColor(Color.parseColor("#E8F0FF"))
                            }
                            highlightedIndex = idx
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (highlightedIndex in items.indices) {
                            items[highlightedIndex].onClick?.invoke()
                        }
                        dismiss(centerX, centerY, views, duration)
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        dismiss(centerX, centerY, views, duration)
                        true
                    }
                    else -> true
                }
            }
        }

        // Helper: find which icon index is under pointer
        private fun indexAt(
            x: Float,
            y: Float,
            views: List<View>,
            itemSizePx: Int
        ): Int {
            val threshold = (itemSizePx / 2f) * (itemSizePx / 2f)
            views.forEachIndexed { i, v ->
                val cx = v.x + v.width / 2f
                val cy = v.y + v.height / 2f
                val dist2 = (x - cx) * (x - cx) + (y - cy) * (y - cy)
                if (dist2 <= threshold * 4) { // slightly larger hit area
                    return i
                }
            }
            return -1
        }

        // Helper: animate all icons back to center and clear
// inside RadialIconsOverlay
        private fun dismiss(
            centerX: Float,
            centerY: Float,
            views: List<View>,
            duration: Long
        ) {
            var left = views.size
            views.forEach { v ->
                v.animate()
                    .x(centerX - v.width / 2f)
                    .y(centerY - v.height / 2f)
                    .alpha(0f).scaleX(0.6f).scaleY(0.6f)
                    .setDuration(duration)
                    .withEndAction {
                        if (--left == 0) {
                            removeAllViews()
                            isClickable = false
                            isFocusable = false
                            onFinished?.invoke() // <-- add this line
                        }
                    }
                    .start()
            }
        }


        fun hideIcons(anchor: View, duration: Long, end: (() -> Unit)? = null) {
            dismiss(duration, end)
        }

        // -------- helpers --------
        private fun newBubbleBackground(highlight: Boolean): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (highlight) Color.parseColor("#E8F0FF") else Color.parseColor("#F2F2F2"))
                setStroke(if (highlight) dp(2f) else 0, Color.parseColor("#2A6AFB"))
            }


        private fun updateHighlight(x: Float, y: Float) {
            val idx = indexAt(x, y)
            if (idx == highlighted) return

            if (highlighted in views.indices) applyHighlight(views[highlighted], false)
            highlighted = idx
            if (highlighted in views.indices) applyHighlight(views[highlighted], true)
        }
        private fun applyHighlight(view: View, on: Boolean) {
            view.background = newBubbleBackground(on)
            if (on) {
                view.animate().scaleX(1.12f).scaleY(1.12f).setDuration(80).start()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) view.elevation = dp(4f).toFloat()
            } else {
                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) view.elevation = 0f
            }
        }


        private fun indexAt(x: Float, y: Float): Int {
            // pick the closest view by center distance (inside a radius threshold)
            var best = -1
            var bestD = Float.MAX_VALUE
            views.forEachIndexed { i, v ->
                val cx = v.x + v.width / 2f
                val cy = v.y + v.height / 2f
                val d = (x - cx) * (x - cx) + (y - cy) * (y - cy)
                if (d < bestD) { bestD = d; best = i }
            }
            // optional: require the pointer to be reasonably close to the bubble
            val threshold = dp(36f) * dp(36f)
            return if (best != -1 && bestD <= threshold) best else -1
        }

        private fun dismiss(duration: Long, end: (() -> Unit)? = null) {
            var left = views.size
            views.forEach { v ->
                v.animate()
                    .x(centerX - v.width / 2f).y(centerY - v.height / 2f)
                    .alpha(0f).scaleX(0.6f).scaleY(0.6f)
                    .setDuration(duration)
                    .withEndAction {
                        if (--left == 0) {
                            removeAllViews()
                            views.clear()
                            isClickable = false
                            isFocusable = false
                            onFinished?.invoke()
                            end?.invoke()
                        }
                    }.start()
            }
        }

        private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()
    }


}
