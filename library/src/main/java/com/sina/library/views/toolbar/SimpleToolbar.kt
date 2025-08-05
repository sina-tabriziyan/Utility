/**
 * Created by ST on 8/5/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.toolbar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.SearchView
import com.google.android.material.appbar.MaterialToolbar

class SimpleToolbar(
    private val context: Context,
    private val toolbar: MaterialToolbar
) {

    private val layoutViewsMap: MutableMap<Int, View> = mutableMapOf()
    private var currentLayout: View? = null

    fun addLayout(layoutResId: Int): SimpleToolbar {
        val layoutView = LayoutInflater.from(context).inflate(layoutResId, toolbar, false)
        layoutViewsMap[layoutResId] = layoutView
        return this
    }


    fun activate(layoutResId: Int): SimpleToolbar {
        toolbar.removeAllViews() // Clear previous views
        currentLayout = layoutViewsMap[layoutResId]
        currentLayout?.let {
            toolbar.addView(it)
        }
        return this
    }

    fun <T : View> getView(@IdRes viewId: Int): T? {
        return currentLayout?.findViewById(viewId)
    }


    fun setOnClickListener(@IdRes viewId: Int, listener: (View) -> Unit): SimpleToolbar {
        getView<View>(viewId)?.setOnClickListener { view ->
            listener(view)
        }
        return this
    }


    fun setSearchViewListeners(
        @IdRes searchViewId: Int,
        onQueryTextSubmit: (String) -> Unit = {},
        onQueryTextChange: (String) -> Unit = {},
        onClose: () -> Boolean = { false }
    ): SimpleToolbar {
        val searchView = getView<SearchView>(searchViewId)

        searchView?.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    onQueryTextSubmit(query ?: "")
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    onQueryTextChange(newText ?: "")
                    return true
                }
            })
            setOnCloseListener {
                onClose()
                true
            }
        }

        return this
    }
}
