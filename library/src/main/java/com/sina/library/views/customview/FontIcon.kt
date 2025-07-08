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

    enum class TextPosition {
        TOP, BOTTOM, START, END
    }

    private var iconCode: String? = null
    private var textPosition: TextPosition = TextPosition.BOTTOM
    private var customText: String? = null

    init {
        // Load the custom font
        typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")

        // Default values
        var desiredShape = BackgroundShape.RECTANGLE
        var bgColor = Color.TRANSPARENT
        var enableRipple = false
        var shouldCreateCustomBackground = true

        if (this.background != null) {
            shouldCreateCustomBackground = false
        }

        attrs?.let { attributeSet ->
            val typedArray = context.obtainStyledAttributes(
                attributeSet,
                R.styleable.FontIcon,
                defStyleAttr,
                0
            )
            try {
                iconCode = typedArray.getString(R.styleable.FontIcon_setIcon)
                customText = typedArray.getString(R.styleable.FontIcon_text)

                textPosition = when (typedArray.getInt(R.styleable.FontIcon_textPosition, 1)) {
                    0 -> TextPosition.TOP
                    1 -> TextPosition.BOTTOM
                    2 -> TextPosition.START
                    3 -> TextPosition.END
                    else -> TextPosition.BOTTOM
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

                    bgColor = typedArray.getColor(R.styleable.FontIcon_backgroundColor, Color.TRANSPARENT)
                    enableRipple = typedArray.getBoolean(R.styleable.FontIcon_ripple, false)
                } else {
                    enableRipple = typedArray.getBoolean(R.styleable.FontIcon_ripple, false)
                    if (enableRipple && this.background != null && this.background !is RippleDrawable &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val rippleColorStateList = ColorStateList.valueOf(Color.parseColor("#33000000"))
                        this.background = RippleDrawable(rippleColorStateList, this.background, null)
                    }
                }
            } finally {
                typedArray.recycle()
            }
        }

        // Apply background with optional ripple
        if (shouldCreateCustomBackground) {
            background = createBackgroundDrawable(desiredShape, bgColor, enableRipple)
        }

        // Set the compound drawable and text
        updateContent()

        // Enable ripple effect
        isClickable = true
        isFocusable = true
    }

    fun setIcon(iconCode: String) {
        try {
            this.iconCode = iconCode
            updateContent()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTextPosition(position: TextPosition) {
        this.textPosition = position
        updateContent()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        this.customText = text?.toString()
        updateContent()
    }

    private fun updateContent() {
        val icon = iconCode?.let {
            try {
                String(Character.toChars(it.toInt(16)))
            } catch (e: Exception) {
                null
            }
        }

        val text = customText ?: ""

        when (textPosition) {
            TextPosition.TOP -> {
                super.setText(text + "\n" + icon, BufferType.NORMAL)
                gravity = Gravity.CENTER
            }
            TextPosition.BOTTOM -> {
                super.setText(icon + "\n" + text, BufferType.NORMAL)
                gravity = Gravity.CENTER
            }
            TextPosition.START -> {
                super.setText(text + " " + icon, BufferType.NORMAL)
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
            TextPosition.END -> {
                super.setText(icon + " " + text, BufferType.NORMAL)
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
        }
    }

    private fun createBackgroundDrawable(
        shape: BackgroundShape,
        color: Int,
        ripple: Boolean
    ): Drawable {
        val shapeDrawable = GradientDrawable().apply {
            this.shape = when (shape) {
                BackgroundShape.OVAL -> GradientDrawable.OVAL
                else -> GradientDrawable.RECTANGLE
            }
            cornerRadius = if (shape == BackgroundShape.ROUNDED) 16f else 0f
            setColor(color)
        }

        return if (ripple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val rippleColor = ColorStateList.valueOf(Color.parseColor("#33000000"))
            RippleDrawable(rippleColor, shapeDrawable, null)
        } else {
            shapeDrawable
        }
    }
}