package com.sina.library.network.download

sealed class DownloadResult {
    /** Indicates the download is in progress. */
    data class Progress(val percentage: Int) : DownloadResult()

    /** Indicates the download started but the total size is unknown. UI can show indeterminate progress. */
    data object StartedUnknownSize : DownloadResult()

    /** Indicates the download completed successfully. */
    data class Success(val filePath: String, val totalBytes: Long) : DownloadResult()

    /** Indicates the download failed. */
    data class Error(val message: String, val cause: Throwable? = null) : DownloadResult()

    /** Indicates the download was explicitly cancelled. */
    data object Cancelled : DownloadResult()
}
