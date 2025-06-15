/**
 * Created by ST on 1/26/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.customview

import android.net.Uri
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

fun loadThumbnailIntoPlayerView(playerView:PlayerView, videoUri:Uri) {
    val player = ExoPlayer.Builder(playerView.context).build()
    playerView.player = player
    player.setMediaItem( MediaItem.fromUri(videoUri))
    player.prepare()
    player.seekTo(0) // Seek to the start (or any other time)
    playerView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            player.release()
        }
    })
}