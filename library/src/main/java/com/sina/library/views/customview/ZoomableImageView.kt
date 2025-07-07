package com.sina.library.views.customview

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.bottomsheet.BottomSheetBehavior


class ZoomableImageView(context: Context, attrs: AttributeSet?) : // Renamed for clarity
    AppCompatImageView(context, attrs) {

    private var imageDisplayMatrix = Matrix()
    private var currentScaleFactor = 1.0f // Represents the scale relative to the original drawable size
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isDragging = false
    private var scaleGestureDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    private val drawableRect = RectF()
    private val viewRect = RectF()

    // Minimum and maximum zoom relative to the base "fit" scale
    // e.g. minZoomFactor = 1.0f means you can't zoom out further than what fits the screen.
    // e.g. maxZoomFactor = 3.0f means you can zoom in up to 3x the size that fits the screen.
    private val minZoomFactorRelativeToFit = 1.0f
    private val maxZoomFactorRelativeToFit = 5.0f


    init {
        super.setScaleType(ScaleType.MATRIX)
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (width > 0 && height > 0) {
            configureInitialImageMatrix()
        } else {
            // If view is not yet laid out, mark that matrix needs update
            // onSizeChanged will then call configureInitialImageMatrix
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewRect.set(0f, 0f, w.toFloat(), h.toFloat())
        configureInitialImageMatrix()
    }

    private fun configureInitialImageMatrix() {
        drawable ?: return
        if (viewRect.isEmpty) { // View not laid out yet
            if (width > 0 && height > 0) viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
            else return
        }
        if (drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return

        drawableRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())

        imageDisplayMatrix.reset()
        // Set matrix to scale drawable to fit view, centered
        imageDisplayMatrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
        currentScaleFactor = getCurrentActualMatrixScale() // Initialize currentScaleFactor to this fit scale
        imageMatrix = imageDisplayMatrix
        fixTranslations() // This will ensure it's perfectly centered if aspect ratios differ
    }

    private fun fixTranslations() {
        drawable ?: return
        if (viewRect.isEmpty) return

        val matrixValues = FloatArray(9)
        imageDisplayMatrix.getValues(matrixValues)

        val currentX = matrixValues[Matrix.MTRANS_X]
        val currentY = matrixValues[Matrix.MTRANS_Y]
        val currentActualScale = matrixValues[Matrix.MSCALE_X] // Assuming uniform scaling

        if (currentActualScale <= 0) return // Invalid scale

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val scaledImageWidth = drawableWidth * currentActualScale
        val scaledImageHeight = drawableHeight * currentActualScale

        var deltaX = 0f
        var deltaY = 0f

        // Check X-axis bounds
        if (scaledImageWidth > viewRect.width()) {
            // Image is wider than the view, so it can be panned.
            // Don't let the left edge of the image go past the left edge of the view.
            if (currentX > 0f) {
                deltaX = -currentX
            }
            // Don't let the right edge of the image go past the right edge of the view.
            else if (currentX + scaledImageWidth < viewRect.width()) {
                deltaX = viewRect.width() - (currentX + scaledImageWidth)
            }
        } else {
            // Image is narrower than the view, so center it.
            deltaX = (viewRect.width() - scaledImageWidth) / 2f - currentX
        }

        // Check Y-axis bounds
        if (scaledImageHeight > viewRect.height()) {
            // Image is taller than the view.
            if (currentY > 0f) {
                deltaY = -currentY
            } else if (currentY + scaledImageHeight < viewRect.height()) {
                deltaY = viewRect.height() - (currentY + scaledImageHeight)
            }
        } else {
            // Image is shorter than the view, so center it.
            deltaY = (viewRect.height() - scaledImageHeight) / 2f - currentY
        }

        if (deltaX != 0f || deltaY != 0f) {
            imageDisplayMatrix.postTranslate(deltaX, deltaY)
            imageMatrix = imageDisplayMatrix // Apply corrected matrix
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val currentEventX: Float
        val currentEventY: Float

        val activePointerCurrentIndex = event.findPointerIndex(activePointerId)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.getX() // For ACTION_DOWN, pointerIndex 0 is fine
                lastTouchY = event.getY()
                activePointerId = event.getPointerId(0)
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId != MotionEvent.INVALID_POINTER_ID && activePointerCurrentIndex != -1) {
                    currentEventX = event.getX(activePointerCurrentIndex)
                    currentEventY = event.getY(activePointerCurrentIndex)
                    val dx = currentEventX - lastTouchX
                    val dy = currentEventY - lastTouchY

                    if (isZoomedBeyondFitSlightly()) { // Only drag if zoomed in
                        imageDisplayMatrix.postTranslate(dx, dy)
                        fixTranslations() // Apply boundary checks after translation
                        imageMatrix = imageDisplayMatrix
                        isDragging = true
                        disableBottomSheetDrag()
                    }
                    lastTouchX = currentEventX
                    lastTouchY = currentEventY
                } else if (activePointerId != MotionEvent.INVALID_POINTER_ID && activePointerCurrentIndex == -1) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID // Active pointer left screen
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                if (!isDragging || !isZoomedBeyondFitSlightly()) {
                    enableBottomSheetDrag()
                }
                isDragging = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerIndex = event.actionIndex
                val upPointerId = event.getPointerId(upPointerIndex)
                if (upPointerId == activePointerId) {
                    val newPointerIndex = if (upPointerIndex == 0) 1 else 0
                    if (newPointerIndex < event.pointerCount) {
                        lastTouchX = event.getX(newPointerIndex)
                        lastTouchY = event.getY(newPointerIndex)
                        activePointerId = event.getPointerId(newPointerIndex)
                    } else {
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
            }
        }
        return true
    }

    private fun getBaseFitScale(): Float {
        drawable ?: return 1.0f
        if (viewRect.isEmpty || drawableRect.isEmpty || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return 1.0f

        val tempMatrix = Matrix()
        // Calculate the scale factor as if we were fitting the drawable into the view centered
        tempMatrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
        val values = FloatArray(9)
        tempMatrix.getValues(values)
        return values[Matrix.MSCALE_X] // Assuming uniform scale from FIT_CENTER
    }

    private fun getCurrentActualMatrixScale(): Float {
        val values = FloatArray(9)
        imageDisplayMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun isZoomedBeyondFitSlightly(): Boolean {
        // Use a small tolerance for floating point comparisons
        return getCurrentActualMatrixScale() > getBaseFitScale() + 0.001f
    }


    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isDragging = false
            return true // Indicate we are handling the scale gesture
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleChangeFactor = detector.scaleFactor
            val currentActualScale = getCurrentActualMatrixScale()
            val baseFit = getBaseFitScale()

            var targetActualScale = currentActualScale * scaleChangeFactor

            // Clamp the target scale based on min/max zoom factors relative to the fit scale
            val minAllowedActualScale = baseFit * minZoomFactorRelativeToFit
            val maxAllowedActualScale = baseFit * maxZoomFactorRelativeToFit

            targetActualScale = targetActualScale.coerceIn(minAllowedActualScale, maxAllowedActualScale)

            // Calculate the effective scale factor to apply to the current matrix
            val effectiveScaleFactor = targetActualScale / currentActualScale

            if (effectiveScaleFactor != 0f) { // Avoid division by zero or no-op
                currentScaleFactor = targetActualScale // Update our tracking variable
                imageDisplayMatrix.postScale(effectiveScaleFactor, effectiveScaleFactor, detector.focusX, detector.focusY)
                fixTranslations() // Apply boundary checks after scaling
                imageMatrix = imageDisplayMatrix
            }

            if (isZoomedBeyondFitSlightly()) {
                disableBottomSheetDrag()
            } else {
                enableBottomSheetDrag()
            }
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val baseFit = getBaseFitScale()
            val currentActualScale = getCurrentActualMatrixScale()
            val targetActualScale: Float

            // If already zoomed in (even slightly more than fit), zoom out to fit.
            // Otherwise, zoom in to a mid-point (e.g., half of max zoom) or maxZoom if that's smaller.
            if (isZoomedBeyondFitSlightly()) {
                targetActualScale = baseFit
            } else {
                val midZoomFactor = (minZoomFactorRelativeToFit + maxZoomFactorRelativeToFit) / 2f
                targetActualScale = (baseFit * midZoomFactor).coerceAtMost(baseFit * maxZoomFactorRelativeToFit)
            }

            val effectiveScaleFactor = targetActualScale / currentActualScale
            currentScaleFactor = targetActualScale // Update tracking variable

            imageDisplayMatrix.postScale(effectiveScaleFactor, effectiveScaleFactor, e.x, e.y)
            fixTranslations() // Apply boundary checks after double-tap scaling
            imageMatrix = imageDisplayMatrix

            if (isZoomedBeyondFitSlightly()) {
                disableBottomSheetDrag()
            } else {
                enableBottomSheetDrag()
            }
            return true
        }
    }

    fun setBottomSheetBehavior(behavior: BottomSheetBehavior<*>) {
        bottomSheetBehavior = behavior
        // Set initial draggable state
        if (isZoomedBeyondFitSlightly()) {
            disableBottomSheetDrag()
        } else {
            enableBottomSheetDrag()
        }
    }

    private fun disableBottomSheetDrag() {
        bottomSheetBehavior?.isDraggable = false
    }

    private fun enableBottomSheetDrag() {
        if (!isDragging && !isZoomedBeyondFitSlightly()) { // Check isDragging as well
            bottomSheetBehavior?.isDraggable = true
        }
    }
}
