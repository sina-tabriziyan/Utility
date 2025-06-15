package com.sina.library.views.customview

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * A generic ItemTouchHelper.SimpleCallback for swipe-to-reply functionality,
 * designed to work with any ListAdapter.
 *
 * @param T The type of the items in the adapter's list.
 * @param VH The type of the ViewHolder used by the adapter.
 * @property adapter The ListAdapter instance.
 * @property getItemSwipeDirections A function that determines the allowed swipe directions
 *                                  for a given item of type T.
 * @property onSwipeDetected A lambda invoked when a swipe is completed.
 */
class ItemTouchHelperSwipeCallback<T, VH : RecyclerView.ViewHolder>(
    private val adapter: ListAdapter<T, VH>,
    private val getItemSwipeDirections: (item: T) -> Int,
    private val onSwipeDetected: (position: Int, direction: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) { // Default to L/R, getSwipeDirs will specify

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false // No move support

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.absoluteAdapterPosition
        if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
            onSwipeDetected(position, direction)
            adapter.notifyItemChanged(position) // Reset item view
        }
    }

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val position = viewHolder.absoluteAdapterPosition
        return if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
            val item = adapter.currentList[position]
            getItemSwipeDirections(item) // Delegate direction decision
        } else {
            0 // Disable swipe for invalid positions
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val currentSwipeDirs = getSwipeDirs(recyclerView, viewHolder)
            val maxSwipeDistance = viewHolder.itemView.width * 0.3f
            var clampedDx = dX

            if (currentSwipeDirs == ItemTouchHelper.LEFT) {
                clampedDx = dX.coerceAtMost(0f).coerceAtLeast(-maxSwipeDistance)
            } else if (currentSwipeDirs == ItemTouchHelper.RIGHT) {
                clampedDx = dX.coerceAtLeast(0f).coerceAtMost(maxSwipeDistance)
            } else if (currentSwipeDirs == (ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)) {
                // Allow both directions up to the threshold
                clampedDx = dX.coerceIn(-maxSwipeDistance, maxSwipeDistance)
            }
            else {
                clampedDx = 0f // No swipe allowed or invalid direction, don't move
            }

            super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive)
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.translationX = 0f
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.5f // 50% drag to trigger swipe
    }
}