/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.responses

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

suspend fun <T> Flow<T>.collectHandler(
    onError: suspend (Throwable) -> Unit,
    onCollect: suspend (T) -> Unit
) {
    this
        .catch { exception -> onError(exception) }
        .collect { value -> onCollect(value) }
}