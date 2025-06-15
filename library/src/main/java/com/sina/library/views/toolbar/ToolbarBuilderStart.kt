/**
 * Created by ST on 6/11/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.toolbar

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
// Step 1: Initial builder interface
interface IToolbarBuilderStart {
    fun setTitle(title: String): IToolbarBuilderStart
    fun setSubtitle(subtitle: String): IToolbarBuilderStart
    fun setMenu(menuRes: Int): ToolbarBuilderAfterMenu
    fun setSearchViewId(searchViewId: Int): IToolbarBuilderStart
}

// Step 2: Builder interface after menu is set
interface ToolbarBuilderAfterMenu {
    fun setSearchQueryListener(listener: (String) -> Unit): ToolbarBuilderAfterMenu
    fun setMenuItemClickListener(itemId: Int, listener: (MenuItem) -> Boolean): ToolbarBuilderAfterMenu
    fun build(): MenuProvider
}

// Implementation of the builder
class ToolbarBuilderStart public constructor(
    private val context: Context,
    private val toolbar: Toolbar
) : IToolbarBuilderStart, ToolbarBuilderAfterMenu {

    private var title: String? = null
    private var subtitle: String? = null
    private var menuRes: Int? = null
    private var searchViewId: Int? = null
    private var searchQueryListener: ((String) -> Unit)? = null
    private val itemClickListeners: MutableMap<Int, (MenuItem) -> Boolean> = mutableMapOf()
    private var isMenuSet = false

    companion object {
        fun with(context: Context, toolbar: Toolbar): IToolbarBuilderStart {
            return ToolbarBuilderStart(context, toolbar)
        }
    }

    override fun setTitle(title: String): IToolbarBuilderStart = apply {
        this.title = title
    }

    override fun setSubtitle(subtitle: String): IToolbarBuilderStart = apply {
        this.subtitle = subtitle
    }

    override fun setMenu(menuRes: Int): ToolbarBuilderAfterMenu = apply {
        this.menuRes = menuRes
        isMenuSet = true

        toolbar.menu.clear()
        toolbar.inflateMenu(menuRes)

        toolbar.title = title
        toolbar.subtitle = subtitle
    }

    override fun setSearchViewId(searchViewId: Int): IToolbarBuilderStart = apply {
        this.searchViewId = searchViewId
    }

    override fun setSearchQueryListener(listener: (String) -> Unit): ToolbarBuilderAfterMenu = apply {
        if (!isMenuSet) {
            throw IllegalStateException("Menu must be set before setting the search query listener.")
        }
        if (searchViewId == null) {
            throw IllegalStateException("SearchView ID must be set before setting the search query listener.")
        }

        val searchView = toolbar.menu.findItem(searchViewId!!)?.actionView as? SearchView
        if (searchView != null) {
            searchQueryListener = listener
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { listener(it) }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { listener(it) }
                    return true
                }
            })
        } else {
            throw IllegalStateException("SearchView is not present in the menu. Ensure that the menu contains an item with id 'action_search' and an actionViewClass of SearchView.")
        }
    }

    override fun setMenuItemClickListener(itemId: Int, listener: (MenuItem) -> Boolean): ToolbarBuilderAfterMenu = apply {
        if (!isMenuSet) {
            throw IllegalStateException("Menu must be set before adding item click listeners.")
        }

        itemClickListeners[itemId] = listener
        toolbar.menu.findItem(itemId)?.setOnMenuItemClickListener(listener)
    }

    override fun build(): MenuProvider {
        if (!isMenuSet) {
            throw IllegalStateException("You must call setMenu() before build().")
        }

        return object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(menuRes!!, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return itemClickListeners[menuItem.itemId]?.invoke(menuItem) ?: false
            }
        }
    }
}