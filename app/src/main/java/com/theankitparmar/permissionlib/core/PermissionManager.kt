/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.56
 */

package com.theankitparmar.permissionlib.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.theankitparmar.permissionlib.dialog.PermissionDialog
import com.theankitparmar.permissionlib.utils.PermissionUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Central orchestrator for all runtime permission operations.
 *
 * Instances are created via the [with] factory methods and are
 * lifecycle-aware — they clean up automatically when the host Activity
 * or Fragment is destroyed, preventing memory leaks and crashes.
 *
 * ### Quick-start
 * ```kotlin
 * // In an Activity or Fragment
 * PermissionManager.with(this)
 *     .permissions(Manifest.permission.CAMERA)
 *     .onGranted  { startCamera() }
 *     .onDenied   { showDeniedMessage() }
 *     .onPermanentlyDenied { /* default dialog shown automatically */ }
 *     .request()
 * ```
 *
 * ### Coroutine
 * ```kotlin
 * lifecycleScope.launch {
 *     val result = PermissionManager.with(this@MainActivity)
 *         .permissions(Manifest.permission.CAMERA)
 *         .requestSuspend()
 *     if (result.allGranted) startCamera()
 * }
 * ```
 */
class PermissionManager private constructor(
    private val activity: FragmentActivity
) : DefaultLifecycleObserver {

    // ─── Dialog tag constants ────────────────────────────────────
    private companion object Tags {
        const val TAG_RATIONALE     = "permlib_rationale"
        const val TAG_PERM_DENIED   = "permlib_permanent_deny"
    }

    // ─── Activity Result launchers ───────────────────────────────
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var settingsLauncher: ActivityResultLauncher<Intent>? = null
    private var pendingRequest: PermissionRequest? = null

    // ─── Init / Lifecycle ────────────────────────────────────────

    init {
        activity.lifecycle.addObserver(this)
        registerLaunchers()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        pendingRequest = null
        permissionLauncher = null
        settingsLauncher = null
        activity.lifecycle.removeObserver(this)
    }

    private fun registerLaunchers() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { resultsMap: Map<String, Boolean> ->
            val request = pendingRequest ?: return@registerForActivityResult
            handlePermissionResults(resultsMap, request)
        }

        settingsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _: ActivityResult ->
            val request = pendingRequest ?: return@registerForActivityResult
            recheckAfterSettings(request)
        }
    }

    // ─── Factory / companion ─────────────────────────────────────

    companion object {

        /** Begin building a permission request scoped to the given [FragmentActivity]. */
        fun with(activity: FragmentActivity): PermissionRequest =
            PermissionRequest(PermissionManager(activity))

        /**
         * Begin building a permission request scoped to the given [Fragment].
         * The request is lifecycle-scoped to the fragment's host activity.
         */
        fun with(fragment: Fragment): PermissionRequest =
            PermissionRequest(PermissionManager(fragment.requireActivity() as FragmentActivity))

        // ─── Static helpers ──────────────────────────────────────

        /** `true` when [permission] is currently granted. */
        fun isPermissionGranted(context: Context, permission: String): Boolean =
            PermissionUtils.isGranted(context, permission)

        /** `true` when [permission] is permanently denied ("Don't ask again" selected). */
        fun isPermanentlyDenied(activity: Activity, permission: String): Boolean =
            PermissionUtils.isPermanentlyDenied(activity, permission)

        /** `true` when every permission in [permissions] is granted. */
        fun areAllGranted(context: Context, permissions: List<String>): Boolean =
            PermissionUtils.areAllGranted(context, permissions)

        /** Open the app's system settings so the user can manually grant permissions. */
        fun openAppSettings(context: Context): Unit =
            PermissionUtils.openAppSettings(context)
    }

    // ─── Internal execution (called by PermissionRequest) ────────

    internal fun executeRequest(request: PermissionRequest) {
        // Fast-path: everything already granted
        val notGranted = PermissionUtils.filterNotGranted(activity, request.permissions)
        if (notGranted.isEmpty()) {
            dispatchGranted(request, request.permissions)
            return
        }

        pendingRequest = request

        // Show rationale before the system dialog if the developer configured one
        val needsRationale = notGranted.any { PermissionUtils.shouldShowRationale(activity, it) }
        if (needsRationale && request.rationaleMessage != null) {
            showRationaleDialog(request, notGranted)
        } else {
            launchPermissionRequest(notGranted.toTypedArray())
        }
    }

    internal suspend fun executeRequestSuspend(request: PermissionRequest): PermissionResults =
        suspendCancellableCoroutine { cont ->
            request.onResult { results ->
                if (cont.isActive) cont.resume(results)
            }
            executeRequest(request)
        }

    // ─── Internal helpers ────────────────────────────────────────

    private fun launchPermissionRequest(permissions: Array<String>) {
        requireNotNull(permissionLauncher) {
            "PermissionManager: ActivityResultLauncher not initialised. " +
                "Ensure PermissionManager.with() is called before the Activity reaches STARTED state."
        }.launch(permissions)
    }

    private fun handlePermissionResults(
        resultsMap: Map<String, Boolean>,
        request: PermissionRequest
    ) {
        val granted      = resultsMap.filter { it.value }.keys.toList()
        val allDenied    = resultsMap.filter { !it.value }.keys.toList()

        val permanentlyDenied = allDenied.filter { PermissionUtils.isPermanentlyDenied(activity, it) }
        val softDenied        = allDenied.filter { !PermissionUtils.isPermanentlyDenied(activity, it) }

        // retryOnDenied: automatically re-request soft-denied permissions once
        if (request.retryOnDenied && !request.hasRetried && softDenied.isNotEmpty()) {
            request.hasRetried = true
            pendingRequest = request
            launchPermissionRequest(softDenied.toTypedArray())
            return
        }

        val results = PermissionResults(
            granted          = granted,
            denied           = softDenied,
            permanentlyDenied = permanentlyDenied
        )

        if (granted.isNotEmpty() && softDenied.isEmpty() && permanentlyDenied.isEmpty()) {
            dispatchGranted(request, granted)
            return
        }

        if (softDenied.isNotEmpty()) {
            request.onDenied?.invoke(softDenied)
            request.callback?.onDenied(softDenied)
        }

        if (permanentlyDenied.isNotEmpty()) {
            request.onPermanentlyDenied?.invoke(permanentlyDenied)
            request.callback?.onPermanentlyDenied(permanentlyDenied)
            if (request.showDialogOnPermanentDeny) {
                showPermanentlyDeniedDialog(request)
            }
        }

        // Always dispatch aggregate result
        request.onResult?.invoke(results)
    }

    private fun dispatchGranted(request: PermissionRequest, permissions: List<String>) {
        request.onGranted?.invoke(permissions)
        request.callback?.onGranted(permissions)
        request.onResult?.invoke(
            PermissionResults(granted = permissions, denied = emptyList(), permanentlyDenied = emptyList())
        )
    }

    // ─── Dialog helpers ──────────────────────────────────────────

    private fun showRationaleDialog(request: PermissionRequest, permissions: List<String>) {
        PermissionDialog.newInstance(
            config = request.dialogConfig.copy(
                title             = request.rationaleTitle  ?: request.dialogConfig.title,
                message           = request.rationaleMessage ?: request.dialogConfig.message,
                positiveButtonText = "Continue",
                negativeButtonText = "Cancel"
            ),
            onPositive = { launchPermissionRequest(permissions.toTypedArray()) },
            onNegative = {
                request.onDenied?.invoke(permissions)
                request.callback?.onDenied(permissions)
            }
        ).show(activity.supportFragmentManager, TAG_RATIONALE)
    }

    private fun showPermanentlyDeniedDialog(request: PermissionRequest) {
        PermissionDialog.newInstance(
            config     = request.dialogConfig,
            onPositive = { openSettingsForResult() },
            onNegative = null
        ).show(activity.supportFragmentManager, TAG_PERM_DENIED)
    }

    private fun openSettingsForResult() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        settingsLauncher?.launch(intent)
    }

    private fun recheckAfterSettings(request: PermissionRequest) {
        val (granted, denied, permanentlyDenied) =
            PermissionUtils.partitionPermissions(activity, request.permissions)

        val results = PermissionResults(
            granted           = granted,
            denied            = denied,
            permanentlyDenied = permanentlyDenied
        )

        if (results.allGranted) {
            dispatchGranted(request, granted)
        } else {
            if (denied.isNotEmpty()) {
                request.onDenied?.invoke(denied)
                request.callback?.onDenied(denied)
            }
            if (permanentlyDenied.isNotEmpty()) {
                request.onPermanentlyDenied?.invoke(permanentlyDenied)
                request.callback?.onPermanentlyDenied(permanentlyDenied)
            }
            request.onResult?.invoke(results)
        }
    }
}
