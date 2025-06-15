/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.download

import kotlinx.coroutines.channels.Channel

object DownloadFileUtils {
    val downloadApkChannel = Channel<DownloadProgressBarEvent>()

    fun calculatePercent(per: Int, bytesRead: Long, fileSize: Long): Int {
        return if (per == -1 && fileSize > 0) (bytesRead * 100 / fileSize).toInt() else per
    }
}