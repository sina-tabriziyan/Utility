package com.sina.library.views.customview

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.TypedValue

class FontIconDrawable constructor(
    private var context: Context? = null,
    private var icon: String? = null,
    typeface: Typeface,
    hexColor: Int
) : Drawable() {

    val ANDROID_ACTIONBAR_ICON_SIZE_DP = 128


    var paint: TextPaint = TextPaint()

    init {
        paint.typeface = typeface
        paint.textAlign = Paint.Align.CENTER
        paint.color = hexColor
    }


    var size = -1

    private var alpha = 255


    fun actionBarSize(): FontIconDrawable? {
        return sizeDp(ANDROID_ACTIONBAR_ICON_SIZE_DP)
    }

    fun sizeRes(dimenRes: Int): FontIconDrawable? {
        return sizePx(context!!.resources.getDimensionPixelSize(dimenRes))
    }

    fun sizeDp(size: Int): FontIconDrawable? {
        return sizePx(dpToPx(context!!.resources, size))
    }

    fun dpToPx(res: Resources, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            res.displayMetrics
        ).toInt()
    }

    fun sizePx(size: Int): FontIconDrawable? {
        this.size = size
        setBounds(0, 0, size, size)
        invalidateSelf()
        return this
    }

    fun color(color: Int): FontIconDrawable? {
        paint!!.color = color
        invalidateSelf()
        return this
    }

    fun colorRes(colorRes: Int): FontIconDrawable? {
        paint!!.color = context!!.resources.getColor(colorRes)
        invalidateSelf()
        return this
    }

    fun alpha(alpha: Int): FontIconDrawable? {
        setAlpha(alpha)
        invalidateSelf()
        return this
    }

    override fun getIntrinsicHeight(): Int {
        return size
    }

    override fun getIntrinsicWidth(): Int {
        return size
    }

    override fun draw(canvas: Canvas) {
        paint!!.textSize = getBounds().height().toFloat()
        val textBounds = Rect()
        paint!!.getTextBounds(icon, 0, 1, textBounds)
        val textBottom: Float =
            (getBounds().height() - textBounds.height()) / 2f + textBounds.height() - textBounds.bottom
        canvas.drawText(icon!!, getBounds().width() / 2f, textBottom, paint!!)
    }

    override fun isStateful(): Boolean {
        return true
    }

    fun isEnabled(stateSet: IntArray): Boolean {
        for (state in stateSet) if (state == android.R.attr.state_enabled) return true
        return false
    }

    override fun setAlpha(alpha: Int) {
        this.alpha = alpha
        paint!!.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint!!.colorFilter = cf
    }

    override fun clearColorFilter() {
        paint!!.colorFilter = null
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }
}