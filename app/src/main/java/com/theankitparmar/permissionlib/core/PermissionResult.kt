package com.theankitparmar.permissionlib.core

/**
 * Sealed class representing the state of a single permission.
 * Use [PermissionResults.toResultList] to convert an aggregate result into
 * a per-permission list of these states.
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
    val allGranted: Boolean          get() = denied.isEmpty() && permanentlyDenied.isEmpty()
    val anyPermanentlyDenied: Boolean get() = permanentlyDenied.isNotEmpty()
    val anyDenied: Boolean            get() = denied.isNotEmpty()
}

/**
 * Convert an aggregate [PermissionResults] into a per-permission list of [PermissionResult] states.
 *
 * ```kotlin
 * val result = PermissionManager.with(this)
 *     .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
 *     .requestSuspend()
 *
 * result.toResultList().forEach { state ->
 *     when (state) {
 *         is PermissionResult.Granted           -> enableFeature(state.permission)
 *         is PermissionResult.Denied            -> showRationale(state.permission)
 *         is PermissionResult.PermanentlyDenied -> showSettingsPrompt(state.permission)
 *     }
 * }
 * ```
 */
fun PermissionResults.toResultList(): List<PermissionResult> = buildList {
    granted.forEach           { add(PermissionResult.Granted(it)) }
    denied.forEach            { add(PermissionResult.Denied(it)) }
    permanentlyDenied.forEach { add(PermissionResult.PermanentlyDenied(it)) }
}
