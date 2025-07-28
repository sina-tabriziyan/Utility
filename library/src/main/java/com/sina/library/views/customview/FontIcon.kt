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


class FontIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

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
                // Get stroke color and width
                strokeColor =
                    typedArray.getColor(R.styleable.FontIcon_strokeColor, Color.TRANSPARENT)
                strokeWidth = typedArray.getDimensionPixelSize(R.styleable.FontIcon_strokeWidth, 0)
                // Handle textPosition (if you plan to use it)
                val textPositionValue = typedArray.getInt(R.styleable.FontIcon_textPosition, 0)
                val iconPosition = when (textPositionValue) {
                    0 -> Gravity.TOP
                    1 -> Gravity.BOTTOM
                    2 -> Gravity.START
                    3 -> Gravity.END
                    else -> Gravity.START
                }
                gravity = Gravity.CENTER or iconPosition

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
        // Enable ripple effect
        isClickable = true
        isFocusable = true
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