package com.sina.library.views.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

open class BaseVM<I : Any, S : Any>(initialState: S, ) : ViewModel(), KoinComponent {

    protected open val TAG: String get() = this::class.java.simpleName

    protected val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    protected val _intents: Channel<I> = Channel(Channel.BUFFERED)
    val intents: Flow<I> = _intents.receiveAsFlow()

    protected fun launchIo(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }

    protected fun launchMain(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) { block() }
    }

    fun <T> MutableStateFlow<T>.update(update: T.() -> T) {
        value = value.update()
    }

    protected fun findSidInHeaders(values: List<String>): String =
        values.firstOrNull { it.startsWith("SID=") }?.substringBefore(";") ?: ""
}
