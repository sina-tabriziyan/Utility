/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.upload

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import com.sina.library.data.model.UploadFileData
import okhttp3.internal.tls.OkHostnameVerifier
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object UploadFile {
    fun upload(uploadFileModel: UploadFileData): String? {
        val connection: HttpURLConnection
        var outputStream: DataOutputStream? = null
        var inputStream: InputStream? = null
        val twoHyphens = "--"
        val boundary = "-----------------------------" + System.currentTimeMillis().toString()
        val lineEnd = "\r\n"
        var result: String? = ""
        var bytesRead: Int
        var bytesAvailable: Int
        var bufferSize: Int
        var bytesRead2: Int
        var bytesAvailable2: Int
        var bufferSize2: Int
        val buffer: ByteArray
        val buffer2: ByteArray
        val maxBufferSize = 4 * 1024 * 1024
        val str: Array<String>? = uploadFileModel.filepath?.split("/".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
        val fileName = str?.get(str.size - 1)
        try {
            val file = uploadFileModel.filepath?.let { File(it) }
            val fileInputStream = FileInputStream(file)
            val trustAllCerts = arrayOf<TrustManager>(
                @SuppressLint("CustomX509TrustManager")
                object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate>? {
                        return arrayOf()
                    }
                }
            )
            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val delegate = sslContext.socketFactory
            sslContext.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(delegate)
            HttpsURLConnection.setDefaultHostnameVerifier(OkHostnameVerifier)
            val url = URL(uploadFileModel.urlTo)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.doOutput = true
            connection.useCaches = false
            connection.connectTimeout = 120000
            connection.setRequestProperty("Cookie", uploadFileModel.cookie)
            connection.setRequestProperty("Referer", uploadFileModel.urlTo)
            connection.requestMethod = "POST"
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("User-Agent", uploadFileModel.userAgent)
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            outputStream = DataOutputStream(connection.outputStream)
            outputStream.writeBytes(twoHyphens + boundary + lineEnd)
            outputStream.writeBytes(
                "Content-Disposition: form-data; name=\"" + uploadFileModel.fileField + "\"; filename=\"" + java.lang.String(
                    fileName?.toByteArray(StandardCharsets.UTF_8), Charsets.ISO_8859_1
                ) + "\"" + lineEnd
            )
            outputStream.writeBytes("Content-Type: ${uploadFileModel.fileMimeType}$lineEnd")
            outputStream.writeBytes(lineEnd)
            bytesAvailable = fileInputStream.available()
            bufferSize = Math.min(bytesAvailable, maxBufferSize)
            buffer = ByteArray(bufferSize)
            bytesRead = fileInputStream.read(buffer, 0, bufferSize)
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize)
                bytesAvailable = fileInputStream.available()
                bufferSize = Math.min(bytesAvailable, maxBufferSize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize)
            }
            if (uploadFileModel.isProfile) {
                val dir = Environment.getExternalStorageDirectory().absoluteFile.toString() + "/Teamyar/Teamyar Images"
                val file2 = File(dir, "resize.jpg")
                val fileInputStream2 = FileInputStream(file2)
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"new_file_2\"; filename=\"resize.jpg\"$lineEnd")
                outputStream.writeBytes("Content-Type: ${uploadFileModel.fileMimeType}$lineEnd")
                outputStream.writeBytes(lineEnd)
                bytesAvailable2 = fileInputStream2.available()
                bufferSize2 = Math.min(bytesAvailable2, maxBufferSize)
                buffer2 = ByteArray(bufferSize2)
                bytesRead2 = fileInputStream2.read(buffer2, 0, bufferSize2)
                while (bytesRead2 > 0) {
                    outputStream.write(buffer2, 0, bufferSize2)
                    bytesAvailable2 = fileInputStream2.available()
                    bufferSize2 = Math.min(bytesAvailable2, maxBufferSize)
                    bytesRead2 = fileInputStream2.read(buffer2, 0, bufferSize2)
                }
            }
            outputStream.writeBytes(lineEnd)

            // Upload POST Data
            val keys: Iterator<String> = uploadFileModel.params.keys.iterator()
            while (keys.hasNext()) {
                val key = keys.next()
                val value: String = uploadFileModel.params.get(key).toString()
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"$key\"$lineEnd")
                outputStream.writeBytes(lineEnd)
                outputStream.writeBytes(lineEnd)
                outputStream.writeBytes(value)
                outputStream.writeBytes(lineEnd)
            }
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
            try {
                if (connection.responseCode != HttpsURLConnection.HTTP_OK) {
                    Log.e("uploaaad_failed", connection.responseMessage + connection.responseCode)
                } else {
                    Log.e("uploaaad_success", connection.responseMessage + connection.responseCode)
                }
                if (connection.responseCode == 413) result = "large"
                inputStream = connection.inputStream
                result = convertStreamToString(inputStream)
            } catch (e: InterruptedIOException) {
                e.printStackTrace()
            }
            inputStream!!.close()
            fileInputStream.close()
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            result = "FailedConnect"
        }
        return result
    }

    private fun convertStreamToString(`is`: InputStream): String {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String? = null
        try {
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return sb.toString()
    }
}
