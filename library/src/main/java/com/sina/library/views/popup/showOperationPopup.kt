package com.sina.library.views.popup

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sina.library.R
import com.sina.library.views.customview.FontIcon
import com.sina.library.data.enums.OperationItem

fun <T> showOperationPopup(
    anchorView: View,
    operations: List<T>,
    onOperationSelected: (T) -> Unit
) where T : Enum<T>, T : OperationItem {
    val context = anchorView.context
    val inflater = LayoutInflater.from(context)
    val popupView = inflater.inflate(R.layout.popup_chat_operations, null)

    // Measure the popup view to determine its dimensions
    popupView.measure(
        View.MeasureSpec.UNSPECIFIED,
        View.MeasureSpec.UNSPECIFIED
    )
    val popupWidth = popupView.measuredWidth
    val popupHeight = popupView.measuredHeight

    // Get the screen dimensions
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    // Get the anchor view's location on screen
    val location = IntArray(2)
    anchorView.getLocationOnScreen(location)
    val anchorX = location[0]
    val anchorY = location[1]
    val anchorWidth = anchorView.width
    val anchorHeight = anchorView.height

    // Calculate the X position: center the popup horizontally relative to the anchor view
    var xPos = anchorX + (anchorWidth - popupWidth) / 2
    // Ensure the popup doesn't go off-screen horizontally
    xPos = xPos.coerceIn(0, screenWidth - popupWidth)

    // Calculate the Y position: show below the anchor view
    var yPos = anchorY + anchorHeight
    // If there's not enough space below, show above the anchor view
    if (yPos + popupHeight > screenHeight) {
        yPos = anchorY - popupHeight
        if (yPos < 0) {
            yPos = 0
        }
    }

    // Initialize the PopupWindow
    val popupWindow = PopupWindow(
        popupView,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true
    ).apply {
        isOutsideTouchable = true
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }

    // Set up the RecyclerView inside the popup
    val recyclerView = popupView.findViewById<RecyclerView>(R.id.operations_recycler_view)
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_operation, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val operation = operations[position]
            val iconView: FontIcon = holder.itemView.findViewById(R.id.operationIcon)
            val nameView: TextView = holder.itemView.findViewById(R.id.operation_name)

            iconView.setIcon(operation.icon)
            nameView.setText(operation.resNameId)
            holder.itemView.setOnClickListener {
                onOperationSelected(operation)
                popupWindow.dismiss()
            }
        }

        override fun getItemCount(): Int = operations.size
    }

    // Display the popup window at the calculated position
    popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xPos, yPos)
}