/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.responses

import com.sina.library.network.states.DownloadState
import com.sina.library.network.states.ResponseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform

fun <T> Flow<ResponseState<T>>.asDownloadState(): Flow<DownloadState<T>> =
    transform { responseState ->
        when (responseState) {
            is ResponseState.Success -> emit(DownloadState.Success(responseState.data))
            is ResponseState.Error -> emit(DownloadState.Error(responseState.exception))
        }
    }.onStart { emit(DownloadState.Loading) }
        .catch { e -> emit(DownloadState.Error(e)) }
