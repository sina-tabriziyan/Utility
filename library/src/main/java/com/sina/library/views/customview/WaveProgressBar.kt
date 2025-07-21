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
import androidx.core.graphics.withSave
import com.sina.library.utility.R
import kotlin.math.sin

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

    // Wave properties
    private var waveOffset: Float = 0f // For wave animation
    private val waveAmplitude = 20f // Height of the wave
    private val waveFrequency = 0.05f // How many waves across the circle

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

    // Animation for the wave
    private val waveAnimator: ValueAnimator by lazy {
        ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
            addUpdateListener {
                waveOffset = it.animatedValue as Float
                invalidate() // Redraw on each animation frame
            }
            duration = 2000 // 2 seconds for one full wave cycle
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    init {
        // Initialize paints
        wavePaint.style = Paint.Style.FILL

        textPaint.color = ContextCompat.getColor(context, android.R.color.white) // Default text color
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 48f * resources.displayMetrics.scaledDensity // Scale text size

        circleOutlinePaint.style = Paint.Style.STROKE
        circleOutlinePaint.strokeWidth = 4f
        circleOutlinePaint.color = ContextCompat.getColor(context, android.R.color.white) // Default glass outline

        reflectionPaintStrong.style = Paint.Style.FILL
        reflectionPaintStrong.color = ContextCompat.getColor(context, android.R.color.white) // Default reflection color

        reflectionPaintWeak.style = Paint.Style.FILL
        reflectionPaintWeak.color = ContextCompat.getColor(context, android.R.color.white) // Default reflection color

        // Read custom attributes from XML
        // CORRECTED LINE HERE 👇
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveProgressBar, defStyleAttr, 0)
        try {
            // CORRECTED LINES HERE 👇
            progress = typedArray.getFloat(R.styleable.WaveProgressBar_progress, 0f)
            waveStartColor = typedArray.getColor(R.styleable.WaveProgressBar_waveStartColor, waveStartColor)
            waveMiddleColor = typedArray.getColor(R.styleable.WaveProgressBar_waveMiddleColor, waveMiddleColor)
            waveEndColor = typedArray.getColor(R.styleable.WaveProgressBar_waveEndColor, waveEndColor)
            circleOutlinePaint.color = typedArray.getColor(R.styleable.WaveProgressBar_glassOutlineColor, circleOutlinePaint.color)
            reflectionPaintStrong.color = typedArray.getColor(R.styleable.WaveProgressBar_glassReflectionColorStrong, reflectionPaintStrong.color)
            reflectionPaintWeak.color = typedArray.getColor(R.styleable.WaveProgressBar_glassReflectionColorWeak, reflectionPaintWeak.color)
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
        radius = w.coerceAtMost(h) / 2f - 20f // Adjusted for padding/border
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate fill height based on progress
        val fillHeight = viewHeight - (progress / 100f) * viewHeight

        // Save canvas state before clipping
        canvas.withSave {

            // Create a circular clipping path
            val path = Path().apply {
                addCircle(centerX, centerY, radius, Path.Direction.CW)
            }
            clipPath(path)

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
                val y =
                    fillHeight + sin((x * waveFrequency + waveOffset).toDouble()).toFloat() * waveAmplitude
                wavePath.lineTo(x.toFloat(), y)
            }
            wavePath.lineTo(viewWidth.toFloat(), viewHeight.toFloat())
            wavePath.lineTo(0f, viewHeight.toFloat())
            wavePath.close()
            drawPath(wavePath, wavePaint)

            // Restore canvas state (remove clipping)
        }

        // Draw the circle outline (glass effect)
        canvas.drawCircle(centerX, centerY, radius, circleOutlinePaint)

        // Inner light reflection (bulb effect)
        canvas.drawCircle(centerX - radius * 0.4f, centerY - radius * 0.4f, radius * 0.2f, reflectionPaintStrong)
        canvas.drawCircle(centerX + radius * 0.3f, centerY - radius * 0.5f, radius * 0.1f, reflectionPaintWeak)

        // Draw the percentage text
        canvas.drawText("${progress.toInt()}%", centerX, centerY - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator.cancel() // Stop animation when view is detached
    }
}