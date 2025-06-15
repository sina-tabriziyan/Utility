package com.sina.library.views.base

import android.content.Context
import android.content.ContextWrapper
import java.util.Locale

class ContextUtils(base: Context) : ContextWrapper(base) {
    companion object {
        fun updateLocale(context: Context, locale: Locale): ContextWrapper {
            val configuration = context.resources.configuration
            configuration.setLocale(locale)
            return ContextUtils(context.createConfigurationContext(configuration))
        }
    }
}
