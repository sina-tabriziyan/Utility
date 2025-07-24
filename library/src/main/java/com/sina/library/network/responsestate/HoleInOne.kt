///**
// * Created by ST on 7/24/2025.
// * Author: Sina Tabriziyan
// * @sina.tabriziyan@gmail.com
// */
//package com.sina.library.network.responsestate
//
//import com.google.gson.JsonParseException
//import retrofit2.Response
//import java.io.IOException
//
//sealed interface Error
//
//typealias RootError = Error
//
//sealed interface Result<out D, out E: RootError> {
//    data class Success<out D, out E: RootError>(val data: D): Result<D, E>
//    data class Error<out D, out E: RootError>(val error: E): Result<D, E>
//}
//
//inline fun <D, E : RootError> Result<D, E>.getOrNull(): D? =
//    when (this) {
//        is Result.Success -> data
//        is Result.Error -> null
//    }
//inline fun <D, E : RootError> Result<D, E>.getErrorOrNull(): E? =
//    when (this) {
//        is Result.Success -> null
//        is Result.Error -> error
//    }
//inline fun <D, E : RootError> Result<D, E>.isSuccess() = this is Result.Success
//sealed class Resource<T>(val data: T? = null, val message: String? = null) {
//    class Success<T>(data: T?): Resource<T>(data)
//    class Error<T>(message: String, data: T? = null): Resource<T>(data, message)
//}
//
//sealed interface DataError: Error {
//    enum class Network: DataError {
//        REQUEST_TIMEOUT,
//        TOO_MANY_REQUESTS,
//        NO_INTERNET,
//        PAYLOAD_TOO_LARGE,
//        SERVER_ERROR,
//        SERIALIZATION,
//        UNKNOWN
//    }
//    enum class Local: DataError {
//        DISK_FULL
//    }
//}
//
//data class ApiSuccess<T>(
//    val code: Int,
//    val body: T
//)
//
//suspend inline fun <reified T> safeApiCall(
//    crossinline apiCall: suspend () -> Response<T>,
//    successCodes: List<Int> = listOf(200, 302)
//): Result<ApiSuccess<T>, DataError.Network> {
//    return try {
//        val response = apiCall()
//        val code = response.code()
//        val body = response.body()
//
//        if (code in successCodes) {
//            if (body != null) {
//                Result.Success(ApiSuccess(code, body))
//            } else {
//                Result.Error(DataError.Network.SERIALIZATION)
//            }
//        } else {
//            val error = when (code) {
//                408 -> DataError.Network.REQUEST_TIMEOUT
//                429 -> DataError.Network.TOO_MANY_REQUESTS
//                413 -> DataError.Network.PAYLOAD_TOO_LARGE
//                in 500..599 -> DataError.Network.SERVER_ERROR
//                else -> DataError.Network.UNKNOWN
//            }
//            Result.Error(error)
//        }
//
//    } catch (e: IOException) {
//        Result.Error(DataError.Network.NO_INTERNET)
//    } catch (e: JsonParseException) {
//        Result.Error(DataError.Network.SERIALIZATION)
//    } catch (e: Exception) {
//        Result.Error(DataError.Network.UNKNOWN)
//    }
//}
//
//
//inline fun <T> safeLocalCall(block: () -> T): Result<T, DataError.Local> {
//    return try {
//        Result.Success(block())
//    } catch (e: Exception) {
//        Result.Error(DataError.Local.DISK_FULL) // You could make this smarter
//    }
//}
//suspend inline fun <D1, E1 : RootError, D2, E2 : RootError> Result<D1, E1>.flatMapIfSuccess(
//    crossinline block: suspend (D1) -> Result<D2, E2>
//): Result<D2, RootError> {
//    return when (this) {
//        is Result.Success -> block(this.data)
//        is Result.Error -> Result.Error(this.error)
//    }
//}
//
//inline fun <reified T, E : RootError> Result<Unit, E>.mapTo(data: T): Result<T, E> {
//    return when (this) {
//        is Result.Success -> Result.Success(data)
//        is Result.Error -> Result.Error(error)
//    }
//}