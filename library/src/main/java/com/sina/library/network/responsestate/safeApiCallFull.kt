package com.sina.library.network.responsestate

import com.google.gson.JsonParseException
import retrofit2.Response
import java.io.IOException

suspend inline fun <reified T> safeApiCallFull(
    crossinline apiCall: suspend () -> Response<T>,
    successCodes: List<Int> = listOf(200, 302)
): Result<FullApiResponse<T>, DataError.Network> {
    return try {
        val response = apiCall()
        val code = response.code()
        val body = response.body()

        if (code in successCodes) {
            Result.Success(
                FullApiResponse(
                    code = code,
                    body = body,
                    headers = response.headers()
                )
            )
        } else {
            Result.Error(
                when (code) {
                    408 -> DataError.Network.REQUEST_TIMEOUT
                    429 -> DataError.Network.TOO_MANY_REQUESTS
                    413 -> DataError.Network.PAYLOAD_TOO_LARGE
                    in 500..599 -> DataError.Network.SERVER_ERROR
                    else -> DataError.Network.UNKNOWN
                }
            )
        }
    } catch (_: IOException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (_: JsonParseException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (_: Exception) {
        Result.Error(DataError.Network.UNKNOWN)
    }
}
