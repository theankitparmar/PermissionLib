package com.theankitparmar.permissionlib.extensions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// ─── Context extensions ──────────────────────────────────────────────────────

/**
 * Returns `true` if [permission] is currently granted.
 *
 * ```kotlin
 * if (context.isGranted(Manifest.permission.CAMERA)) startCamera()
 * ```
 */
fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Returns `true` if every permission in [permissions] is currently granted.
 */
fun Context.areAllGranted(vararg permissions: String): Boolean =
    permissions.all { isGranted(it) }

/**
 * Returns the subset of [permissions] that are NOT yet granted.
 */
fun Context.filterDenied(vararg permissions: String): List<String> =
    permissions.filterNot { isGranted(it) }