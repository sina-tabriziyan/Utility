package com.sina.library.permission.newPermission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object AppPermissions {
    // All permission groups
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

    val STORAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    else arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    val CONTACTS = arrayOf(Manifest.permission.READ_CONTACTS)
    val CAMERA = arrayOf(Manifest.permission.CAMERA)
    val LOCATION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
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

    // Permission management system
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var currentCallback: (Map<String, Boolean>) -> Unit = { _ -> }

    fun initialize(activity: AppCompatActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            currentCallback(results)
        }
    }

    fun initialize(fragment: Fragment) {
        permissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            currentCallback(results)
        }
    }

    fun requestPermissions(
        context: Context,
        permissions: Array<String>,
        callback: (Map<String, Boolean>) -> Unit
    ) {
        currentCallback = callback

        when {
            hasPermissions(context, permissions) -> {
                callback(permissions.associateWith { true })
            }
            shouldShowRationale(context, permissions) -> {
                showRationaleDialog(context as Activity, permissions)
            }
            else -> {
                permissionLauncher.launch(permissions)
            }
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun shouldShowRationale(context: Context, permissions: Array<String>): Boolean {
        val activity = context as? Activity ?: return false
        return permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    private fun showRationaleDialog(activity: Activity, permissions: Array<String>) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Needed")
            .setMessage("These permissions are required for full app functionality")
            .setPositiveButton("Continue") { _, _ ->
                permissionLauncher.launch(permissions)
            }
            .setNegativeButton("Cancel") { _, _ ->
                currentCallback(permissions.associateWith { false })
            }
            .setOnDismissListener {
                currentCallback(permissions.associateWith { false })
            }
            .show()
    }

    fun showSettingsDialog(context: Context) {
        val activity = context as? Activity ?: return
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("Please enable permissions in app settings")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun getDeniedPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }
}