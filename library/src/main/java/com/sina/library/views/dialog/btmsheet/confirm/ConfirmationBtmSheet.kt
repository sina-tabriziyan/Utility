/**
 * Created by st on 2/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.dialog.btmsheet.confirm

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sina.library.R


class ConfirmationBtmSheet(
    context: Context,
    private val message: String,
    positiveText: String?,
    negativeText: String?,
    private val style: ConfirmButtonStyle = ConfirmButtonStyle.NORMAL,
    private val buttonColor: Int,
    private val onConfirm: () -> Unit
) : BottomSheetDialog(context) {

    private val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_confirmation, null)

    private val txtMessage: TextView = view.findViewById(R.id.txtMessage)
    private val btnPositive: Button = view.findViewById(R.id.btnPositive)
    private val btnNegative: Button = view.findViewById(R.id.btnNegative)

    init {
        setContentView(view)

        txtMessage.text = message
        btnPositive.text = positiveText ?: context.getString(R.string.confirm)
        btnNegative.text = negativeText ?: context.getString(R.string.cancel)

        applyButtonStyles()

        btnPositive.setOnClickListener {
            onConfirm.invoke()
            dismiss()
        }

        btnNegative.setOnClickListener { dismiss() }
    }

    private fun applyButtonStyles() {
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(buttonColor)
            cornerRadius = if (style == ConfirmButtonStyle.ROUNDED) 50f else 8f
        }

        btnPositive.background = backgroundDrawable
        btnNegative.background = backgroundDrawable
    }
}
