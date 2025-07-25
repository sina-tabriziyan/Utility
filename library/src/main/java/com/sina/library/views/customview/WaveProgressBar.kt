package com.sina.library.views.customview


import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
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
import kotlin.math.min

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
    private val waveAmplitude1 = 20f
    private val waveFrequency1 = 0.05f
    private var waveOffset1: Float = 0f

    private val waveAmplitude2 = 12f
    private val waveFrequency2 = 0.03f
    private var waveOffset2: Float = 0f

    private val waveAmplitude3 = 8f
    private val waveFrequency3 = 0.08f
    private var waveOffset3: Float = 0f

    // Progress property (0-100)
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }

    // Gradient colors
    private var waveStartColor: Int = ContextCompat.getColor(context, android.R.color.holo_blue_light)
    private var waveMiddleColor: Int = ContextCompat.getColor(context, android.R.color.holo_green_light)
    private var waveEndColor: Int = ContextCompat.getColor(context, android.R.color.holo_orange_light)

    // Default glass colors
    private val defaultGlassOutlineColor: Int = "#4DFFFFFF".toColorInt()
    private val defaultReflectionColorStrong: Int = "#66FFFFFF".toColorInt()
    private val defaultReflectionColorWeak: Int = "#33FFFFFF".toColorInt()

    // Animators
    private val waveAnimator: ValueAnimator by lazy {
        ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            addUpdateListener {
                val animatedValue = it.animatedValue as Float
                waveOffset1 = animatedValue * 1f
                waveOffset2 = animatedValue * 2f
                waveOffset3 = animatedValue * 3f
                invalidate()
            }
            duration = 4000
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }
    private var progressAnimator: ValueAnimator? = null

    init {
        wavePaint.style = Paint.Style.FILL

        // Default text setup; actual color set in onDraw based on theme
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 48f * resources.displayMetrics.scaledDensity

        circleOutlinePaint.style = Paint.Style.STROKE
        circleOutlinePaint.strokeWidth = 2f * resources.displayMetrics.density
        circleOutlinePaint.color = defaultGlassOutlineColor

        reflectionPaintStrong.style = Paint.Style.FILL
        reflectionPaintStrong.color = defaultReflectionColorStrong
        reflectionPaintWeak.style = Paint.Style.FILL
        reflectionPaintWeak.color = defaultReflectionColorWeak

        // Read XML attrs
        val ta = context.obtainStyledAttributes(attrs, R.styleable.WaveProgressBar, defStyleAttr, 0)
        try {
            progress = ta.getFloat(R.styleable.WaveProgressBar_progress, progress)
            waveStartColor = ta.getColor(R.styleable.WaveProgressBar_waveStartColor, waveStartColor)
            waveMiddleColor = ta.getColor(R.styleable.WaveProgressBar_waveMiddleColor, waveMiddleColor)
            waveEndColor = ta.getColor(R.styleable.WaveProgressBar_waveEndColor, waveEndColor)
            circleOutlinePaint.color = ta.getColor(R.styleable.WaveProgressBar_glassOutlineColor, defaultGlassOutlineColor)
            reflectionPaintStrong.color = ta.getColor(R.styleable.WaveProgressBar_glassReflectionColorStrong, defaultReflectionColorStrong)
            reflectionPaintWeak.color = ta.getColor(R.styleable.WaveProgressBar_glassReflectionColorWeak, defaultReflectionColorWeak)
            textPaint.color = ta.getColor(R.styleable.WaveProgressBar_waveTextColor, textPaint.color)
            textPaint.textSize = ta.getDimension(R.styleable.WaveProgressBar_waveTextSize, textPaint.textSize)
        } finally {
            ta.recycle()
        }

        waveAnimator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 2f - 20f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Determine text color based on current UI mode
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        textPaint.color = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }

        // Calculate fill height
        val fillHeight = viewHeight - (progress / 100f) * viewHeight

        canvas.save()
        val clipPath = Path().apply {
            addCircle(centerX, centerY, radius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        // Wave gradient
        wavePaint.shader = LinearGradient(
            0f, viewHeight.toFloat(), viewWidth.toFloat(), 0f,
            intArrayOf(waveStartColor, waveMiddleColor, waveEndColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // Draw wave
        val wavePath = Path().apply {
            moveTo(0f, fillHeight)
            for (x in 0..viewWidth) {
                val y = fillHeight + (
                        kotlin.math.sin(x * waveFrequency1 + waveOffset1) * waveAmplitude1 +
                                kotlin.math.sin(x * waveFrequency2 + waveOffset2) * waveAmplitude2 +
                                kotlin.math.sin(x * waveFrequency3 + waveOffset3) * waveAmplitude3
                        ).toFloat()
                lineTo(x.toFloat(), y)
            }
            lineTo(viewWidth.toFloat(), viewHeight.toFloat())
            lineTo(0f, viewHeight.toFloat())
            close()
        }
        canvas.drawPath(wavePath, wavePaint)
        canvas.restore()

        // Glass outline and reflections
        canvas.drawCircle(centerX, centerY, radius, circleOutlinePaint)
        canvas.drawCircle(centerX - radius * 0.4f, centerY - radius * 0.4f, radius * 0.2f, reflectionPaintStrong)
        canvas.drawCircle(centerX + radius * 0.3f, centerY - radius * 0.5f, radius * 0.1f, reflectionPaintWeak)

        // Draw progress text
        canvas.drawText("${"%.0f".format(progress)}%", centerX, centerY - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    }

    fun animateProgress(targetProgress: Float, durationMs: Long? = null) {
        progressAnimator?.cancel()
        val dur = durationMs ?: (targetProgress * 1000L).toLong()
        progressAnimator = ValueAnimator.ofFloat(progress, targetProgress.coerceIn(0f, 100f)).apply {
            addUpdateListener { ani -> progress = ani.animatedValue as Float }
            duration = dur
            interpolator = LinearInterpolator()
            start()
        }
    }

    fun resetProgress(duration: Long = 500) {
        animateProgress(0f, duration)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator.cancel()
        progressAnimator?.cancel()
    }
}
