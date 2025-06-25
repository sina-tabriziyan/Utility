package com.sina.library.permission

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Build

object SimplePermissions {
    val AUDIO_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(READ_MEDIA_AUDIO)
    else arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)

    val IMAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(READ_MEDIA_IMAGES)
    else arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)

    val VIDEO_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(READ_MEDIA_VIDEO, CAMERA)
    else arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)

    val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(READ_MEDIA_AUDIO, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
    else arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)

    val CONTACT_PERMISSIONS = arrayOf(READ_CONTACTS)
    val CAMERA_PERMISSIONS = arrayOf(CAMERA)
    val LOCATION_PERMISSIONS = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
    val RECORD_AUDIO_PERMISSIONS = arrayOf(RECORD_AUDIO)

    val SELFIE_RECORDING_PERMISSIONS: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(CAMERA, READ_MEDIA_IMAGES)
        else arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
}
