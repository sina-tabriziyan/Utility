/**
 * Created by ST on 5/7/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.customview

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sina.library.data.enums.AttachIcons
import com.sina.library.utility.R
import com.sina.library.views.extensions.ViewExtensions.setIconBuilder

class IconDialogBuilder(fragment: Fragment) {
    companion object {
        private const val DEFAULT_NUM_COLUMNS = 4
    }

    private val dialog: Dialog = Dialog(fragment.requireContext())
    private var icons: List<AttachIcons> = emptyList()
    private var itemClickListener: ((attachIcons: AttachIcons) -> Unit)? = null

    private var iconSize: Int = 0
    private var gravity: Int = Gravity.CENTER

    fun setIcons(icons: List<AttachIcons>, iconSize: Int = 0): IconDialogBuilder {
        this.icons = icons
        this.iconSize = iconSize
        return this
    }

    fun setOnItemClickListener(listener: (attachIcons: AttachIcons) -> Unit): IconDialogBuilder {
        this.itemClickListener = listener
        return this
    }

    fun setGravity(gravity: Int): IconDialogBuilder {
        this.gravity = gravity
        return this
    }

    fun build(): Dialog {
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_icon_picker)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setGravity(gravity)
                attributes?.windowAnimations = R.style.DialogAnimation
            }
        }
        val gridView = dialog.findViewById<GridView>(R.id.iconGridView)
        gridView.apply {
            numColumns = calculateNumColumns()
            adapter = IconAdapter(dialog.context, icons)
            setOnItemClickListener { _, _, position, _ ->
                itemClickListener?.invoke(icons[position])
                dialog.dismiss()
            }
        }
        return dialog
    }

    private fun calculateNumColumns(): Int {
        return if (iconSize > 0) Resources.getSystem().displayMetrics.widthPixels / iconSize
        else DEFAULT_NUM_COLUMNS
    }

    private inner class IconAdapter(
        private val context: Context,
        private val icons: List<AttachIcons>
    ) : BaseAdapter() {

        override fun getCount(): Int = icons.size
        override fun getItem(position: Int): AttachIcons = icons[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(context)
                    .inflate(R.layout.item_dialog_builder, parent, false)
                holder = ViewHolder(view)
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ViewHolder
            }

            val icon = icons[position]
            holder.apply {
                nameTextView.text = icon.iconName
                tvIconImage.apply {
                    setIconBuilder(icon.iconId, icon.iconColor)
                }
            }
            return view
        }

        private inner class ViewHolder(view: View) {
            val tvIconImage: TextView = view.findViewById(R.id.tvIconDialog)
            val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        }
    }
}
