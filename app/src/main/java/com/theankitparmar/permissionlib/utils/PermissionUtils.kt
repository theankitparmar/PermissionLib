/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.56
 */

package com.theankitparmar.permissionlib.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility functions for runtime permission management.
 *
 * These are low-level helpers used internally by [PermissionManager]
 * but also available to consumers directly.
 */
object PermissionUtils {

    private const val TAG = "PermissionUtils"

    /**
     * Check whether a single permission is currently granted.
     *
     * @param context    Application or Activity context
     * @param permission Android permission string (e.g. [android.Manifest.permission.CAMERA])
     * @return `true` if the permission is granted
     */
    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Check whether a permission is permanently denied.
     *
     * A permission is considered permanently denied when it has been denied before
     * AND [Activity.shouldShowRequestPermissionRationale] returns `false`,
     * meaning the user selected "Don't ask again".
     *
     * Note: this also returns `false` for a permission that has **never** been requested,
     * because `shouldShowRequestPermissionRationale` returns `false` in that case too.
     */
    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean {
        val isDenied          = !isGranted(activity, permission)
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        return isDenied && !shouldShowRationale
    }

    /**
     * Returns `true` if the system would show a rationale UI for this permission,
     * meaning it was denied at least once without "Don't ask again".
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    /**
     * Returns `true` if ALL provided permissions are granted.
     */
    fun areAllGranted(context: Context, permissions: List<String>): Boolean =
        permissions.all { isGranted(context, it) }

    /**
     * Filter a list of permissions down to only those not yet granted.
     */
    fun filterNotGranted(context: Context, permissions: List<String>): List<String> =
        permissions.filter { !isGranted(context, it) }

    /**
     * Open the application's system settings page so the user can manually
     * grant permissions that have been permanently denied.
     *
     * Logs a warning if the settings screen cannot be opened.
     *
     * @param context Application or Activity context
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Could not open app settings screen: ${e.message}")
            // Fallback: open generic app settings
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (fallback: ActivityNotFoundException) {
                Log.e(TAG, "Could not open manage applications settings either: ${fallback.message}")
            }
        }
    }

    /**
     * Partition [permissions] into three groups based on their current state.
     *
     * @return Triple of (granted, denied, permanentlyDenied) permission lists
     */
    fun partitionPermissions(
        activity: Activity,
        permissions: List<String>
    ): Triple<List<String>, List<String>, List<String>> {
        val granted           = mutableListOf<String>()
        val denied            = mutableListOf<String>()
        val permanentlyDenied = mutableListOf<String>()

        permissions.forEach { permission ->
            when {
                isGranted(activity, permission)           -> granted.add(permission)
                isPermanentlyDenied(activity, permission) -> permanentlyDenied.add(permission)
                else                                      -> denied.add(permission)
            }
        }

        return Triple(granted, denied, permanentlyDenied)
    }

    /**
     * Return a human-readable label for a permission string.
     * Strips the package prefix and converts underscores to spaces.
     *
     * Example: `android.permission.CAMERA` → `Camera`
     */
    fun readablePermissionName(permission: String): String =
        permission.substringAfterLast(".")
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
}
