package com.sina.library.views.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.doOnAttach
import com.sina.library.utility.R

/**
 * A 5-digit OTP input rendered as 5 square boxes.
 *
 * Usage:
 * <com.example.otpview.OtpEditText
 *     android:id="@+id/otp"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:digits="5"
 *     app:boxSize="48dp"
 *     app:boxSpacing="8dp"
 *     app:activeStrokeColor="@color/purple_500"
 *     app:inactiveStrokeColor="@color/grey_400"
 *     app:strokeWidth="2dp"
 *     app:textSize="24sp"
 *     app:textColor="@color/black" />
 */
class OtpEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val digits: Int
    private val boxSize: Float
    private val boxSpacing: Float
    private val strokeWidth: Float
    private val activeColor: Int
    private val inactiveColor: Int
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxRect = RectF()

    private var onOtpFilled: ((String) -> Unit)? = null

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.OtpEditText,
            defStyleAttr,
            0
        )
        digits = a.getInt(R.styleable.OtpEditText_digits, 5)
        boxSize = a.getDimension(R.styleable.OtpEditText_boxSize, 48f.dp())
        boxSpacing = a.getDimension(R.styleable.OtpEditText_boxSpacing, 8f.dp())
        strokeWidth = a.getDimension(R.styleable.OtpEditText_strokeWidth, 2f.dp())
        activeColor = a.getColor(R.styleable.OtpEditText_activeStrokeColor, Color.BLUE)
        inactiveColor = a.getColor(R.styleable.OtpEditText_inactiveStrokeColor, Color.GRAY)
        val textSizePx = a.getDimension(R.styleable.OtpEditText_otpTextSize, 24f.sp())
        val textColor = a.getColor(R.styleable.OtpEditText_otpTextColor, Color.BLACK)
        a.recycle()

        // Configure the backing EditText so it’s invisible
        background = null
        isCursorVisible = false
        filters = arrayOf(InputFilter.LengthFilter(digits))
        setTextColor(Color.TRANSPARENT)
        setHintTextColor(Color.TRANSPARENT)
        highlightColor = Color.TRANSPARENT
        setTextIsSelectable(false)

        // Paint for the digits
        textPaint.textSize = textSizePx
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.CENTER

        // Box paint (stroke only)
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = strokeWidth

        // Forward text changes
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == digits) onOtpFilled?.invoke(s.toString())
                invalidate()
            }
        })

        // When view is attached, request focus + show keyboard
        doOnAttach {
            requestFocus()
            post { showSoftKeyboard() }
        }
    }

    fun setOnOtpFilledListener(listener: (String) -> Unit) {
        onOtpFilled = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = paddingLeft + paddingRight +
                (digits * boxSize + (digits - 1) * boxSpacing)
        val h = paddingTop + paddingBottom + boxSize
        setMeasuredDimension(w.toInt(), h.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        // Do NOT call super.onDraw – we draw everything ourselves
        val text = text ?: return

        for (i in 0 until digits) {
            val left = paddingLeft + i * (boxSize + boxSpacing)
            val top = paddingTop.toFloat()
            boxRect.set(left, top, left + boxSize, top + boxSize)

            // Border
            boxPaint.color = if (i == text.length) activeColor else inactiveColor
            canvas.drawRoundRect(boxRect, 8f.dp(), 8f.dp(), boxPaint)

            // Character
            if (i < text.length) {
                val ch = text[i].toString()
                val x = boxRect.centerX()
                val y = boxRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(ch, x, y, textPaint)
            }
        }
    }

    // Make sure the keyboard pops up
    private fun showSoftKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    // Allow backspace to clear last digit
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && text?.isNotEmpty() == true) {
            val newText = text?.dropLast(1)
            setText(newText)
            setSelection(newText?.length ?: 0)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // Helper extensions
    private fun Float.dp() = this * resources.displayMetrics.density
    private fun Float.sp() = this * resources.displayMetrics.scaledDensity
}