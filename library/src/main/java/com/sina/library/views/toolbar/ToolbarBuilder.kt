package com.sina.library.views.toolbar

import android.content.Context
import android.view.MenuInflater
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.sina.library.utility.R

class ToolbarBuilder(private val context: Context) {
    private var toolbar: Toolbar? = null
    private var menuResId: Int = 0
    private var toolbarTitle: String? = null
    private var drawableIcon: Int = 0
    private var textColor: Int = 0
    private var menuProvider: MenuProvider? = null
    private var language: String = "en"

    fun languageState(language: String?): ToolbarBuilder {
        if (language != null) this.language = language
        return this
    }

    fun withToolbar(toolbar: Toolbar): ToolbarBuilder {
        this.toolbar = toolbar
        return this
    }

    fun withMenu(menuResId: Int): ToolbarBuilder {
        this.menuResId = menuResId
        return this
    }

    fun withMenuProvider(menuProvider: MenuProvider?): ToolbarBuilder {
        this.menuProvider = menuProvider
        return this
    }

    fun setNavigationIcon(@DrawableRes drawableIcon: Int): ToolbarBuilder {
        this.drawableIcon = drawableIcon
        return this
    }

    fun setTitleTextColor(@ColorInt textColor: Int): ToolbarBuilder {
        this.textColor = textColor
        return this
    }

    fun setToolbarTitle(toolbarTitle: String): ToolbarBuilder {
        this.toolbarTitle = toolbarTitle
        return this
    }

    fun build(fragment: Fragment): Toolbar {
        val activity = fragment.requireActivity() as AppCompatActivity
        toolbar?.let { toolbar ->
            toolbar.apply {
                if (menuResId != 0) {
                    inflateMenu(menuResId)
                    setOnMenuItemClickListener { item -> menuProvider?.onMenuItemSelected(item) ?: false }
                }
                setNavigationIcon(
                    if (language == context.getString(R.string.txt_persian))
                        R.drawable.ic_right_arrow
                    else R.drawable.ic_left_arrow
                )
                setTitleTextColor(textColor)
            }
            activity.apply {
                setSupportActionBar(toolbar)
                supportActionBar?.apply {
                    this.title = toolbarTitle
                    setDisplayHomeAsUpEnabled(true)
                    setDisplayShowHomeEnabled(true)
                    setHomeAsUpIndicator(drawableIcon)
                }
            }
            menuProvider?.onCreateMenu(toolbar.menu, MenuInflater(context))
        }
        return toolbar!!
    }
}
