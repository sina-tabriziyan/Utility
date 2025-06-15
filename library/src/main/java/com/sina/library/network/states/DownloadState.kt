/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.states

sealed class DownloadState<out T> {
    data object Loading : DownloadState<Nothing>()
    data class Success<T>(val data: T) : DownloadState<T>()
    data class Error(val exception: Throwable? = null) : DownloadState<Nothing>()
}