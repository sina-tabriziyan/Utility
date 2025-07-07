package com.sina.library.views.customview

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatImageView
import androidx.media3.common.util.Log
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.abs


class ZoomableImageView(context: Context, attrs: AttributeSet?) : // Renamed for clarity
    AppCompatImageView(context, attrs) {

    private var imageDisplayMatrix = Matrix()
    private var currentScaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downTouchX = 0f // For tracking initial touch for drag detection
    private var downTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isImageBeingPanned = false // More specific than isDragging
    private var isImageBeingScaled = false

    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    private val drawableRect = RectF()
    private val viewRect = RectF()

    private val minZoomFactorRelativeToFit = 1.0f
    private val maxZoomFactorRelativeToFit = 5.0f
    private val touchSlop: Int

    companion object {
        private const val TAG = "ZoomableImageView"
    }

    init {
        super.setScaleType(ScaleType.MATRIX)
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        imageMatrix = imageDisplayMatrix // Initialize with identity or current
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        // Defer matrix configuration until view is laid out
        if (width > 0 && height > 0) {
            configureInitialImageMatrix()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            viewRect.set(0f, 0f, w.toFloat(), h.toFloat())
            configureInitialImageMatrix()
        }
    }

    private fun configureInitialImageMatrix() {
        drawable ?: return
        if (viewRect.isEmpty) {
            return
        }
        if (drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            imageDisplayMatrix.reset() // Reset to identity if drawable is invalid
            imageMatrix = imageDisplayMatrix
            return
        }

        drawableRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())

        imageDisplayMatrix.reset()
        imageDisplayMatrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
        currentScaleFactor = getCurrentActualMatrixScale()
        imageMatrix = imageDisplayMatrix
        fixTranslations() // This will ensure it's perfectly centered
        updateBottomSheetDraggableState() // Set initial state
    }

    private fun fixTranslations() {
        // ... (Keep the fixTranslations method exactly as in the previous good version)
        drawable ?: return
        if (viewRect.isEmpty) return

        val matrixValues = FloatArray(9)
        imageDisplayMatrix.getValues(matrixValues)

        val currentX = matrixValues[Matrix.MTRANS_X]
        val currentY = matrixValues[Matrix.MTRANS_Y]
        val currentActualScale = matrixValues[Matrix.MSCALE_X]

        if (currentActualScale <= 0) return

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        val scaledImageWidth = drawableWidth * currentActualScale
        val scaledImageHeight = drawableHeight * currentActualScale

        var deltaX = 0f
        var deltaY = 0f

        if (scaledImageWidth > viewRect.width()) {
            if (currentX > 0f) deltaX = -currentX
            else if (currentX + scaledImageWidth < viewRect.width()) deltaX = viewRect.width() - (currentX + scaledImageWidth)
        } else {
            deltaX = (viewRect.width() - scaledImageWidth) / 2f - currentX
        }

        if (scaledImageHeight > viewRect.height()) {
            if (currentY > 0f) deltaY = -currentY
            else if (currentY + scaledImageHeight < viewRect.height()) deltaY = viewRect.height() - (currentY + scaledImageHeight)
        } else {
            deltaY = (viewRect.height() - scaledImageHeight) / 2f - currentY
        }

        if (deltaX != 0f || deltaY != 0f) {
            imageDisplayMatrix.postTranslate(deltaX, deltaY)
            // No need to set imageMatrix here, it will be set by the caller after all matrix ops
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consumedByChildDetectors = scaleGestureDetector.onTouchEvent(event)
        if (!consumedByChildDetectors) {
            consumedByChildDetectors = gestureDetector.onTouchEvent(event)
        }
        isImageBeingScaled = scaleGestureDetector.isInProgress // Update scaling state

        val action = event.actionMasked
        val pointerIndex = event.actionIndex // Index of the pointer that performed the action
        val pointerId = event.getPointerId(pointerIndex) // ID of that pointer

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downTouchX = event.getX(0) // Use index 0 for initial down
                downTouchY = event.getY(0)
                lastTouchX = downTouchX
                lastTouchY = downTouchY
                activePointerId = event.getPointerId(0)
                isImageBeingPanned = false

                // Crucially, if the image is zoomable/pannable, immediately tell the BottomSheet NOT to drag.
                // The BottomSheet might still try to intercept if its touch processing happens first.
                // This is why parent.requestDisallowInterceptTouchEvent(true) is also important.
                if (isZoomedBeyondFitSlightly() || isImageBeingScaled) { // Check if we are starting a gesture that needs control
                    parent?.requestDisallowInterceptTouchEvent(true)
                    disableBottomSheetDrag()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val currentActivePointerIndex = event.findPointerIndex(activePointerId)
                if (activePointerId != MotionEvent.INVALID_POINTER_ID && currentActivePointerIndex != -1) {
                    val currentEventX = event.getX(currentActivePointerIndex)
                    val currentEventY = event.getY(currentActivePointerIndex)

                    if (!isImageBeingScaled) { // Do not pan if currently scaling
                        val deltaX = currentEventX - lastTouchX
                        val deltaY = currentEventY - lastTouchY

                        // Start panning only if touch has moved beyond slop and image is zoomed
                        if (!isImageBeingPanned && (abs(currentEventX - downTouchX) > touchSlop || abs(currentEventY - downTouchY) > touchSlop)) {
                            if (isZoomedBeyondFitSlightly()) {
                                isImageBeingPanned = true
                                // Once panning starts, ensure parent doesn't intercept
                                parent?.requestDisallowInterceptTouchEvent(true)
                                disableBottomSheetDrag()
                            }
                        }

                        if (isImageBeingPanned) {
                            imageDisplayMatrix.postTranslate(deltaX, deltaY)
                            // fixTranslations() will be called before setting imageMatrix
                        }
                    }
                    lastTouchX = currentEventX
                    lastTouchY = currentEventY
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                parent?.requestDisallowInterceptTouchEvent(false) // Allow parent to intercept again
                isImageBeingPanned = false
                isImageBeingScaled = false // Reset scaling flag from detector
                updateBottomSheetDraggableState()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (pointerId == activePointerId) { // If our active pointer went up
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerIndex < event.pointerCount) {
                        downTouchX = event.getX(newPointerIndex) // Reset down for new active pointer
                        downTouchY = event.getY(newPointerIndex)
                        lastTouchX = downTouchX
                        lastTouchY = downTouchY
                        activePointerId = event.getPointerId(newPointerIndex)
                    } else { // No other pointer left
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        parent?.requestDisallowInterceptTouchEvent(false)
                        isImageBeingPanned = false
                        updateBottomSheetDraggableState()
                    }
                }
            }
        }

        // Apply matrix changes and fix translations if needed
        if (action == MotionEvent.ACTION_MOVE && isImageBeingPanned && !isImageBeingScaled) {
            fixTranslations()
            imageMatrix = imageDisplayMatrix
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // Ensure final state is correct, fixTranslations might have been missed if no move
            fixTranslations()
            imageMatrix = imageDisplayMatrix
        }


        // Consume the event if we are panning, scaling, or if a child detector consumed it
        return isImageBeingPanned || isImageBeingScaled || consumedByChildDetectors || true // return true to indicate we want to continue receiving events
    }


    // ... (getBaseFitScale, getCurrentActualMatrixScale, isZoomedBeyondFitSlightly methods remain the same)
    private fun getBaseFitScale(): Float {
        drawable ?: return 1.0f
        if (viewRect.isEmpty || drawableRect.isEmpty || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return 1.0f
        val tempMatrix = Matrix()
        tempMatrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
        val values = FloatArray(9)
        tempMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun getCurrentActualMatrixScale(): Float {
        val values = FloatArray(9)
        imageDisplayMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun isZoomedBeyondFitSlightly(): Boolean {
        return getCurrentActualMatrixScale() > getBaseFitScale() + 0.001f
    }


    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isImageBeingScaled = true
            isImageBeingPanned = false // Stop panning when scaling begins
            parent?.requestDisallowInterceptTouchEvent(true)
            disableBottomSheetDrag()
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // ... (Scale logic remains the same as previous version with boundary checks)
            val scaleChangeFactor = detector.scaleFactor
            val currentActualScale = getCurrentActualMatrixScale()
            val baseFit = getBaseFitScale()
            var targetActualScale = currentActualScale * scaleChangeFactor
            val minAllowedActualScale = baseFit * minZoomFactorRelativeToFit
            val maxAllowedActualScale = baseFit * maxZoomFactorRelativeToFit
            targetActualScale = targetActualScale.coerceIn(minAllowedActualScale, maxAllowedActualScale)
            val effectiveScaleFactor = targetActualScale / currentActualScale

            if (effectiveScaleFactor != 0f && abs(effectiveScaleFactor - 1.0f) > 0.0001f) { // Check for meaningful scale change
                currentScaleFactor = targetActualScale
                imageDisplayMatrix.postScale(effectiveScaleFactor, effectiveScaleFactor, detector.focusX, detector.focusY)
                fixTranslations()
                imageMatrix = imageDisplayMatrix
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isImageBeingScaled = false
            // updateBottomSheetDraggableState will be called in ACTION_UP/CANCEL
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            parent?.requestDisallowInterceptTouchEvent(true) // Ensure we handle this
            disableBottomSheetDrag()
            // ... (Double tap logic remains the same)
            val baseFit = getBaseFitScale()
            val currentActualScale = getCurrentActualMatrixScale()
            val targetActualScale: Float
            if (isZoomedBeyondFitSlightly()) {
                targetActualScale = baseFit
            } else {
                val midZoomFactor = (minZoomFactorRelativeToFit + maxZoomFactorRelativeToFit) / 2f
                targetActualScale = (baseFit * midZoomFactor).coerceAtMost(baseFit * maxZoomFactorRelativeToFit)
            }
            val effectiveScaleFactor = targetActualScale / currentActualScale
            currentScaleFactor = targetActualScale
            imageDisplayMatrix.postScale(effectiveScaleFactor, effectiveScaleFactor, e.x, e.y)
            fixTranslations()
            imageMatrix = imageDisplayMatrix

            // No immediate ACTION_UP after double tap, so update state here too
            // However, onTouchEvent's ACTION_UP will also call updateBottomSheetDraggableState.
            // For safety, let's allow onTouchEvent's final UP to handle it.
            // updateBottomSheetDraggableState()
            return true
        }

        // It's good practice to override onDown if you override other gesture methods
        override fun onDown(e: MotionEvent): Boolean {
            return true // Required for gesture detector to work properly
        }
    }

    fun setBottomSheetBehavior(behavior: BottomSheetBehavior<*>) {
        bottomSheetBehavior = behavior
        updateBottomSheetDraggableState() // Set initial state based on current zoom
    }

    private fun disableBottomSheetDrag() {
        if (bottomSheetBehavior?.isDraggable == true) {
            // Log.d(TAG, "Disabling BottomSheet drag")
            bottomSheetBehavior?.isDraggable = false
        }
    }

    private fun enableBottomSheetDrag() {
        if (bottomSheetBehavior?.isDraggable == false) {
            // Log.d(TAG, "Enabling BottomSheet drag")
            bottomSheetBehavior?.isDraggable = true
        }
    }

    private fun updateBottomSheetDraggableState() {
        // Only enable bottom sheet dragging if the image is NOT zoomed/scaled
        // AND we are not currently in a pan or scale gesture.
        if (!isImageBeingPanned && !isImageBeingScaled && !isZoomedBeyondFitSlightly()) {
            enableBottomSheetDrag()
        } else {
            disableBottomSheetDrag()
        }
    }
}
