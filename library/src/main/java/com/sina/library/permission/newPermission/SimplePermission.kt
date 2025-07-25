package com.sina.library.permission.newPermission

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

object SimplePermission {
  private const val TAG = "PermissionFragment"
  private const val PREFS_NAME = "simple_perm_prefs"
  private const val COUNT_PREFIX = "perm_count_"
  private const val REQUEST_CODE = 0xF00D
  private const val DENIALS_NEEDED_FOR_SETTINGS = 2 // Show settings after 2 denials

  private lateinit var app: Application
  private lateinit var prefs: SharedPreferences

  fun register(application: Application) {
    app = application
    prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun request(
    activity: FragmentActivity,
    permissions: Array<String>,
    onResult: (Boolean) -> Unit
  ) {
    // Check manifest declarations
    val pm: PackageManager = activity.packageManager
    val pkgInfo: PackageInfo = pm.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
    val declared: Set<String> = pkgInfo.requestedPermissions?.toSet() ?: emptySet()
    val missing = permissions.filter { it !in declared }

    if (missing.isNotEmpty()) {
      throw IllegalStateException("Missing permissions in manifest: ${missing.joinToString()}")
    }

    // If already granted, return immediately
    if (permissions.all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }) {
      onResult(true)
      return
    }

    // Get current denial count
    val key = COUNT_PREFIX + permissions.joinToString("_")
    val denialCount = prefs.getInt(key, 0)

    // Attach fragment to handle callbacks
    val fm: FragmentManager = activity.supportFragmentManager
    val frag = (fm.findFragmentByTag(TAG) as? PermissionFragment)
      ?: PermissionFragment().also { f ->
        fm.beginTransaction().add(f, TAG).commitNow()
      }

    // Only show settings dialog if user has denied exactly DENIALS_NEEDED_FOR_SETTINGS times
    frag.startRequest(permissions, onResult, denialCount >= DENIALS_NEEDED_FOR_SETTINGS)
  }

  class PermissionFragment : Fragment() {
    private lateinit var permissions: Array<String>
    private var onResult: ((Boolean) -> Unit)? = null
    private var showSettingsDialog: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      retainInstance = true
    }

    fun startRequest(
      permissions: Array<String>,
      callback: (Boolean) -> Unit,
      showSettingsDialog: Boolean
    ) {
      this.permissions = permissions
      this.onResult = callback
      this.showSettingsDialog = showSettingsDialog
      requestPermissions(permissions, REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
    ) {
      if (requestCode != REQUEST_CODE) return

      val key = COUNT_PREFIX + this.permissions.joinToString("_")
      val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

      if (allGranted) {
        // Reset counter on success
        prefs.edit().remove(key).apply()
        onResult?.invoke(true)
      } else {
        // Increment denial count
        val newCount = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, newCount).apply()

        if (showSettingsDialog) {
          // Only show settings dialog if we've reached the denial threshold
          showGoToSettingsDialog()
        } else {
          // Otherwise just return the failure
          onResult?.invoke(false)
        }
      }
    }

    private fun showGoToSettingsDialog() {
      AlertDialog.Builder(requireContext())
        .setTitle("Permission Required")
        .setMessage("You've denied this permission $DENIALS_NEEDED_FOR_SETTINGS times. Please enable it in app settings.")
        .setPositiveButton("Settings") { _, _ ->
          val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", app.packageName, null)
          )
          startActivity(intent)
          onResult?.invoke(false)
        }
        .setNegativeButton("Cancel") { _, _ ->
          onResult?.invoke(false)
        }
        .setCancelable(false)
        .show()
    }
  }
}
