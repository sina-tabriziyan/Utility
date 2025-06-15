package com.sina.library.network.errors

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().build())
}