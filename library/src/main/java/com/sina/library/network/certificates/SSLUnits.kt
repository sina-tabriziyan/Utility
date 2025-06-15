/**
 * Created by ST on 5/3/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */

package com.sina.library.network.certificates

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SSLUnits {

    fun unsafeSSL(): SSLSocketFactory?{
        val delegate: SSLSocketFactory

        try {
            // Install the all-trusting trust manager
            val sslContext= SSLContext.getInstance("SSL")
            sslContext.init(null, trustManager(), SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            delegate=sslContext.socketFactory
        }catch (e:Exception){
            throw RuntimeException(e)
        }

        return delegate
    }

    fun trustManager() : Array<TrustManager>{
        val trustManager= object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }

        }

        return arrayOf(trustManager)
    }

}
