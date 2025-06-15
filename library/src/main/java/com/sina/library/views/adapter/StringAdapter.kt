package com.sina.library.views.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sina.library.utility.R
import com.sina.library.utility.databinding.ItemStringsBinding

class StringAdapter(private val click: (String) -> Unit) :
    ListAdapter<String, StringAdapter.Holder>(object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem
    }) {

    inner class Holder(private val binding: ItemStringsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) = with(binding) {
            root.setBackgroundColor(
                ContextCompat.getColor(
                    root.context,
                    if (bindingAdapterPosition % 2 == 0) R.color.white else R.color.transparent_gray_50
                )
            )
            tvDomain.text = item
            tvDomain.setOnClickListener { click(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemStringsBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

}
