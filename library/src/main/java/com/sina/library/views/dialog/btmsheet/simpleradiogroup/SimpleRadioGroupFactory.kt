/**
 * Created by ST on 2/11/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.dialog.btmsheet.simpleradiogroup

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding

object SimpleRadioGroupFactory {
    inline fun <reified T : Enum<T>, reified B : ViewBinding> create(
        context: Context,
        selectedItem: T? = null,
        noinline setup: (binding: B, dialog: Dialog, selectedItem: T) -> Unit
    ) {
        val bindingInflater: (LayoutInflater) -> B = { inflater ->
            B::class.java.getMethod("inflate", LayoutInflater::class.java)
                .invoke(null, inflater) as B
        }

        val dialog = SimpleRadioGroupDialog(
            bindingInflater = bindingInflater,  // ✅ Auto-Inflate ViewBinding
            enumValues = enumValues(),
            selectedItem = selectedItem,
            setup = setup
        )
        dialog.show(context) // ✅ Automatically determine fragment manager
    }
}

