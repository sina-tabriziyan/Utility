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

    // --- Radial menu public API ---
    var radialMenuIcons: List<String> = emptyList()
    var radialItemScale: Float = 0.8f     // relative to this view size
    var radialRadiusPx: Float = 160f      // absolute px radius
    var radialUseHaptics: Boolean = true

    enum class BackgroundShape { OVAL, RECTANGLE, ROUNDED }

    private var lastPopupAt = 0L

    private val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                showRadialIfPossible()
            }
        })

    init {
        // Load the custom font (won't crash if missing)
        try {
            typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")
        } catch (t: Throwable) {
            Log.w("FontIcon", "fonticons.ttf not found in /assets (only affects glyphs).", t)
        }

        // Defaults
        var desiredShape = BackgroundShape.RECTANGLE
        var bgColor = Color.TRANSPARENT
        var enableRipple = false
        var shouldCreateCustomBackground = this.background == null
        var strokeColor = Color.TRANSPARENT
        var strokeWidth = 0

        // Read custom attrs if present
        attrs?.let { attributeSet ->
            val ta = context.obtainStyledAttributes(attributeSet, R.styleable.FontIcon)
            try {
                val iconCode = ta.getString(R.styleable.FontIcon_setIcon)
                if (!iconCode.isNullOrEmpty()) setIcon(iconCode)

                val tintColor = ta.getColor(R.styleable.FontIcon_tint, currentTextColor)
                setTextColor(tintColor)

                if (shouldCreateCustomBackground) {
                    desiredShape = when (ta.getInt(R.styleable.FontIcon_backgroundShape, 1)) {
                        0 -> BackgroundShape.OVAL
                        1 -> BackgroundShape.RECTANGLE
                        2 -> BackgroundShape.ROUNDED
                        else -> BackgroundShape.RECTANGLE
                    }
                    bgColor = ta.getColor(R.styleable.FontIcon_backgroundColor, Color.TRANSPARENT)
                    enableRipple = ta.getBoolean(R.styleable.FontIcon_ripple, false)
                } else {
                    enableRipple = ta.getBoolean(R.styleable.FontIcon_ripple, false)
                    if (enableRipple &&
                        this.background != null &&
                        this.background !is RippleDrawable &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    ) {
                        val ripple = ColorStateList.valueOf(Color.parseColor("#33000000"))
                        this.background = RippleDrawable(ripple, this.background, null)
                    }
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

        isClickable = true
        isFocusable = true
        isLongClickable = true

        // Classic long-click (works in simple containers)
        setOnLongClickListener {
            showRadialIfPossible()
        }

        // Robust long-press: avoid parent intercept (Recycler/ScrollView)
        setOnTouchListener { v, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                v.parent?.requestDisallowInterceptTouchEvent(true) // ViewParent method
            }
            detector.onTouchEvent(ev)
            false
        }

    }

    private fun showRadialIfPossible(): Boolean {
        if (radialMenuIcons.isEmpty()) {
            Log.w("FontIcon", "radialMenuIcons is empty — nothing to show.")
            return true
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastPopupAt < 250) return true // guard double trigger
        lastPopupAt = now

        if (radialUseHaptics) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
            val rippleColor = ColorStateList.valueOf(Color.parseColor("#33000000"))
            RippleDrawable(rippleColor, shapeDrawable, null)
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
        val ctx = anchor.context
        val activity = findActivity(anchor)
        val attachTo: ViewGroup? = activity?.findViewById(android.R.id.content)
            ?: (anchor.rootView as? ViewGroup)

        if (attachTo == null) {
            Log.w("RadialIconPopup", "No suitable parent to attach PopupWindow.")
            return
        }

        val root = FrameLayout(ctx).apply {
            isClickable = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // outside tap to dismiss
            setOnClickListener { popup?.dismiss() }
            // DEBUG: uncomment to verify overlay is shown
            // setBackgroundColor(0x11FF0000.toInt())
        }

        val pw = PopupWindow(
            root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true // focusable => back button & outside dismiss work
        ).apply {
            isClippingEnabled = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // must be non-null
            isOutsideTouchable = true
            elevation = 0f
        }
        popup = pw

        // Show FIRST so root has coordinates/window
        pw.showAtLocation(attachTo, Gravity.NO_GRAVITY, 0, 0)

        // Lay out children after attach
        root.post {
            // Anchor center (SCREEN coords)
            val anchorLoc = IntArray(2)
            anchor.getLocationOnScreen(anchorLoc)
            val anchorCxScreen = anchorLoc[0] + anchor.width / 2f
            val anchorCyScreen = anchorLoc[1] + anchor.height / 2f

            // Root origin (SCREEN coords)
            val rootLoc = IntArray(2)
            root.getLocationOnScreen(rootLoc)
            val rootX = rootLoc[0].toFloat()
            val rootY = rootLoc[1].toFloat()

            val itemSize = (min(anchor.width, anchor.height) * itemScale).toInt().coerceAtLeast(24)
            val count = iconHexList.size.coerceAtLeast(1)
            val angleStep = 360f / count

            iconHexList.forEachIndexed { idx, hex ->
                val angleDeg = idx * angleStep - 90f // start at top
                val angleRad = Math.toRadians(angleDeg.toDouble())

                // target center in SCREEN coords
                val cxScreen = (anchorCxScreen + radiusPx * cos(angleRad)).toFloat()
                val cyScreen = (anchorCyScreen + radiusPx * sin(angleRad)).toFloat()

                // convert to ROOT coords
                val cx = cxScreen - rootX
                val cy = cyScreen - rootY

                val child = FontIcon(ctx).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setIcon(hex)
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    // subtle rounded bg for better hit area
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = itemSize * 0.3f
                        setColor(Color.parseColor("#F2FFFFFF"))
                    }
                    contentDescription = "Icon $hex"
                }

                val lp = FrameLayout.LayoutParams(itemSize, itemSize).apply {
                    leftMargin = (cx - itemSize / 2f).toInt()
                    topMargin  = (cy - itemSize / 2f).toInt()
                }
                root.addView(child, lp)

                // animate from anchor center (in ROOT coords)
                val anchorCxRoot = anchorCxScreen - rootX
                val anchorCyRoot = anchorCyScreen - rootY

                child.scaleX = 0f
                child.scaleY = 0f
                child.alpha = 0f
                child.translationX = (anchorCxRoot - cx)
                child.translationY = (anchorCyRoot - cy)

                child.animate()
                    .scaleX(1f).scaleY(1f)
                    .alpha(1f)
                    .translationX(0f).translationY(0f)
                    .setDuration(140)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                child.setOnClickListener {
                    onPick(hex)
                    pw.dismiss()
                }
            }
        }
    }

    private fun findActivity(view: View): Activity? {
        var ctx: Context = view.context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
