package com.sina.library.permission.newPermission

import android.Manifest
import android.os.Build

object Permissions {
    val CONTACT = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )
    val SCAN_APPS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(Manifest.permission.QUERY_ALL_PACKAGES)
    } else {
        emptyArray()
    }

    // For storage access
    val STORAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    val CAMERA = arrayOf(Manifest.permission.CAMERA)
    val LOCATION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val AUDIO = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    else arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val IMAGES = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    val VIDEO = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.CAMERA)
    else arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    val CONTACTS = arrayOf(Manifest.permission.READ_CONTACTS)
    val MICROPHONE = arrayOf(Manifest.permission.RECORD_AUDIO)
    val PACKAGE_USAGE = arrayOf(Manifest.permission.PACKAGE_USAGE_STATS)
    val QUERY_ALL_PACKAGES = arrayOf(Manifest.permission.QUERY_ALL_PACKAGES)

    val SELFIE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    else arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
}