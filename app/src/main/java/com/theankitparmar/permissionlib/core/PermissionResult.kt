package com.theankitparmar.permissionlib.core

/**
 * Sealed class representing all possible states of a permission.
 */
sealed class PermissionResult {

    /** The permission has been granted by the user. */
    data class Granted(val permission: String) : PermissionResult()

    /** The permission was denied by the user (can ask again). */
    data class Denied(val permission: String) : PermissionResult()

    /** The permission was permanently denied ("Don't ask again" selected). */
    data class PermanentlyDenied(val permission: String) : PermissionResult()
}

/**
 * Aggregated result for multiple permission requests.
 */
data class PermissionResults(
    val granted: List<String>,
    val denied: List<String>,
    val permanentlyDenied: List<String>
) {
    val allGranted: Boolean get() = denied.isEmpty() && permanentlyDenied.isEmpty()
    val anyPermanentlyDenied: Boolean get() = permanentlyDenied.isNotEmpty()
    val anyDenied: Boolean get() = denied.isNotEmpty()
}