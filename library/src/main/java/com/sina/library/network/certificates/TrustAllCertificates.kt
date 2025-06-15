/**
 * Created by ST on 5/3/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.certificates

import android.annotation.SuppressLint
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object TrustAllCertificates {
    val trustAllCerts: X509TrustManager = @SuppressLint("CustomX509TrustManager")
    object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    fun createSSLSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())
        return sslContext.socketFactory
    }
}
