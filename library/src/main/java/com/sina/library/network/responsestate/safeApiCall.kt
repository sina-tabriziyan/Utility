package com.sina.library.network.responsestate

import android.util.Log
import com.google.gson.JsonParseException
import retrofit2.Response
import java.io.IOException


suspend inline fun <reified T> safeApiCall(
    crossinline apiCall: suspend () -> Response<T>,
    successCodes: List<Int> = listOf(200, 302)
): Result<ApiSuccess<T>, DataError.Network> {
    return try {
        val response = apiCall()
        val code = response.code()
        val body = response.body()
        Log.e("TAG", "safeApiCall code: $code", )
        if (code in successCodes) {
            return when{
                code in 300..302->Result.Success(
                    ApiSuccess(
                        code = code,
                        body = body,
                        headers = response.headers()
                    )
                )
                body != null ->Result.Success(
                    ApiSuccess(
                        code = code,
                        body = body,
                        headers = response.headers()
                    )
                )
                else->Result.Error(DataError.Network.SERIALIZATION)
            }

        } else {
            val error = when (code) {
                408 -> DataError.Network.REQUEST_TIMEOUT
                429 -> DataError.Network.TOO_MANY_REQUESTS
                413 -> DataError.Network.PAYLOAD_TOO_LARGE
                in 500..599 -> DataError.Network.SERVER_ERROR
                else -> DataError.Network.UNKNOWN
            }
            Result.Error(error)
        }

    } catch (e: IOException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: JsonParseException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: Exception) {
        Result.Error(DataError.Network.UNKNOWN)
    }
}


suspend inline fun <reified T> safeApiCall(
    crossinline apiCall: suspend () -> Response<T>,
    successCodes: List<Int> = listOf(200), // default: only 200
    treatEmptyBodyAsError: Boolean = true  // config for nullable body
): Result<ApiSuccess<T>, DataError.Network> {
    return try {
        val response = apiCall()
        val code = response.code()
        val body = response.body()

        if (code in successCodes) {
            if (body != null || !treatEmptyBodyAsError) {
                Result.Success(
                    ApiSuccess(
                        code,
                        body as T,
                        headers = response.headers()
                    )
                ) // safe cast: body could be null if allowed
            } else {
                Result.Error(DataError.Network.SERIALIZATION)
            }
        } else {
            val error = when (code) {
                408 -> DataError.Network.REQUEST_TIMEOUT
                429 -> DataError.Network.TOO_MANY_REQUESTS
                413 -> DataError.Network.PAYLOAD_TOO_LARGE
                in 500..599 -> DataError.Network.SERVER_ERROR
                else -> DataError.Network.UNKNOWN
            }
            Result.Error(error)
        }

    } catch (e: IOException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: JsonParseException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: Exception) {
        Result.Error(DataError.Network.UNKNOWN)
    }
}
