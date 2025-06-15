/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.download

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

class FDLRepository {

    companion object {
        private const val TAG = "FileDownloadRepo"
        private const val DEFAULT_BUFFER_SIZE = 8192 // Increased buffer size for potentially better performance
        private const val MIN_PROGRESS_STEP_PERCENTAGE = 1 // Only emit progress if it changes by at least this much
    }

    /**
     * Downloads a file from the given ResponseBody to the specified destination path.
     * Emits DownloadResult states via a Flow.
     *
     * @param responseBody The ResponseBody containing the file data.
     * @param destinationPath The absolute path where the file should be saved.
     * @param Clobber if true, existing file at destinationPath will be overwritten.
     * @return A Flow emitting DownloadResult states.
     */
    fun downloadFile(
        responseBody: ResponseBody,
        destinationPath: String,
        clobber: Boolean = true // Default to true, meaning overwrite if file exists
    ): Flow<DownloadResult> = flow {
        // 1. Initial Validations
        if (destinationPath.isBlank()) {
            emit(DownloadResult.Error("Destination path cannot be blank.", IllegalArgumentException("Path is blank")))
            return@flow
        }

        val file = File(destinationPath)

        // Handle existing file based on 'clobber' flag
        if (file.exists()) {
            if (clobber) {
                if (!file.delete()) {
                    emit(DownloadResult.Error("Failed to delete existing file at: $destinationPath", IOException("Delete failed")))
                    return@flow
                }
                Log.d(TAG, "Existing file deleted: $destinationPath")
            } else {
                // File exists and clobber is false, treat as success or specific error
                emit(DownloadResult.Error("File already exists and clobber is false: $destinationPath", FileAlreadyExistsException(file)))
                // Alternatively, could emit Success if existing file is considered valid
                // emit(DownloadResult.Success(destinationPath, file.length()))
                return@flow
            }
        }

        // Ensure parent directory exists
        file.parentFile?.let {
            if (!it.exists() && !it.mkdirs()) {
                emit(DownloadResult.Error("Failed to create parent directories for: $destinationPath", IOException("Mkdirs failed")))
                return@flow
            }
        }

        // 2. Setup Streams (within try-finally for resource cleanup)
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var totalBytesRead: Long = 0

        try {
            inputStream = responseBody.byteStream()
            outputStream = FileOutputStream(file)
            Log.d(TAG, "Starting download for: $destinationPath")

            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val totalFileSize = responseBody.contentLength() // Returns -1 if unknown

            if (totalFileSize <= 0) {
                emit(DownloadResult.StartedUnknownSize)
                Log.d(TAG, "Download size unknown for: $destinationPath")
            } else {
                emit(DownloadResult.Progress(0)) // Initial progress
            }

            var bytesReadLoop: Int
            var lastReportedPercentage = -1

            // 3. Download Loop
            while (true) {
                // Check for coroutine cancellation before each read
                // This makes the download loop cooperative with cancellation.
                // ensureActive() or if(!isActive) can be used.
                // `currentCoroutineContext().ensureActive()` is more explicit.
                kotlinx.coroutines.currentCoroutineContext().ensureActive()


                bytesReadLoop = inputStream.read(buffer)
                if (bytesReadLoop == -1) break // End of stream

                outputStream.write(buffer, 0, bytesReadLoop)
                totalBytesRead += bytesReadLoop

                if (totalFileSize > 0) {
                    val progressPercentage = ((totalBytesRead * 100) / totalFileSize).toInt()
                    // Only emit if progress changes significantly to avoid flooding
                    if (progressPercentage >= lastReportedPercentage + MIN_PROGRESS_STEP_PERCENTAGE && progressPercentage <= 100) {
                        emit(DownloadResult.Progress(progressPercentage))
                        lastReportedPercentage = progressPercentage
                    }
                }
                // For unknown size, no percentage progress, but UI knows it's active.
            }

            outputStream.flush() // Ensure all buffered data is written to disk

            // 4. Final Result Emission
            if (totalFileSize > 0 && totalBytesRead < totalFileSize) {
                // This indicates a potential issue like premature stream closing from server
                Log.w(TAG, "Download for $destinationPath may be incomplete. Read $totalBytesRead of $totalFileSize bytes.")
                // Depending on requirements, you might want to emit an Error here
                // For now, we'll emit success, but the byte count difference is a flag.
                file.delete() // Clean up potentially corrupt file
                emit(DownloadResult.Error("Download incomplete: Received $totalBytesRead of $totalFileSize bytes.", IOException("Incomplete download")))
            } else {
                // Ensure 100% is emitted if it wasn't due to MIN_PROGRESS_STEP_PERCENTAGE
                if (totalFileSize > 0 && lastReportedPercentage < 100) {
                    emit(DownloadResult.Progress(100))
                }
                emit(DownloadResult.Success(destinationPath, totalBytesRead))
                Log.i(TAG, "Download successful: $destinationPath, Total Bytes: $totalBytesRead")
            }

        } catch (e: CancellationException) {
            Log.i(TAG, "Download cancelled for: $destinationPath", e)
            file.delete() // Clean up partially downloaded file on cancellation
            emit(DownloadResult.Cancelled)
            throw e // Re-throw CancellationException as per structured concurrency guidelines
        } catch (e: IOException) {
            Log.e(TAG, "IOException during download to $destinationPath: ${e.message}", e)
            file.delete() // Clean up partially downloaded file on error
            emit(DownloadResult.Error("Network or file error: ${e.localizedMessage}", e))
        } catch (e: Exception) { // Catch any other unexpected exceptions
            Log.e(TAG, "Unexpected error during download to $destinationPath: ${e.message}", e)
            file.delete() // Clean up
            emit(DownloadResult.Error("An unexpected error occurred: ${e.localizedMessage}", e))
        } finally {
            // 5. Resource Cleanup
            try {
                inputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing input stream for $destinationPath: ${e.message}", e)
            }
            try {
                outputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing output stream for $destinationPath: ${e.message}", e)
            }
        }
    }.flowOn(Dispatchers.IO) // Crucial: Perform all blocking IO operations on the IO dispatcher
}
