package com.sina.library.network.download

data class DownloadProgressBarEvent(var downloadID: Int, var percent: Int, var bytesRead: Long, var contentLength: Long)
