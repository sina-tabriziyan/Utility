package com.sina.library.views.customview

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.sina.library.data.enums.Popup
import com.sina.library.utility.R

fun Context.showCustomPopupView(
    position: Popup = Popup.BOTTOM,
    backgroundColor: Int = Color.GRAY,
    textColor: Int = Color.YELLOW,
    confirmText: String = getString(R.string.confirm),
    cancelText: String = getString(R.string.cancel),
    confirm: () -> Unit,
    cancel: () -> Unit
) {
    val rootView =
        (this as? Activity)?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
            ?: return

    val inflater = LayoutInflater.from(this)
    val popupView = inflater.inflate(R.layout.view_inline_popup, rootView, false)

    val container = popupView.findViewById<LinearLayout>(R.id.popup_container)
    val tvMessage = popupView.findViewById<TextView>(R.id.tv_message)
    val tvCancel = popupView.findViewById<TextView>(R.id.tv_cancel)
    val tvConfirm = popupView.findViewById<TextView>(R.id.tv_confirm)

    container.setBackgroundColor(backgroundColor)
    tvMessage.setTextColor(textColor)
    tvCancel.setTextColor(textColor)
    tvConfirm.setTextColor(textColor)

    tvCancel.text = cancelText
    tvConfirm.text = confirmText

    tvCancel.setOnClickListener {
        cancel()
        rootView.removeView(popupView)
    }

    tvConfirm.setOnClickListener {
        confirm()
        rootView.removeView(popupView)
    }

    val layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = when (position) {
            Popup.TOP -> Gravity.TOP
            Popup.CENTER -> Gravity.CENTER
            Popup.BOTTOM -> Gravity.BOTTOM
        }
        topMargin = 50
    }

    rootView.addView(popupView, layoutParams)
}
