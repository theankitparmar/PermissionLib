/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.56
 */

package com.theankitparmar.permissionlib.core

import android.graphics.Color
import com.theankitparmar.permissionlib.dialog.DialogConfig

/**
 * Fluent builder for constructing a permission request.
 *
 * Obtain an instance via [PermissionManager.with], then chain methods and
 * call [request] (or the suspending [requestSuspend]) to execute.
 *
 * ```kotlin
 * PermissionManager.with(activity)
 *     .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
 *     .onGranted  { startCamera() }
 *     .onDenied   { showToast("Denied") }
 *     .onPermanentlyDenied { /* default dialog shown automatically */ }
 *     .request()
 * ```
 */
class PermissionRequest internal constructor(
    private val manager: PermissionManager
) {
    internal val permissions          = mutableListOf<String>()
    internal var onGranted:            ((List<String>) -> Unit)? = null
    internal var onDenied:             ((List<String>) -> Unit)? = null
    internal var onPermanentlyDenied:  ((List<String>) -> Unit)? = null
    internal var onResult:             ((PermissionResults) -> Unit)? = null
    internal var dialogConfig:         DialogConfig = DialogConfig()
    internal var showDialogOnPermanentDeny: Boolean = true
    internal var rationaleTitle:       String? = null
    internal var rationaleMessage:     String? = null
    internal var retryOnDenied:        Boolean = false
    internal var hasRetried:           Boolean = false   // tracked by PermissionManager
    internal var callback:             PermissionCallback? = null

    // ─── Fluent API ──────────────────────────────────────────────

    /** Specify one or more permissions to request. */
    fun permissions(vararg perms: String): PermissionRequest {
        permissions.addAll(perms)
        return this
    }

    /** Specify permissions from a list. */
    fun permissions(perms: List<String>): PermissionRequest {
        permissions.addAll(perms)
        return this
    }

    /** Invoked when ALL requested permissions are granted. */
    fun onGranted(block: (List<String>) -> Unit): PermissionRequest {
        onGranted = block
        return this
    }

    /** Invoked when one or more permissions are denied (can ask again). */
    fun onDenied(block: (List<String>) -> Unit): PermissionRequest {
        onDenied = block
        return this
    }

    /** Invoked when one or more permissions are permanently denied. */
    fun onPermanentlyDenied(block: (List<String>) -> Unit): PermissionRequest {
        onPermanentlyDenied = block
        return this
    }

    /** Invoked with the full [PermissionResults] regardless of outcome. */
    fun onResult(block: (PermissionResults) -> Unit): PermissionRequest {
        onResult = block
        return this
    }

    /** Attach a structured [PermissionCallback]. */
    fun callback(cb: PermissionCallback): PermissionRequest {
        callback = cb
        return this
    }

    /**
     * Customise the permanently-denied dialog.
     *
     * All parameters are optional; unspecified parameters fall back to the
     * library defaults defined in [DialogConfig].
     */
    fun dialogConfig(
        title:           String = "Permission Required",
        message:         String = "This permission is required for the app to function properly.",
        positiveText:    String = "Open Settings",
        negativeText:    String = "Cancel",
        positiveColor:   Int    = Color.parseColor("#2196F3"),
        backgroundColor: Int    = Color.WHITE,
        titleFontSize:   Float  = 18f,
        messageFontSize: Float  = 14f
    ): PermissionRequest {
        dialogConfig = DialogConfig(
            title              = title,
            message            = message,
            positiveButtonText = positiveText,
            negativeButtonText = negativeText,
            positiveButtonColor = positiveColor,
            backgroundColor    = backgroundColor,
            titleFontSize      = titleFontSize,
            messageFontSize    = messageFontSize
        )
        return this
    }

    /** Apply a pre-built [DialogConfig] object. */
    fun dialogConfig(config: DialogConfig): PermissionRequest {
        dialogConfig = config
        return this
    }

    /** Suppress the automatic permanently-denied dialog. */
    fun disableDefaultDialog(): PermissionRequest {
        showDialogOnPermanentDeny = false
        return this
    }

    /**
     * Show a rationale dialog before the system permission prompt when the
     * permission was previously denied (but not permanently).
     */
    fun rationale(title: String, message: String): PermissionRequest {
        rationaleTitle   = title
        rationaleMessage = message
        return this
    }

    /**
     * When `true`, soft-denied permissions (user can be asked again) are
     * automatically re-requested once before [onDenied] is called.
     *
     * Useful for cases where the user accidentally dismisses the system dialog.
     */
    fun retryOnDenied(retry: Boolean = true): PermissionRequest {
        retryOnDenied = retry
        return this
    }

    // ─── Terminal operations ─────────────────────────────────────

    /** Execute the permission request (fire-and-forget via callbacks). */
    fun request() {
        require(permissions.isNotEmpty()) {
            "PermissionRequest: at least one permission must be specified via .permissions()"
        }
        manager.executeRequest(this)
    }

    /**
     * Execute the permission request as a suspending call.
     * Must be invoked from a coroutine scope.
     *
     * ```kotlin
     * lifecycleScope.launch {
     *     val result = PermissionManager.with(this@Activity)
     *         .permissions(Manifest.permission.CAMERA)
     *         .requestSuspend()
     *     if (result.allGranted) startCamera()
     * }
     * ```
     */
    suspend fun requestSuspend(): PermissionResults {
        require(permissions.isNotEmpty()) {
            "PermissionRequest: at least one permission must be specified via .permissions()"
        }
        return manager.executeRequestSuspend(this)
    }
}
