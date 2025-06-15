package com.sina.library.network.errors

import com.sina.library.network.status.Status

data class Error(
    val code: String,
    val message: String,
    val data: Status
)