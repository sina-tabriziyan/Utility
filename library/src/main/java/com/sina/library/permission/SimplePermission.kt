package com.sina.library.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.collections.all
import kotlin.collections.any
import kotlin.collections.toTypedArray
import kotlin.jvm.functions.Function1

object SimplePermission {

    private lateinit var requestLauncher: ActivityResultLauncher<Array<String>>
    private var permissionCallback: ((Boolean) -> Unit)? = null

    fun register(activity: AppCompatActivity) {
        requestLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(activity, permissions)
        }
    }

    fun register(fragment: Fragment) {
        requestLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(fragment.requireActivity(), permissions)
        }
    }

    fun request(activity: Activity, permissions: Array<String>, callback: (Boolean) -> Unit) {
        permissionCallback = callback

        if (isPermissionGranted(activity, permissions)) {
            callback(true)
        } else if (shouldShowRationale(activity, permissions)) {
            showRationaleDialog(activity) {
                requestLauncher.launch(permissions)
            }
        } else {
            requestLauncher.launch(permissions)
        }
    }

    private fun isPermissionGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun shouldShowRationale(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    private fun handlePermissionResult(activity: Activity, permissions: Map<String, Boolean>) {
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            Toast.makeText(activity, "Permission Granted!", Toast.LENGTH_SHORT).show()
            permissionCallback?.invoke(true)
        } else {
            if (!shouldShowRationale(activity, permissions.keys.toTypedArray())) {
                showSettingsDialog(activity)
            } else {
                Toast.makeText(activity, "Permission Denied!", Toast.LENGTH_SHORT).show()
                permissionCallback?.invoke(false)
            }
        }
    }

    private fun showRationaleDialog(activity: Activity, onAccept: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("This app needs access to this feature to function properly.")
            .setPositiveButton("Grant") { _, _ -> onAccept() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("Permission is permanently denied. Enable it in settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null)
                )
                activity.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
