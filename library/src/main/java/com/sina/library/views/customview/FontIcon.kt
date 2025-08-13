package com.sina.library.views.customview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatTextView
import com.sina.library.utility.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class FontIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    // Radial menu public API
    var radialMenuIcons: List<String> = emptyList()
    var radialItemScale: Float = 0.8f
    var radialRadiusPx: Float = 160f
    var radialUseHaptics: Boolean = true

    enum class BackgroundShape { OVAL, RECTANGLE, ROUNDED }

    private var lastPopupAt = 0L

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                showRadialIfPossible()
            }
        })

    init {
        // Load font
        try {
            typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")
        } catch (t: Throwable) {
            Log.w("FontIcon", "fonticons.ttf not found", t)
        }

        // Defaults
        var desiredShape = BackgroundShape.RECTANGLE
        var bgColor = Color.TRANSPARENT
        var enableRipple = false
        var shouldCreateCustomBackground = background == null
        var strokeColor = Color.TRANSPARENT
        var strokeWidth = 0

        // Read attributes
        attrs?.let { attributeSet ->
            val ta = context.obtainStyledAttributes(attributeSet, R.styleable.FontIcon)
            try {
                val iconCode = ta.getString(R.styleable.FontIcon_setIcon)
                if (!iconCode.isNullOrEmpty()) setIcon(iconCode)

                setTextColor(ta.getColor(R.styleable.FontIcon_tint, currentTextColor))

                if (shouldCreateCustomBackground) {
                    desiredShape = when (ta.getInt(R.styleable.FontIcon_backgroundShape, 1)) {
                        0 -> BackgroundShape.OVAL
                        1 -> BackgroundShape.RECTANGLE
                        2 -> BackgroundShape.ROUNDED
                        else -> BackgroundShape.RECTANGLE
                    }
                    bgColor = ta.getColor(R.styleable.FontIcon_backgroundColor, Color.TRANSPARENT)
                    enableRipple = ta.getBoolean(R.styleable.FontIcon_ripple, false)
                }

                strokeColor = ta.getColor(R.styleable.FontIcon_strokeColor, Color.TRANSPARENT)
                strokeWidth = ta.getDimensionPixelSize(R.styleable.FontIcon_strokeWidth, 0)
                gravity = Gravity.CENTER
            } finally {
                ta.recycle()
            }
        }

        if (shouldCreateCustomBackground) {
            background = createBackgroundDrawable(
                desiredShape, bgColor, enableRipple, strokeColor, strokeWidth
            )
        }

        // Touch handling
        isClickable = true
        isFocusable = true
        isLongClickable = true

        setOnLongClickListener {
            showRadialIfPossible()
            true
        }

        setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    // Return true to ensure we get subsequent events
                    true
                }
                else -> false
            }
        }
    }

    private fun showRadialIfPossible(): Boolean {
        if (radialMenuIcons.isEmpty()) {
            Log.w("FontIcon", "No radial menu icons set")
            return true
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastPopupAt < 250) return true
        lastPopupAt = now

        if (radialUseHaptics) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        RadialIconPopup.show(
            anchor = this,
            iconHexList = radialMenuIcons,
            itemScale = radialItemScale,
            radiusPx = radialRadiusPx
        ) { pickedHex ->
            setIcon(pickedHex)
        }
        return true
    }

    fun setIcon(iconCode: String) {
        try {
            val iconChar = String(Character.toChars(iconCode.toInt(16)))
            text = iconChar
        } catch (e: Exception) {
            Log.e("FontIcon", "Invalid icon hex: $iconCode", e)
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
            RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#33000000")),
                shapeDrawable,
                null
            )
        } else {
            shapeDrawable
        }
    }
}



object RadialIconPopup {

    private var popup: PopupWindow? = null

    fun show(
        anchor: View,
        iconHexList: List<String>,
        itemScale: Float = 0.8f,
        radiusPx: Float = 160f,
        onPick: (String) -> Unit
    ) {
        // Dismiss any existing popup
        popup?.dismiss()

        val context = anchor.context
        val activity = context.findActivity()
        val rootView = activity?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
            ?: return

        val rootLayout = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { popup?.dismiss() }
        }

        popup = PopupWindow(
            rootLayout,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 10f
        }

        // Show the popup first to get proper measurements
        popup?.showAtLocation(rootView, Gravity.NO_GRAVITY, 0, 0)

        rootLayout.post {
            val anchorLocation = IntArray(2)
            anchor.getLocationOnScreen(anchorLocation)
            val anchorCenterX = anchorLocation[0] + anchor.width / 2f
            val anchorCenterY = anchorLocation[1] + anchor.height / 2f

            val rootLocation = IntArray(2)
            rootLayout.getLocationOnScreen(rootLocation)
            val rootX = rootLocation[0].toFloat()
            val rootY = rootLocation[1].toFloat()

            val itemSize = (min(anchor.width, anchor.height) * itemScale).toInt().coerceAtLeast(24)
            val count = iconHexList.size
            val angleStep = 360f / count

            iconHexList.forEachIndexed { index, hex ->
                val angleDeg = index * angleStep - 90f // Start from top
                val angleRad = Math.toRadians(angleDeg.toDouble())

                // Calculate position in screen coordinates
                val itemX = anchorCenterX + radiusPx * cos(angleRad).toFloat()
                val itemY = anchorCenterY + radiusPx * sin(angleRad).toFloat()

                // Convert to root layout coordinates
                val xInRoot = itemX - rootX
                val yInRoot = itemY - rootY

                val iconView = FontIcon(context).apply {
                    setIcon(hex)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.WHITE)
                        setStroke(2, Color.LTGRAY)
                    }
                    layoutParams = FrameLayout.LayoutParams(itemSize, itemSize).apply {
                        leftMargin = (xInRoot - itemSize / 2).toInt()
                        topMargin = (yInRoot - itemSize / 2).toInt()
                    }
                    scaleX = 0f
                    scaleY = 0f
                    alpha = 0f
                    translationX = (anchorCenterX - rootX - xInRoot)
                    translationY = (anchorCenterY - rootY - yInRoot)
                }

                rootLayout.addView(iconView)

                iconView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator())
                    .start()

                iconView.setOnClickListener {
                    onPick(hex)
                    popup?.dismiss()
                }
            }
        }
    }

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}
