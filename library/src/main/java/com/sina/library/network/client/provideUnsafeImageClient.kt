package com.sina.library.network.client

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun provideUnsafeImageClient(
    address: String,
    sid: String,
    version: String,
    downloadedSize: String = "0",
    flag: Boolean = false
): OkHttpClient {


    try {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val delegate = sslContext.socketFactory
        val debugInterceptor: Interceptor = Interceptor { chain ->
            val request = chain.request()
            val rangeHeader = request.header("Range") ?: "none"

            Log.d("CoilDownloadCheck", "➡️ Request URL: ${request.url}")
            Log.d("CoilDownloadCheck", "➡️ Range header: $rangeHeader")

            val response = chain.proceed(request)
            val responseCode = response.code
            val contentLength = response.body?.contentLength() ?: -1

            Log.d("CoilDownloadCheck", "⬅️ Response code: $responseCode")
            Log.d("CoilDownloadCheck", "⬅️ Response Content-Length: $contentLength bytes")

            // Wrap the response body to count read bytes
            val countingSource = object : ForwardingSource(response.body!!.source()) {
                var totalBytesRead = 0L
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    if (bytesRead != -1L) {
                        totalBytesRead += bytesRead
                        Log.d("CoilDownloadCheck", "📦 Read: $totalBytesRead bytes so far")
                    }
                    return bytesRead
                }
            }

            val newResponseBody = ResponseBody.create(
                response.body!!.contentType(),
                response.body!!.contentLength(),
                countingSource.buffer()
            )

            return@Interceptor response.newBuilder()
                .body(newResponseBody)
                .build()
        }

        val builder = OkHttpClient.Builder().readTimeout(10_000, TimeUnit.MILLISECONDS)
            .connectTimeout(10_000, TimeUnit.MILLISECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val original = chain.request()
                val keys = mutableListOf<String>()
                val values = mutableListOf<String>()

                try {
                    val queries = address.split("/")
                    val query = queries.last().split("?").getOrNull(1)?.split("&") ?: emptyList()
                    for (param in query) {
                        val (key, value) = param.split("=").let { it[0] to it[1] }
                        keys.add(key)
                        values.add(value)
                    }
                } catch (_: Exception) {
                }

                val originalHttpUrl = original.url
                val url = if (keys.isNotEmpty()) {
                    originalHttpUrl.newBuilder().apply {
                        keys.forEachIndexed { index, key -> addQueryParameter(key, values[index]) }
                    }.build()
                } else {
                    originalHttpUrl
                }

                val authorized = original.newBuilder()
                    .addHeader("Cookie", sid)
                    .addHeader("User-Agent", "TeamyarMobileApp/Android/${version}")
                    .addHeader("Referer", address)
                    .apply {
                        if (flag) {
                            addHeader("Range", "bytes=$downloadedSize-")
                        } else {
                            addHeader("Upgrade-Insecure-Requests", "1")
                        }
                    }
                    .url(url)
                    .build()

                // 🟢 Only ONE proceed call — this is what Coil will receive
                return@addInterceptor chain.proceed(authorized)
            }
            .addInterceptor(debugInterceptor)


        builder.sslSocketFactory(delegate, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier { _, _ -> true }

        return builder.build()
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}
