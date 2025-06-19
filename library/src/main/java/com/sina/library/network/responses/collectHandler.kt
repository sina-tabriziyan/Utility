/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.responses

import androidx.activity.result.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Collects a Flow with simplified error handling and suspendable collector and error handler.
 *
 * @param onError Suspendable lambda to handle any Throwable exceptions caught during collection.
 * @param onCollect Suspendable lambda to process each emitted value from the Flow.
 */
suspend fun <T> Flow<T>.collectHandler(
    onError: suspend (Throwable) -> Unit,
    onCollect: suspend (T) -> Unit
) {
    // It's good practice to ensure this collectHandler is itself called from a CoroutineScope.
    // We can grab the current scope to launch the onError if needed.
    val currentScope = CoroutineScope(currentCoroutineContext())

    this
        .catch { exception ->
            // Launch a new coroutine within the current scope to call the suspendable onError
            currentScope.launch {
                onError(exception)
            }
        }
        .collect { value ->
            // This 'collect' block is already a suspend context
            onCollect(value)
        }
}