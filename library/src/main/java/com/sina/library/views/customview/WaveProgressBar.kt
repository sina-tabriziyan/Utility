package com.sina.library.views.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.sina.library.utility.R

class WaveProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints for drawing
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reflectionPaintStrong = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reflectionPaintWeak = Paint(Paint.ANTI_ALIAS_FLAG)

    // View dimensions and properties
    private var viewWidth: Int = 0
    private var viewHeight: Int = 0
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radius: Float = 0f

    // Wave properties for multiple superimposed waves to create a more natural look
    private val waveAmplitude1 = 20f // Main wave amplitude
    private val waveFrequency1 = 0.05f // Main wave frequency
    private var waveOffset1: Float = 0f // Offset for first wave

    private val waveAmplitude2 = 12f // Smaller secondary wave amplitude
    private val waveFrequency2 = 0.03f // Slower secondary wave frequency
    private var waveOffset2: Float = 0f // Offset for second wave

    private val waveAmplitude3 = 8f // Even smaller tertiary wave amplitude
    private val waveFrequency3 = 0.08f // Faster tertiary wave frequency
    private var waveOffset3: Float = 0f // Offset for third wave

    // Progress property (0-100)
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f) // Ensure progress stays between 0 and 100
            invalidate() // Request a redraw when progress changes
        }

    // Gradient colors for the wave fill
    private var waveStartColor: Int = ContextCompat.getColor(context, android.R.color.holo_blue_light)
    private var waveMiddleColor: Int = ContextCompat.getColor(context, android.R.color.holo_green_light)
    private var waveEndColor: Int = ContextCompat.getColor(context, android.R.color.holo_orange_light)

    // Default glass colors (if not provided via XML attributes)
    private val defaultGlassOutlineColor: Int = "#4DFFFFFF".toColorInt() // 30% white
    private val defaultReflectionColorStrong: Int = "#66FFFFFF".toColorInt() // 40% white
    private val defaultReflectionColorWeak: Int = "#33FFFFFF".toColorInt() // 20% white

    // Animation for the wave itself (continuous floating motion)
    private val waveAnimator: ValueAnimator by lazy {
        // Animate a full 2*PI cycle for the base offset
        ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
            addUpdateListener {
                val animatedValue = it.animatedValue as Float
                // Update offsets for each wave layer.
                // Multiplying by integers ensures each wave completes a full cycle (or multiple cycles)
                // when the base animatedValue completes 2*PI, guaranteeing a seamless loop.
                waveOffset1 = animatedValue * 1f // Base speed
                waveOffset2 = animatedValue * 2f // Twice the speed
                waveOffset3 = animatedValue * 3f // Thrice the speed
                invalidate() // Redraw on each animation frame
            }
            duration = 4000 // Longer duration for more subtle continuous motion
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator() // Ensures smooth, constant speed
        }
    }

    // Animator for changing the progress value smoothly
    private var progressAnimator: ValueAnimator? = null

    init {
        // Initialize paints
        wavePaint.style = Paint.Style.FILL

        textPaint.color = ContextCompat.getColor(context, android.R.color.white) // Default text color
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 48f * resources.displayMetrics.scaledDensity // Scale text size

        circleOutlinePaint.style = Paint.Style.STROKE
        circleOutlinePaint.strokeWidth = 2f * resources.displayMetrics.density // Set stroke width to 2dp
        circleOutlinePaint.color = defaultGlassOutlineColor // Default glass outline

        reflectionPaintStrong.style = Paint.Style.FILL
        reflectionPaintStrong.color = defaultReflectionColorStrong // Default reflection color

        reflectionPaintWeak.style = Paint.Style.FILL
        reflectionPaintWeak.color = defaultReflectionColorWeak // Default reflection color

        // Read custom attributes from XML
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveProgressBar, defStyleAttr, 0)
        try {
            progress = typedArray.getFloat(R.styleable.WaveProgressBar_progress, 0f)
            waveStartColor = typedArray.getColor(R.styleable.WaveProgressBar_waveStartColor, waveStartColor)
            waveMiddleColor = typedArray.getColor(R.styleable.WaveProgressBar_waveMiddleColor, waveMiddleColor)
            waveEndColor = typedArray.getColor(R.styleable.WaveProgressBar_waveEndColor, waveEndColor)
            circleOutlinePaint.color = typedArray.getColor(R.styleable.WaveProgressBar_glassOutlineColor, defaultGlassOutlineColor)
            reflectionPaintStrong.color = typedArray.getColor(R.styleable.WaveProgressBar_glassReflectionColorStrong, defaultReflectionColorStrong)
            reflectionPaintWeak.color = typedArray.getColor(R.styleable.WaveProgressBar_glassReflectionColorWeak, defaultReflectionColorWeak)
            textPaint.color = typedArray.getColor(R.styleable.WaveProgressBar_waveTextColor, textPaint.color)
            textPaint.textSize = typedArray.getDimension(R.styleable.WaveProgressBar_waveTextSize, textPaint.textSize)
        } finally {
            typedArray.recycle() // Always recycle TypedArray
        }

        // Start the wave animation
        waveAnimator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        centerX = w / 2f
        centerY = h / 2f
        radius = Math.min(w, h) / 2f - 20f // Adjusted for padding/border
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate fill height based on progress
        val fillHeight = viewHeight - (progress / 100f) * viewHeight

        // Save canvas state before clipping
        canvas.save()

        // Create a circular clipping path
        val path = Path().apply {
            addCircle(centerX, centerY, radius, Path.Direction.CW)
        }
        canvas.clipPath(path)

        // Create linear gradient for the wave
        val gradient = LinearGradient(
            0f, viewHeight.toFloat(), viewWidth.toFloat(), 0f, // Diagonal gradient
            intArrayOf(waveStartColor, waveMiddleColor, waveEndColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        wavePaint.shader = gradient

        // Draw the wave
        val wavePath = Path()
        wavePath.moveTo(0f, fillHeight)
        for (x in 0..viewWidth step 1) {
            // Sum of multiple sine waves to create a more complex and "random" wave
            val y = fillHeight +
                    (Math.sin((x * waveFrequency1 + waveOffset1).toDouble()) * waveAmplitude1 +
                            Math.sin((x * waveFrequency2 + waveOffset2).toDouble()) * waveAmplitude2 +
                            Math.sin((x * waveFrequency3 + waveOffset3).toDouble()) * waveAmplitude3).toFloat()
            wavePath.lineTo(x.toFloat(), y)
        }
        wavePath.lineTo(viewWidth.toFloat(), viewHeight.toFloat())
        wavePath.lineTo(0f, viewHeight.toFloat())
        wavePath.close()
        canvas.drawPath(wavePath, wavePaint)

        // Restore canvas state (remove clipping)
        canvas.restore()

        // Draw the circle outline (glass effect)
        canvas.drawCircle(centerX, centerY, radius, circleOutlinePaint)

        // Inner light reflection (bulb effect)
        canvas.drawCircle(centerX - radius * 0.4f, centerY - radius * 0.4f, radius * 0.2f, reflectionPaintStrong)
        canvas.drawCircle(centerX + radius * 0.3f, centerY - radius * 0.5f, radius * 0.1f, reflectionPaintWeak)

        // Draw the percentage text
        canvas.drawText("${progress.toInt()}%", centerX, centerY - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    }

    /**
     * Animates the progress of the wave from the current value to a target value.
     * @param targetProgress The target progress value (0-100).
     * @param durationMs The duration of the animation in milliseconds. If null, duration will be calculated
     * as `targetProgress * 1000` (i.e., 1 second per unit of progress).
     */
    fun animateProgress(targetProgress: Float, durationMs: Long? = null) {
        progressAnimator?.cancel() // Cancel any ongoing progress animation

        // Calculate duration: if durationMs is provided, use it. Otherwise, use targetProgress * 1000ms.
        val actualDuration = durationMs ?: (targetProgress * 1000L).toLong()

        progressAnimator = ValueAnimator.ofFloat(this.progress, targetProgress.coerceIn(0f, 100f)).apply {
            addUpdateListener { animator ->
                this@WaveProgressBar.progress = animator.animatedValue as Float
            }
            this.duration = actualDuration
            interpolator = LinearInterpolator() // Smooth linear transition
            start()
        }
    }

    /**
     * Resets the progress to 0 and animates it.
     * @param duration The duration of the animation in milliseconds.
     */
    fun resetProgress(duration: Long = 500) {
        animateProgress(0f, duration)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator.cancel() // Stop continuous wave animation
        progressAnimator?.cancel() // Stop any ongoing progress animation
    }
}