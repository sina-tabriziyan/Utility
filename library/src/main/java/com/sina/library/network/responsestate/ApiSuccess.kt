package com.sina.library.network.responsestate

data class ApiSuccess<T>(
    val code: Int,
    val body: T
)
