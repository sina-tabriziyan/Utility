package com.sina.library.network.responsestate

import okhttp3.Headers

data class ApiSuccess<T>(
    val code: Int,
    val body: T,
    val headers: Headers
)

data class FullApiResponse<T>(
    val code: Int,
    val body: T,
    val headers: Headers
)
