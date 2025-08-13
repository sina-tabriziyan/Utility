package com.sina.library.views.customview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatTextView
import com.sina.library.utility.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.graphics.drawable.toDrawable


class FontIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    var radialMenuIcons: List<String> = emptyList()

    /** Size of menu items relative to this view (e.g., 0.8f = 80%) */
    var radialItemScale: Float = 0.8f

    /** Radius in px for the ring of items (you can tweak dynamically) */
    var radialRadiusPx: Float = 160f

    /** Haptic + animation toggles if you want them later */
    var radialUseHaptics: Boolean = true
    enum class BackgroundShape {
        OVAL, RECTANGLE, ROUNDED
    }

    init {
        // Load the custom font
        typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")

        // Default values
        var desiredShape = BackgroundShape.RECTANGLE
        var bgColor = Color.TRANSPARENT
        var enableRipple = false
        var shouldCreateCustomBackground = true // Assume we'll create it
        if (this.background != null) {
            shouldCreateCustomBackground = false
        }

        var strokeColor = Color.TRANSPARENT
        var strokeWidth = 0 // Default stroke width
        attrs?.let { attributeSet ->
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.FontIcon)
            try {
                val iconCode = typedArray.getString(R.styleable.FontIcon_setIcon)
                if (!iconCode.isNullOrEmpty()) {
                    setIcon(iconCode)
                }

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
                    if (enableRipple && this.background != null && this.background !is RippleDrawable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val rippleColorStateList =
                            ColorStateList.valueOf(Color.parseColor("#33000000"))
                        this.background =
                            RippleDrawable(rippleColorStateList, this.background, null)
                    }
                }
                strokeColor = typedArray.getColor(R.styleable.FontIcon_strokeColor, Color.TRANSPARENT)
                strokeWidth = typedArray.getDimensionPixelSize(R.styleable.FontIcon_strokeWidth, 0)
                gravity = Gravity.CENTER

            } finally {
                typedArray.recycle()
            }
        }


        // Apply background with optional ripple
        // OR if XML background was set, but we handled ripple above.
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

        setOnLongClickListener {
            if (radialMenuIcons.isNotEmpty()) {
                if (radialUseHaptics) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                RadialIconPopup.show(
                    anchor = this,
                    iconHexList = radialMenuIcons,
                    itemScale = radialItemScale,
                    radiusPx = radialRadiusPx
                ) { pickedHex ->
                    // When user picks, apply:
                    setIcon(pickedHex)
                }
                true
            } else {
                false
            }
        }
    }

    fun setIcon(iconCode: String) {
        try {
            val iconChar = String(Character.toChars(iconCode.toInt(16)))
            text = iconChar
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

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
            val rippleColor =
                ColorStateList.valueOf(Color.parseColor("#33000000")) // semi-transparent black
            RippleDrawable(rippleColor, shapeDrawable, null)
        } else {
            shapeDrawable
        }
    }

}


object RadialIconPopup {

    fun show(
        anchor: View,
        iconHexList: List<String>,
        itemScale: Float = 0.8f,
        radiusPx: Float = 160f,
        onPick: (String) -> Unit
    ) {
        val ctx = anchor.context
        val root = FrameLayout(ctx).apply {
            isClickable = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { popup?.dismiss() } // outside tap
        }

        // Build the popup
        val popupWindow = PopupWindow(
            root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            isClippingEnabled = false
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = 0f
        }

        popup = popupWindow

        // Post so anchor has final layout
        anchor.post {
            // Anchor center on screen
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val anchorCx = location[0] + anchor.width / 2f
            val anchorCy = location[1] + anchor.height / 2f

            // Compute child size
            val itemSize = (min(anchor.width, anchor.height) * itemScale).toInt().coerceAtLeast(24)

            // Distribute icons on a circle (you can switch to a 180° arc if preferred)
            val count = iconHexList.size.coerceAtLeast(1)
            val angleStep = (360f / count)

            iconHexList.forEachIndexed { idx, hex ->
                val angleDeg = idx * angleStep - 90f // start at top
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val cx = (anchorCx + radiusPx * cos(angleRad)).toFloat()
                val cy = (anchorCy + radiusPx * sin(angleRad)).toFloat()

                val child = FontIcon(ctx).apply {
                    // Smaller icon buttons
                    textSize = anchor.resources.displayMetrics.scaledDensity * 14 // adjust if needed
                    setIcon(hex)
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    // a subtle rounded bg so items are tappable
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = itemSize * 0.3f
                        setColor(Color.parseColor("#F2FFFFFF")) // translucent
                    }
                    // optional: ripple on children if you like
                }

                val lp = FrameLayout.LayoutParams(itemSize, itemSize)
                // Position by setting margins so its center lands on (cx, cy)
                lp.leftMargin = (cx - itemSize / 2f).toInt()
                lp.topMargin  = (cy - itemSize / 2f).toInt()
                root.addView(child, lp)

                // Animate from anchor center -> target
                child.scaleX = 0f
                child.scaleY = 0f
                child.alpha = 0f
                child.translationX = (anchorCx - cx)
                child.translationY = (anchorCy - cy)

                child.animate()
                    .scaleX(1f).scaleY(1f)
                    .alpha(1f)
                    .translationX(0f).translationY(0f)
                    .setDuration(140)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                child.setOnClickListener {
                    onPick(hex)
                    popupWindow.dismiss()
                }
            }
        }

        // Show it
        val parent = anchor.rootView
        popupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, 0, 0)
    }

    // Just to keep a handle for outside-tap dismiss
    private var popup: PopupWindow? = null
}
