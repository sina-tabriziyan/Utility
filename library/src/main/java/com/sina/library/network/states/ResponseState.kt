/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.states

sealed class ResponseState<out T> {
    data class Success<T>(val data: T) : ResponseState<T>()
    data class Error(val exception: Throwable? = null) : ResponseState<Nothing>()

    override fun toString(): String = when (this) {
        is Error -> "Failure ${exception?.message}"
        is Success -> "Success $data"
    }
}