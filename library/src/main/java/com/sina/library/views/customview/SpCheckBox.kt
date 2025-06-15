/**
 * Created by ST on 5/11/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.customview

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.sina.library.utility.R


class SpCheckBox : View {
    private var tickDrawable: Drawable? = null
    private var isChecked = false
    private var circleColor = Color.GRAY
    private var checkmarkColor = Color.WHITE
    private var animationDuration = 300L

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f // Adjust as needed
        color = circleColor
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = circleColor
    }

    private val checkmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f // Adjust as needed
        color = checkmarkColor
    }

    private val path = Path()
    private val circleBounds = RectF()

    private var checkmarkProgress = 0f
    private var circleScaleProgress = 1f

    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        tickDrawable = ContextCompat.getDrawable(context, R.drawable.ic_check)
        tickDrawable?.setBounds(0, 0, tickDrawable!!.intrinsicWidth, tickDrawable!!.intrinsicHeight)
        attrs?.let {
            val typedArray =
                context.obtainStyledAttributes(it, R.styleable.CircularCheckbox)
            isChecked = typedArray.getBoolean(R.styleable.CircularCheckbox_isChecked, false)
            circleColor = typedArray.getColor(R.styleable.CircularCheckbox_circleColor, Color.GRAY)
            checkmarkColor =
                typedArray.getColor(R.styleable.CircularCheckbox_checkmarkColor, Color.WHITE)
            animationDuration =
                typedArray.getInt(R.styleable.CircularCheckbox_animationDuration, 300).toLong()

            circlePaint.color = circleColor
            fillPaint.color = circleColor
            checkmarkPaint.color = checkmarkColor
            typedArray.recycle()

            if (isChecked) {
                checkmarkProgress = 1f
                circleScaleProgress = 0.8f
            }
        }

        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = Math.min(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val centerX = w / 2f
        val centerY = h / 2f
        val radius = Math.min(centerX, centerY) - circlePaint.strokeWidth / 2f
        circleBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Define the checkmark path so it extends slightly beyond the circle
        val checkmarkStartX = centerX - radius * 0.45f // Adjusted start to go further left
        val checkmarkStartY = centerY - radius * 0.25f // Adjusted start to go higher
        val checkmarkMidX = centerX - radius * 0.05f
        val checkmarkMidY = centerY + radius * 0.45f // Adjusted middle to go lower
        val checkmarkEndX = centerX + radius * 0.6f // Adjusted end to go further right
        val checkmarkEndY = centerY - radius * 0.3f // Adjusted end to go higher

        path.reset()
        path.moveTo(checkmarkStartX, checkmarkStartY)
        path.lineTo(checkmarkMidX, checkmarkMidY)
        path.lineTo(checkmarkEndX, checkmarkEndY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) / 2f) * circleScaleProgress

        // Draw the circle
        canvas.drawCircle(centerX, centerY, radius, circlePaint)


        // Draw the custom tick only if checked
        if (isChecked && tickDrawable != null) {
            // Calculate position and size
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (Math.min(width, height) / 2f) * circleScaleProgress

            val tickWidth = radius * 1.2f
            val tickHeight = radius * 1.2f

            val left = (centerX - tickWidth / 2).toInt()
            val top = (centerY - tickHeight / 2).toInt()

            tickDrawable!!.setBounds(left, top, left + tickWidth.toInt(), top + tickHeight.toInt())
            tickDrawable!!.draw(canvas)
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            return true // Consume the touch
        } else if (event.action == MotionEvent.ACTION_UP) {
            toggle()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun toggle() {
        isChecked = !isChecked
        tickDrawable?.alpha = 0
        val animator = ObjectAnimator.ofInt(tickDrawable, "alpha", 0, 255)
        animator.duration = animationDuration
        animator.start()
        val checkmarkAnimator =
            ValueAnimator.ofFloat(if (isChecked) 0f else 1f, if (isChecked) 1f else 0f).apply {
                duration = animationDuration
                addUpdateListener { valueAnimator ->
                    checkmarkProgress = valueAnimator.animatedValue as Float
                    invalidate()
                }
            }

        val scaleAnimator = ValueAnimator.ofFloat(1f, 0.8f, 1f).apply {
            duration = animationDuration / 2
            addUpdateListener { valueAnimator ->
                circleScaleProgress = valueAnimator.animatedValue as Float
                invalidate()
            }
            interpolator = OvershootInterpolator()
        }

        val animatorSet = AnimatorSet()
        if (isChecked) {
            animatorSet.playTogether(scaleAnimator, checkmarkAnimator)
        } else {
            animatorSet.playTogether(checkmarkAnimator, scaleAnimator)
        }
        animatorSet.start()

        onCheckedChangeListener?.invoke(isChecked)
    }

    // Public methods to control the checkbox state and appearance
    fun isChecked(): Boolean {
        return isChecked
    }

    fun setChecked(checked: Boolean, animated: Boolean = false) {
        if (isChecked != checked) {
            isChecked = checked
            if (animated) {
                toggle() // Reuse the animation logic
            } else {
                checkmarkProgress = if (isChecked) 1f else 0f
                circleScaleProgress = if (isChecked) 0.8f else 1f
                invalidate()
            }
        }
    }

    fun setCircleColor(color: Int) {
        circleColor = color
        circlePaint.color = color
        fillPaint.color = color
        invalidate()
    }

    fun setCheckmarkColor(color: Int) {
        checkmarkColor = color
        checkmarkPaint.color = color
        invalidate()
    }

    fun setAnimationDuration(duration: Long) {
        animationDuration = duration
    }

    fun setOnCheckedChangeListener(listener: ((Boolean) -> Unit)?) {
        onCheckedChangeListener = listener
    }

}