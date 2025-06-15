package com.sina.library.system.tools


import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService


/**
 * Triggers a default buzz pattern on the device.
 *
 * @param activity The current Activity context.
 */
@SuppressLint("MissingPermission")
fun buzz(activity: Activity?) {
    val buzzer = activity?.getSystemService<Vibrator>()
    buzzer?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buzzer.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 20, 60), -1))
        } else {
            //deprecated in API 26
            buzzer.vibrate(longArrayOf(0, 200, 100, 300), -1)
        }
    }
}

/**
 * Triggers a custom buzz pattern on the device.
 *
 * @param activity The current Activity context.
 * @param timings An array of on/off timings in milliseconds.
 * @param repeat The index into the timings array at which to repeat, or -1 for no repetition.
 */
@SuppressLint("MissingPermission")
fun buzz(activity: Activity?, timings: LongArray, repeat: Int) {
    val buzzer = activity?.getSystemService<Vibrator>()
    buzzer?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buzzer.vibrate(VibrationEffect.createWaveform(timings, repeat))
        } else {
            //deprecated in API 26
            buzzer.vibrate(timings, repeat)
        }
    }
}

/**
 * Triggers a simple one-shot vibration.
 *
 * @param activity The current Activity context.
 * @param durationMillis The duration of the vibration in milliseconds.
 */
@SuppressLint("MissingPermission")
fun vibrate(activity: Activity?, durationMillis: Long = 500) {
    val vibrator = activity?.getSystemService<Vibrator>()
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
            it.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(durationMillis)
        }
    }
}