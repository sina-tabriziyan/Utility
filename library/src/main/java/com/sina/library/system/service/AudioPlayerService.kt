package com.sina.library.system.service

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sina.library.data.sealed.AudioState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class AudioPlayerService(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val _audioStateFlow = MutableStateFlow<AudioState>(AudioState.Initial)
    val audioStateFlow: StateFlow<AudioState> = _audioStateFlow.asStateFlow()

    private var currentPlayingPositionTag: Int = -1
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var progressUpdateRunnable: Runnable

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = exoPlayer ?: return

            if (isPlaying) {
                startProgressUpdater()
                _audioStateFlow.value = AudioState.Playing(
                    currentPlayingPositionTag,
                    player.currentPosition.toInt().coerceAtLeast(0),
                    player.duration.toInt().coerceAtLeast(0)
                )
            } else {
                if (player.playbackState != Player.STATE_ENDED) {
                    _audioStateFlow.value = AudioState.Paused(
                        currentPlayingPositionTag,
                        player.currentPosition.toInt().coerceAtLeast(0)
                    )
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            when (playbackState) {
                Player.STATE_READY -> {
                    // It's possible the player becomes ready but isn't immediately playing
                    // (e.g. if playWhenReady is false initially, or due to audio focus)
                    // So, we update based on current isPlaying state.
                    if (player.isPlaying) {
                        startProgressUpdater()
                        _audioStateFlow.value = AudioState.Playing(
                            currentPlayingPositionTag,
                            player.currentPosition.toInt().coerceAtLeast(0),
                            player.duration.toInt().coerceAtLeast(0)
                        )
                    } else {
                        // If ready but not playing, could be considered Paused or still Preparing/Buffering
                        // For simplicity, if duration is available, and not playing, update Paused.
                        // If it's already playing, onIsPlayingChanged would have set Playing state.
                        if (player.duration > 0 && _audioStateFlow.value !is AudioState.Playing) {
                            _audioStateFlow.value = AudioState.Paused(
                                currentPlayingPositionTag,
                                player.currentPosition.toInt().coerceAtLeast(0)
                            )
                        }
                        // If you have a distinct "ReadyToPlayButNotPlaying" state, you could emit it here.
                    }
                }
                Player.STATE_ENDED -> {
                    stopProgressUpdater()
                    _audioStateFlow.value = AudioState.Stopped(currentPlayingPositionTag)
                }
                Player.STATE_BUFFERING -> { /* Optionally emit Buffering state */ }
                Player.STATE_IDLE -> { /* Optionally emit Idle/Initial state */ }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopProgressUpdater()
            _audioStateFlow.value = AudioState.Error(
                currentPlayingPositionTag,
                error.message ?: "Unknown player error"
            )
        }
    }

    private fun extractFileName(filePath: String): String {
        return File(filePath).name // This directly gives "bikalam.mp3"
    }

    suspend fun playAudio(filePath: String, itemPositionTag: Int) {
        withContext(Dispatchers.Main) {
            stopAudioInternalAndClearPlayer()

            currentPlayingPositionTag = itemPositionTag
            val fileName = extractFileName(filePath)

            // Emit AudioDetails state with the filename
            _audioStateFlow.value = AudioState.AudioDetails(itemPositionTag, fileName)

            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(playerListener)

                val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
                setMediaItem(mediaItem)
                prepare()
                play() // playWhenReady is true by default after play()

                // The Playing state will be emitted by onIsPlayingChanged(true)
                // or onPlaybackStateChanged(READY) if already playing.
                // No need to emit Playing state immediately here, let the listener handle it.
            }
        }
    }

    fun startProgressUpdater() {
        if (this::progressUpdateRunnable.isInitialized) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        }
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    _audioStateFlow.value = AudioState.Playing(
                        currentPlayingPositionTag,
                        player.currentPosition.toInt().coerceAtLeast(0),
                        player.duration.toInt().coerceAtLeast(0)
                    )
                    progressUpdateHandler.postDelayed(this, 500) // Update every 500ms
                } else {
                    stopProgressUpdater()
                }
            }
        }
        progressUpdateHandler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdater() {
        if (this::progressUpdateRunnable.isInitialized) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        }
    }

    suspend fun pauseAudio() {
        withContext(Dispatchers.Main) {
            exoPlayer?.pause()
        }
    }

    suspend fun resumeAudio() {
        withContext(Dispatchers.Main) {
            exoPlayer?.play()
        }
    }

    private suspend fun stopAudioInternalAndClearPlayer() {
        withContext(Dispatchers.Main) {
            stopProgressUpdater()
            exoPlayer?.let { player ->
                player.removeListener(playerListener)
                player.release()
            }
            exoPlayer = null
        }
        if (currentPlayingPositionTag != -1 && _audioStateFlow.value !is AudioState.Initial) {
            // Only emit Stopped if it was playing/paused/error or had details for this tag
            _audioStateFlow.value = AudioState.Stopped(currentPlayingPositionTag)
        }
        currentPlayingPositionTag = -1 // Reset after potential Stopped emission
    }

    suspend fun stopAudio(itemPositionTag: Int) {
        if (currentPlayingPositionTag == itemPositionTag || exoPlayer != null) {
            stopAudioInternalAndClearPlayer()
        }
    }

    suspend fun seekTo(progressMillis: Long) {
        withContext(Dispatchers.Main) {
            exoPlayer?.seekTo(progressMillis)
            exoPlayer?.let {
                val currentPosition = it.currentPosition.toInt().coerceAtLeast(0)
                val duration = it.duration.toInt().coerceAtLeast(0)
                // Reflect seek immediately in the state
                if (it.isPlaying) {
                    _audioStateFlow.value = AudioState.Playing(currentPlayingPositionTag, currentPosition, duration)
                } else {
                    // If duration is 0, it might still be preparing, avoid Paused state if so.
                    if (duration > 0 || it.playbackState == Player.STATE_READY) {
                        _audioStateFlow.value = AudioState.Paused(currentPlayingPositionTag, currentPosition)
                    }
                }
            }
        }
    }

    suspend fun releasePlayer() {
        stopAudioInternalAndClearPlayer()
        _audioStateFlow.value = AudioState.Initial // Explicitly go to Initial state on full release
        currentPlayingPositionTag = -1
    }
}
