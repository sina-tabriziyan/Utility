/**
 * Created by ST on 2/11/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.dialog.btmsheet.confirm

import android.content.Context
import com.sina.library.data.enums.ConfirmButtonStyle
import com.sina.library.utility.R

object ConfirmationBtmSheetFactory {
    fun create(
        context: Context,
        message: String,
        positiveText: String? = null,
        negativeText: String? = null,
        style: ConfirmButtonStyle = ConfirmButtonStyle.NORMAL,
        buttonColor: Int,
        onConfirm: () -> Unit
    ): ConfirmationBtmSheet {
        return ConfirmationBtmSheet(
            context = context,
            message = message,
            positiveText = positiveText ?: context.getString(R.string.confirm),
            negativeText = negativeText ?: context.getString(R.string.cancel),
            style = style,
            buttonColor = buttonColor,
            onConfirm = onConfirm
        )
    }
}
