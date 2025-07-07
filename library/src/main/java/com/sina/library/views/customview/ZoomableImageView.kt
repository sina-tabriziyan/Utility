package com.sina.library.views.customview

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ZoomableImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {

    private var matrix = Matrix()
    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isDragging = false
    private var scaleGestureDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    init {
        scaleType = ScaleType.MATRIX
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val pointerIndex = event.actionIndex
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.getX(pointerIndex)
                lastTouchY = event.getY(pointerIndex)
                activePointerId = event.getPointerId(0)
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    if (scaleFactor > 1.0f) {  // Only allow dragging when zoomed in
                        matrix.postTranslate(dx, dy)
                        imageMatrix = matrix
                        isDragging = true
                        disableBottomSheetDrag()
                    }
                    lastTouchX = x
                    lastTouchY = y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                enableBottomSheetDrag()
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(1.0f, 5.0f) // Restrict zoom between 1x and 5x

            matrix.setScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
            imageMatrix = matrix

            if (scaleFactor > 1.0f) {
                disableBottomSheetDrag()
            } else {
                enableBottomSheetDrag()
            }
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            scaleFactor = if (scaleFactor > 1.0f) 1.0f else 2.0f
            matrix.setScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
            imageMatrix = matrix
            if (scaleFactor > 1.0f) disableBottomSheetDrag() else enableBottomSheetDrag()
            return true
        }
    }

    fun setBottomSheetBehavior(behavior: BottomSheetBehavior<*>) {
        bottomSheetBehavior = behavior
    }

    private fun disableBottomSheetDrag() {
        bottomSheetBehavior?.isDraggable = false
    }

    private fun enableBottomSheetDrag() {
        if (!isDragging) bottomSheetBehavior?.isDraggable = true
    }
}
