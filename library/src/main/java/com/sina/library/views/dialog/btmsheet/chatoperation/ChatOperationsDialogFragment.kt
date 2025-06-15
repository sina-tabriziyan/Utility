package com.sina.library.views.dialog.btmsheet.chatoperation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sina.library.data.enums.OperationItem
import com.sina.library.utility.R

class ChatOperationsDialogFragment<T>(private val operations: List<T>, private val onOperationSelected: (T) -> Unit) : DialogFragment() where T : Enum<T>, T : OperationItem {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_chat_operations)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.operations_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = OperationsAdapter(operations) { operation ->
            onOperationSelected(operation)
            dismiss()
        }

        return dialog
    }

    inner class OperationsAdapter(
        private val operations: List<T>,
        private val onClick: (T) -> Unit
    ) : RecyclerView.Adapter<OperationsAdapter.OperationViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_operation, parent, false)
            return OperationViewHolder(view)
        }

        override fun onBindViewHolder(holder: OperationViewHolder, position: Int) {
            val operation = operations[position]
            holder.bind(operation)
        }

        override fun getItemCount(): Int = operations.size

        inner class OperationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconView: TextView = itemView.findViewById(R.id.operationIcon)
            private val nameView: TextView = itemView.findViewById(R.id.operation_name)

            fun bind(operation: T) {
                iconView.text = operation.icon
                nameView.setText(operation.resNameId)
                itemView.setOnClickListener { onClick(operation) }
            }
        }
    }
}
