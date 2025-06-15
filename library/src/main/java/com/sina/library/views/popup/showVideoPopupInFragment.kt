package com.sina.library.views.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sina.library.R

/**
 * Shows a video inside a PopupWindow using Media3 ExoPlayer.
 *
 * @param hostView An anchor view from your Activity/Fragment (usually `findViewById(android.R.id.content)`).
 * @param videoUrl  The URL or URI string of the video to play.
 */

// Call this whenever you need to show the video popup
@OptIn(UnstableApi::class)
fun showVideoPopupInFragment(activity: ComponentActivity, hostView: View, videoUrl: String) {
    // 1) Inflate your popup layout
    val inflater = LayoutInflater.from(activity)
    val popupView: View = inflater.inflate(R.layout.popup_video, null)

    // 2) (Optional) If you want to reference any views inside popup_video.xml:
    val playerContainer: FrameLayout = popupView.findViewById(R.id.player_container)
    val playerView: PlayerView       = popupView.findViewById(R.id.popup_player_view)
    val fullscreenButton: ImageButton = popupView.findViewById(R.id.fullscreen_toggle_button)

    // 3) Build your ExoPlayer (Media3) the same way you would in an Activity
    val player = ExoPlayer.Builder(activity).build().also { exoPlayer ->
        playerView.player = exoPlayer
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // We do NOT rotate the Activity here—leave onVideoSizeChanged blank
        exoPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                // no-op
            }
        })
    }

    // 4) Compute “small” dimensions (80% × 40% of screen)
    val displayMetrics = activity.resources.displayMetrics
    val screenWidth  = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels
    val smallWidth  = (screenWidth * 0.8f).toInt()
    val smallHeight = (screenHeight * 0.4f).toInt()

    // 5) Initially size your player container to smallWidth × smallHeight
    //    (Assuming popup_video.xml has a root like <FrameLayout> → <MaterialCardView android:id="@+id/player_container">…)
    playerContainer.layoutParams = FrameLayout.LayoutParams(
        smallWidth,
        smallHeight,
        Gravity.CENTER   // <-- keep it centered
    )
    // 6) Create the PopupWindow to cover the entire screen—
    //    so its root’s background (if semi-transparent) or the dim flag actually shades everything behind it.
    val popupWindow = PopupWindow(
        popupView,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
        true
    ).apply {
        isOutsideTouchable = true

        // Give the popup a slight elevation so it casts a shadow
        // (API 21+). You can tweak this value.
        elevation = 12f

        // If you prefer a MaterialCardView w/ its own elevation for the video container,
        // you can skip popupWindow.elevation and set app:cardElevation on the card in XML instead.

        setOnDismissListener {
            // 1) Release your player
            player.release()
            // 2) Restore system UI if we hid it
            showSystemUI(activity.window.decorView)
        }
    }

    // 7) Now show the popup, centered over your Fragment’s hostView (e.g. a RecyclerView or root layout)
    popupWindow.showAtLocation(hostView, Gravity.CENTER, 0, 0)

    // ──────────── DIM THE BACKGROUND ────────────
    // Grab the popup’s “container” View (its parent in the WindowManager),
    // then apply FLAG_DIM_BEHIND + dimAmount.
    val containerView = popupWindow.contentView.parent as? View
    if (containerView != null) {
        val params = (containerView.layoutParams as WindowManager.LayoutParams)
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        params.dimAmount = 0.5f           // 0.0 = no dim; 1.0 = full black
        val wm = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.updateViewLayout(containerView, params)
    }
    // ────────────────────────────────────────────

    // 8) Track fullscreen vs. small state
    var isFullscreen = false
    fullscreenButton.setOnClickListener {
        if (isFullscreen) {
            // ───── EXIT FULLSCREEN ─────
            playerContainer.layoutParams = FrameLayout.LayoutParams(smallWidth, smallHeight)
            showSystemUI(activity.window.decorView)
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen)
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            isFullscreen = false
        } else {
            // ───── ENTER FULLSCREEN ─────
            playerContainer.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            hideSystemUI(activity.window.decorView)
            fullscreenButton.setImageResource(R.drawable.ic_close)
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            isFullscreen = true
        }
        // Because we changed layoutParams, request a layout pass:
        playerContainer.requestLayout()
    }
}

// Helpers to hide/show system UI (same as in an Activity)
private fun hideSystemUI(decorView: View) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        decorView.windowInsetsController?.let { controller ->
            controller.hide(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }
}

private fun showSystemUI(decorView: View) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        decorView.windowInsetsController?.show(
            WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
        )
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}
