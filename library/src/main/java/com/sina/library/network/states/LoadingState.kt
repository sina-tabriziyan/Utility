/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.states

sealed class LoadingState<out T> {
    data class Loading(val progress: Int) : LoadingState<Nothing>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error(val exception: Throwable? = null) : LoadingState<Nothing>()
}