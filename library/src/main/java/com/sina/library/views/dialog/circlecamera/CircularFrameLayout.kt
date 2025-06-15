/**
 * Created by ST on 2/1/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout

class CircularFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val clipPath = Path()
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE // Change this to your desired progress color
        strokeWidth = 5f // Thickness of the progress bar
        style = Paint.Style.STROKE
    }
    private var progress = 0 // Current progress
    private var maxProgress = 100 // Maximum progress

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = w.coerceAtMost(h) // Take the smaller dimension for a perfect circle
        radius = (size - progressPaint.strokeWidth) / 2f
        centerX = w / 2f
        centerY = h / 2f

        clipPath.reset()
        clipPath.addCircle(centerX, centerY, radius, Path.Direction.CW)
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(clipPath) // Clip the content to a circular path
        super.dispatchDraw(canvas)
        canvas.restore()

        // Draw circular progress on top of the clipped content
        drawCircularProgress(canvas)
    }

    private fun drawCircularProgress(canvas: Canvas) {
        // Calculate the sweep angle based on progress
        val sweepAngle = (360f * progress / maxProgress)

        // Draw the progress arc
        canvas.drawArc(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            -90f, // Start from the top
            sweepAngle, // Sweep angle based on progress
            false,
            progressPaint
        )
    }

    // Public method to animate progress change
    fun setProgressAnimated(newProgress: Int, duration: Long = 300L) {
        val animator = ValueAnimator.ofInt(progress, newProgress.coerceIn(0, maxProgress))
        animator.duration = duration
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            progress = animation.animatedValue as Int
            invalidate() // Redraw the view
        }
        animator.start()
    }

    // Public method to set maximum progress
    fun setMaxProgress(maxProgress: Int) {
        this.maxProgress = maxProgress
    }
}
