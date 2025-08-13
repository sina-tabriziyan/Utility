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
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class FontIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    enum class BackgroundShape { OVAL, RECTANGLE, ROUNDED }

    // ---------- NEW: radial overlay state ----------
    private var overlay: RadialIconsOverlay? = null
    private var showingOverlay = false
    private var surroundingIconCodes: List<String> = emptyList()
    private val overlayInterpolator: TimeInterpolator = OvershootInterpolator(1.6f)
    private val overlayRadiusDp = 64f          // distance from main icon center
    private val overlayItemSizeDp = 28f        // diameter/side of preview icons
    private val overlayAnimDuration = 200L
    // ------------------------------------------------

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
                        val rippleColorStateList = ColorStateList.valueOf(Color.parseColor("#33000000"))
                        background = RippleDrawable(rippleColorStateList, background, null)
                    }
                }
                strokeColor = typedArray.getColor(R.styleable.FontIcon_strokeColor, Color.TRANSPARENT)
                strokeWidth = typedArray.getDimensionPixelSize(R.styleable.FontIcon_strokeWidth, 0)

                gravity = Gravity.CENTER
            } finally {
                typedArray.recycle()
            }
        }

        if (shouldCreateCustomBackground) {
            background = createBackgroundDrawable(desiredShape, bgColor, enableRipple, strokeColor, strokeWidth)
        }
        isClickable = true
        isFocusable = true

        // ---------- NEW: interactions ----------
        setOnLongClickListener {
            if (surroundingIconCodes.isNotEmpty() && !showingOverlay) {
                showRadialOverlay()
                true
            } else false
        }
        setOnClickListener {
            if (showingOverlay) hideRadialOverlay()
            // else: let your normal click do whatever it does
        }
        // --------------------------------------
    }

    fun setIcon(iconCode: String) {
        try {
            val iconChar = String(Character.toChars(iconCode.toInt(16)))
            text = iconChar
        } catch (_: Exception) { /* ignore */ }
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

    // ---------- NEW: radial overlay implementation ----------
    private fun showRadialOverlay() {
        val parent = parent as? ViewGroup ?: return
        if (overlay == null) {
            overlay = RadialIconsOverlay(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // purely decorative: do not consume touches; do not block main icon
                isClickable = false
                isFocusable = false
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            // Put overlay ABOVE siblings but it only has small children, not covering main
            parent.addView(overlay)
        }
        overlay?.let { layer ->
            layer.showIconsAround(
                anchor = this,
                hexCodes = surroundingIconCodes,
                radiusPx = dp(overlayRadiusDp),
                itemSizePx = dp(overlayItemSizeDp),
                duration = overlayAnimDuration,
                interpolator = overlayInterpolator
            )
            showingOverlay = true
        }
    }

    private fun hideRadialOverlay() {
        overlay?.hideIcons(
            anchor = this,
            duration = overlayAnimDuration
        ) {
            // Optional: keep overlay for reuse; or remove when empty
            // (kept for reuse to avoid reallocation)
        }
        showingOverlay = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // clean up if view is removed
        (overlay?.parent as? ViewGroup)?.removeView(overlay)
        overlay = null
        showingOverlay = false
    }

    private fun dp(v: Float): Int =
        (v * resources.displayMetrics.density).toInt()

    // A simple overlay that lays out tiny FontIcon views around the anchor.
    private class RadialIconsOverlay(context: Context) : FrameLayout(context) {

        private val previewViews = mutableListOf<AppCompatTextView>()

        fun showIconsAround(
            anchor: View,
            hexCodes: List<String>,
            radiusPx: Int,
            itemSizePx: Int,
            duration: Long,
            interpolator: TimeInterpolator
        ) {
            removeAllViews()
            previewViews.clear()

            if (hexCodes.isEmpty()) return

            // Anchor center within this overlay's coordinate system
            val anchorCenter = IntArray(2)
            val overlayLoc = IntArray(2)

            anchor.getLocationOnScreen(anchorCenter)
            getLocationOnScreen(overlayLoc)

            val centerX = anchorCenter[0] - overlayLoc[0] + anchor.width / 2f
            val centerY = anchorCenter[1] - overlayLoc[1] + anchor.height / 2f

            val count = hexCodes.size
            val angleStep = (2 * Math.PI / count)

            hexCodes.forEachIndexed { index, hex ->
                val tv = AppCompatTextView(context).apply {
                    // Use same typeface as main
                    typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")
                    textSize = 16f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    isClickable = false
                    isLongClickable = false
                    isFocusable = false
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    layoutParams = LayoutParams(itemSizePx, itemSizePx)
                    text = try {
                        String(Character.toChars(hex.toInt(16)))
                    } catch (_: Exception) { "?" }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#F2F2F2"))
                    }
                    // start collapsed at anchor center
                    x = centerX - itemSizePx / 2f
                    y = centerY - itemSizePx / 2f
                    scaleX = 0.6f
                    scaleY = 0.6f
                    alpha = 0f
                }
                addView(tv)
                previewViews += tv

                // Target position on the circle
                val angle = index * angleStep - Math.PI / 2  // start upward
                val targetX = (centerX + radiusPx * cos(angle)).toFloat() - itemSizePx / 2f
                val targetY = (centerY + radiusPx * sin(angle)).toFloat() - itemSizePx / 2f

                tv.animate()
                    .x(targetX)
                    .y(targetY)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .start()
            }
        }

        fun hideIcons(
            anchor: View,
            duration: Long,
            end: (() -> Unit)? = null
        ) {
            if (previewViews.isEmpty()) {
                end?.invoke()
                return
            }
            val anchorCenter = IntArray(2)
            val overlayLoc = IntArray(2)
            anchor.getLocationOnScreen(anchorCenter)
            getLocationOnScreen(overlayLoc)
            val centerX = anchorCenter[0] - overlayLoc[0] + anchor.width / 2f
            val centerY = anchorCenter[1] - overlayLoc[1] + anchor.height / 2f

            var remaining = previewViews.size
            previewViews.forEach { v ->
                v.animate()
                    .x(centerX - v.width / 2f)
                    .y(centerY - v.height / 2f)
                    .alpha(0f)
                    .scaleX(0.6f)
                    .scaleY(0.6f)
                    .setDuration(duration)
                    .withEndAction {
                        if (--remaining == 0) {
                            removeAllViews()
                            previewViews.clear()
                            end?.invoke()
                        }
                    }
                    .start()
            }
        }
    }
}
