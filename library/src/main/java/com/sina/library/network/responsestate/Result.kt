package com.sina.library.network.responsestate


sealed interface Result<out D, out E : RootError> {
    data class Success<out D, out E : RootError>(val data: D) : Result<D, E>
    data class Error<out D, out E : RootError>(val error: E) : Result<D, E>
}


inline fun <D, E : RootError> Result<D, E>.getOrNull(): D? =
    when (this) {
        is Result.Success -> data
        is Result.Error -> null
    }

inline fun <D, E : RootError> Result<D, E>.getErrorOrNull(): E? =
    when (this) {
        is Result.Success -> null
        is Result.Error -> error
    }

inline fun <D, E : RootError> Result<D, E>.isSuccess() = this is Result.Success


suspend inline fun <D1, E1 : RootError, D2, E2 : RootError> Result<D1, E1>.flatMapIfSuccess(
    crossinline block: suspend (D1) -> Result<D2, E2>
): Result<D2, RootError> {
    return when (this) {
        is Result.Success -> block(this.data)
        is Result.Error -> Result.Error(this.error)
    }
}

inline fun <D, E : RootError, R> Result<D, E>.map(transform: (D) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(error)
    }

