package com.sina.library.network.responsestate

import com.sina.library.network.responsestate.asResultBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Dispatcher
import retrofit2.Response
import java.io.IOException


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

inline fun <reified T, E : RootError> Result<Unit, E>.mapTo(data: T): Result<T, E> {
    return when (this) {
        is Result.Success -> Result.Success(data)
        is Result.Error -> Result.Error(error)
    }
}

suspend inline fun <reified T> safeCall(
    crossinline apiCall: suspend () -> Response<T>
): Result<T, DataError.Network> {
    return try {
        val response = apiCall()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(body)
        } else
            Result.Error(DataError.Network.UNKNOWN)
    } catch (e: IOException) {
        Result.Error(DataError.Network.NO_INTERNET)
    }
}

inline fun <T, E : RootError> Result<ApiSuccess<T>, E>.asResultBody(): Result<T?, E> = when (this) {
    is Result.Error -> Result.Error(this.error)
    is Result.Success -> Result.Success(this.data.body)
}

inline fun <T, E : RootError> Result<ApiSuccess<T>, E>.asFullResponse(): Result<FullApiResponse<T>, E> = when (this) {
    is Result.Error -> Result.Error(this.error)
    is Result.Success -> Result.Success(
        FullApiResponse(
            body = this.data.body,
            code = this.data.code,
            headers = this.data.headers
        )
    )
}

inline fun <T, E : RootError> Result<ApiSuccess<T?>, E>.requireBody(orElse: () -> E): Result<T, E> = when (this) {
    is Result.Error   -> Result.Error(error)
    is Result.Success -> data.body?.let { Result.Success(it) } ?: Result.Error(orElse())
}

inline fun <T, E : RootError> Result<ApiSuccess<T?>, E>.asResultBodyWithCode(): Result<Pair<T?, Int>, E> = when (this) {
    is Result.Error -> Result.Error(this.error)
    is Result.Success -> Result.Success(Pair(this.data.body, this.data.code))
}

inline fun <T, E : RootError> Result<ApiSuccess<T?>, E>.asFlowResultBody(
    timeoutMillis: Long = 5000L,
    crossinline onTimeout: () -> E
) = flow {
    emit( withTimeoutOrNull(timeoutMillis) {
        this@asFlowResultBody.asResultBody() // Result<T?, E>
    } ?: Result.Error(onTimeout()))
}.flowOn(Dispatchers.IO)