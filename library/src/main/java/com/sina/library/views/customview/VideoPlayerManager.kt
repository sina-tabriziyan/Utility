package com.sina.library.views.customview

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import android.widget.ImageView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import com.sina.library.utility.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    private var currentPlayerView: PlayerView? = null
    var currentlyPlayingPosition: Int? = null

    init {
        player = ExoPlayer.Builder(context).build()
    }

    fun playVideo(playerView: PlayerView, videoUri: Uri, position: Int) {
        // Check if there's already a video playing
        if (currentlyPlayingPosition != null && currentlyPlayingPosition != position) {
            // Stop the currently playing video
            stopPlayback()
        }

        // Update the currently playing position
        currentlyPlayingPosition = position

        // Detach player from the previous view if necessary
        currentPlayerView?.player = null
        currentPlayerView = playerView

        // Attach player to the new view
        playerView.player = player
        val mediaItem = MediaItem.fromUri(videoUri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    fun stopPlayback() {
        player?.stop()
        currentPlayerView?.player = null // Detach player from the current view
        currentlyPlayingPosition = null // Reset the currently playing position
    }

    fun releasePlayer() {
        player?.release()
        player = null
    }

    private val thumbnailCache = object : LruCache<String, Bitmap>(50) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024 // Cache size in KB
        }
    }

    // Load video thumbnail using Coil
    fun loadVideoThumbnail(videoUri: Uri, imgThumbnailVideo: ImageView) {
        val request = ImageRequest.Builder(context)
            .data(videoUri)
            .placeholder(R.drawable.ic_video_placeholder)
            .error(R.drawable.ic_video_error)
            .target(imgThumbnailVideo)  // Target the ImageView
            .build()

        val imageLoader = ImageLoader(context)
        imageLoader.enqueue(request)
    }

    // Load video thumbnail asynchronously with a background thread if needed
    fun loadVideoThumbnailAsync(videoPath: String, imageView: ImageView) {
        val cachedThumbnail = thumbnailCache.get(videoPath)
        if (cachedThumbnail != null) {
            imageView.setImageBitmap(cachedThumbnail)
        } else {
            // Use coroutines to load the thumbnail on a background thread
            CoroutineScope(Dispatchers.Main).launch {
                val thumbnail = withContext(Dispatchers.IO) {
                    ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND)
                }
                if (thumbnail != null) {
                    thumbnailCache.put(videoPath, thumbnail)
                    imageView.setImageBitmap(thumbnail)
                } else {
                    imageView.setImageResource(R.drawable.ic_video_error)
                }
            }
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media.DATA), null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                        it.getString(columnIndex)
                    } else null
                }
            }
            else -> null
        }
    }
}
