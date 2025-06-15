/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.responses

import com.google.gson.Gson
import com.sina.library.network.errors.NetworkError
import com.sina.library.network.errors.NetworkException
import com.sina.library.network.errors.NoBodyException
import okhttp3.ResponseBody
import retrofit2.Response

fun <T> Response<T>.openResponse(): T {
    return if (isSuccessful) {
        val body = body()
        when {
            code() in 200..201 && body != null -> body
            body == null -> throw NoBodyException()
            else -> throw Exception()
        }
    } else {
        val errorBody: ResponseBody? = errorBody()
        val networkError: NetworkError =
            Gson().fromJson(errorBody?.charStream(), NetworkError::class.java)
        throw NetworkException(error = networkError.asError())
    }
}