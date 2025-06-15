/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.responses

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException


class ProgressResponseBody(
    private val url: String, // URL can be useful for identifying the download
    private val responseBody: ResponseBody,
    private val update: (url: String?, percent: Int, byteRead: Long, contentLength: Long, done: Boolean) -> Unit
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentLength(): Long = responseBody.contentLength()

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun source(): BufferedSource {
        // Use the backing field for lazy initialization correctly
        if (bufferedSource == null) {
            bufferedSource = realSource(responseBody.source()).buffer()
        }
        return bufferedSource!! // Safe due to the check above
    }

    private fun realSource(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            var lastReportedProgressBytes = 0L // To manage progress update frequency

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                val isDownloadComplete = bytesRead == -1L

                if (!isDownloadComplete) {
                    totalBytesRead += bytesRead
                }

                val contentLength = contentLength() // Cache for efficiency within this call

                // Update progress:
                // - If download is complete
                // - Or if enough new bytes have been read since last update
                // - Or if it's the very first read (totalBytesRead > 0 and lastReportedProgressBytes == 0L)
                val shouldReportProgress = isDownloadComplete ||
                        (totalBytesRead > 0 && lastReportedProgressBytes == 0L && !isDownloadComplete) ||
                        (totalBytesRead - lastReportedProgressBytes >= PROGRESS_UPDATE_THRESHOLD_BYTES && !isDownloadComplete)


                if (shouldReportProgress && contentLength > 0) { // Only report if content length is known
                    val percent = ((totalBytesRead * 100) / contentLength).toInt()
                    update(
                        url,
                        percent.coerceIn(0, 100), // Ensure percent is between 0 and 100
                        totalBytesRead,
                        contentLength,
                        isDownloadComplete
                    )
                    lastReportedProgressBytes = totalBytesRead
                } else if (isDownloadComplete) { // Handle case where content length might be unknown but download finishes
                    update(
                        url,
                        100, // Assume 100% if done, even if contentLength was -1
                        totalBytesRead,
                        contentLength, // Could still be -1
                        true
                    )
                }
                return bytesRead
            }
        }
    }

    companion object {
        // Adjust this threshold as needed (e.g., 1% of total size, or a fixed byte count)
        private const val PROGRESS_UPDATE_THRESHOLD_BYTES = 100 * 1024 // e.g., Report every 100KB
    }
}
