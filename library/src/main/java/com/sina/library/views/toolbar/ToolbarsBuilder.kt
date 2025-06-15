package com.sina.library.views.toolbar

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.MenuProvider
import com.google.android.material.appbar.MaterialToolbar
import com.sina.library.data.enums.ToolbarStates
import com.sina.library.data.sealed.ToolbarContent


class ToolbarsBuilder(
    private val context: Context,
    private val toolbar: MaterialToolbar
) : MenuProvider {

    private var title: String? = null
    private val contentMap: MutableMap<ToolbarStates, ToolbarContent> = mutableMapOf()
    private val menuMap: MutableMap<ToolbarStates, Int> = mutableMapOf()
    private val menuItemClickListeners: MutableMap<Int, (MenuItem) -> Boolean> = mutableMapOf()
    private var searchQueryListener: ((String) -> Unit)? = null
    private val activeCustomLayoutViews: MutableMap<Int, View> = mutableMapOf()
    private var currentActiveCustomLayout: View? = null // Reference to the root of the current custom layout

    var currentState: ToolbarStates = ToolbarStates.DefaultMode
    val viewClickListeners: MutableMap<Int, (View) -> Unit> = mutableMapOf()


    fun setToolbarState(state: ToolbarStates): ToolbarsBuilder {
        currentState = state
        return this
    }

    fun setTitle(title: String): ToolbarsBuilder {
        this.title = title
        return this
    }

    fun setMenus(vararg menus: Pair<ToolbarStates, Int>): ToolbarsBuilder {
        menuMap.clear()
        menuMap.putAll(menus)
        return this
    }

    fun setSearchQueryListener(listener: (String) -> Unit): ToolbarsBuilder {
        searchQueryListener = listener
        return this
    }

    fun setContents(vararg contents: Pair<ToolbarStates, ToolbarContent>): ToolbarsBuilder {
        contentMap.clear()
        contentMap.putAll(contents)
        return this
    }

    fun setMenuItemClickListener(itemId: Int, listener: (MenuItem) -> Boolean): ToolbarsBuilder {
        menuItemClickListeners[itemId] = listener
        return this
    }

    fun build(): MenuProvider {
        toolbar.title = title
        return this
    }
    /**
     * Retrieves a specific view from the currently active custom toolbar layout.
     * Returns null if the view ID is not found or no custom layout is active.
     */
    fun <T : View> getActiveCustomView(@IdRes viewId: Int): T? {
        // First, try the map (if we pre-populated it or manually added a view)
        activeCustomLayoutViews[viewId]?.let { return it as? T }

        // If not in the map, try finding it in the currentActiveCustomLayout
        return currentActiveCustomLayout?.findViewById<T>(viewId)?.also {
            activeCustomLayoutViews[viewId] = it // Cache it for next time
        }
    }

    /**
     * Allows manually adding a view reference to the cache,
     * useful if a view is found and configured outside activate().
     */
    fun addCustomViewReference(@IdRes viewId: Int, view: View) {
        activeCustomLayoutViews[viewId] = view
        if (currentActiveCustomLayout == null && view.parent is View) {
            // Attempt to set currentActiveCustomLayout if it's not set and view has a parent
            // This is a basic assumption and might need refinement based on your layout structure.
            var parentView = view.parent as View
            while(parentView.parent != toolbar && parentView.parent is View) {
                parentView = parentView.parent as View
            }
            if(parentView.parent == toolbar) {
                currentActiveCustomLayout = parentView
            }
        }
    }

    fun activate(state: ToolbarStates) : View? { // why give me error
        currentState = state
        toolbar.menu.clear()
        toolbar.removeAllViews()
        activeCustomLayoutViews.clear() // Clear references from the previous layout
        currentActiveCustomLayout = null // Reset the reference to the current custom layout
        var inflatedCustomView: View? = null // Variable to hold the inflated view

        when (val content = contentMap[state]) {
            is ToolbarContent.Menu -> toolbar.inflateMenu(content.menuRes)

            is ToolbarContent.CustomLayout -> {
                val customView = LayoutInflater.from(context).inflate(content.layoutRes, toolbar, false)
                toolbar.addView(customView)
                currentActiveCustomLayout = customView // Store the root of the new layout
                inflatedCustomView = customView
                viewClickListeners.forEach { (viewId, listener) ->
                    customView.findViewById<View>(viewId)?.let { foundView ->
                        activeCustomLayoutViews[viewId] = foundView // Store the view
                        foundView.setOnClickListener { view -> listener(view) }
                    }
                }
            }

            is ToolbarContent.CustomView -> {
                toolbar.addView(content.view)
                viewClickListeners.forEach { (id, listener) ->
                    content.view.findViewById<View>(id)?.setOnClickListener(listener)
                }
                inflatedCustomView = content.view
            }
            null -> {}
        }
        menuMap[state]?.let { toolbar.inflateMenu(it) }
        toolbar.title = title
        return inflatedCustomView // Return the inflated view (or null)
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // Inflate the menu based on the current toolbar state
        menuMap[currentState]?.let { menuRes ->
            menuInflater.inflate(menuRes, menu)
        }

        // Optionally, you can add custom menu items here if needed
        // menu.add(Menu.NONE, R.id.custom_item, Menu.NONE, "Custom Item")
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val listener = menuItemClickListeners[menuItem.itemId]
        return listener?.invoke(menuItem) ?: false
    }
}
