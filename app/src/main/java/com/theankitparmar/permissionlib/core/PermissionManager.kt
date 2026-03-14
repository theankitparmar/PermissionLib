/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.57
 */

package com.theankitparmar.permissionlib.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.theankitparmar.permissionlib.R
import com.theankitparmar.permissionlib.dialog.PermissionDialog
import com.theankitparmar.permissionlib.utils.PermissionUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Central orchestrator for all runtime permission operations.
 *
 * Instances are created via the [with] factory methods and are lifecycle-aware —
 * they clean up automatically when the host Activity or Fragment is destroyed,
 * preventing memory leaks and stale callbacks.
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
    /** Used to register [ActivityResultLauncher]s — either a [FragmentActivity] or a [Fragment]. */
    private val caller: ActivityResultCaller,
    /** Host activity — used for context, `isFinishing`/`isDestroyed` checks, and settings intent. */
    private val activity: FragmentActivity,
    /** Fragment manager to use for dialogs — [FragmentActivity.supportFragmentManager] or
     *  [Fragment.childFragmentManager] so dialogs are scoped to the correct back-stack. */
    private val fragmentManager: FragmentManager,
    lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {

    // ─── Activity Result launchers ───────────────────────────────
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var settingsLauncher:   ActivityResultLauncher<Intent>? = null
    private var pendingRequest:     PermissionRequest? = null

    // ─── Init / Lifecycle ────────────────────────────────────────

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        registerLaunchers()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        pendingRequest = null
        permissionLauncher = null
        settingsLauncher = null
        owner.lifecycle.removeObserver(this)
    }

    private fun registerLaunchers() {
        permissionLauncher = caller.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { resultsMap: Map<String, Boolean> ->
            val request = pendingRequest ?: return@registerForActivityResult
            handlePermissionResults(resultsMap, request)
        }

        settingsLauncher = caller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _: ActivityResult ->
            val request = pendingRequest ?: return@registerForActivityResult
            recheckAfterSettings(request)
        }
    }

    // ─── Factory / companion ─────────────────────────────────────

    companion object {
        private const val TAG             = "PermissionManager"
        private const val TAG_RATIONALE   = "permlib_rationale"
        private const val TAG_PERM_DENIED = "permlib_permanent_deny"


        /**
         * Begin building a permission request scoped to the given [FragmentActivity].
         * The manager is lifecycle-bound to the activity.
         */
        fun with(activity: FragmentActivity): PermissionRequest =
            PermissionRequest(
                PermissionManager(
                    caller          = activity,
                    activity        = activity,
                    fragmentManager = activity.supportFragmentManager,
                    lifecycleOwner  = activity
                )
            )

        /**
         * Begin building a permission request scoped to the given [Fragment].
         *
         * The manager observes the **fragment's** lifecycle (not the activity's), so
         * cleanup happens when the fragment is destroyed — even if the host activity
         * survives. Dialogs are shown on the fragment's [Fragment.childFragmentManager]
         * to match the fragment's back-stack scope.
         */
        fun with(fragment: Fragment): PermissionRequest =
            PermissionRequest(
                PermissionManager(
                    caller          = fragment,
                    activity        = fragment.requireActivity() as FragmentActivity,
                    fragmentManager = fragment.childFragmentManager,
                    lifecycleOwner  = fragment
                )
            )

        // ─── Static helpers ──────────────────────────────────────

        /** `true` when [permission] is currently granted. */
        fun isPermissionGranted(context: Context, permission: String): Boolean =
            PermissionUtils.isGranted(context, permission)

        /**
         * `true` when [permission] is permanently denied ("Don't ask again" selected).
         *
         * **Note:** This also returns `true` for permissions that have **never** been
         * requested, because [android.app.Activity.shouldShowRequestPermissionRationale]
         * returns `false` in both cases. Only call this after a permission has been
         * denied at least once.
         */
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

    @MainThread
    internal fun executeRequest(request: PermissionRequest) {
        // Empty permission list → treat as all granted.
        // This happens when a PermissionGroup returns emptyArray() for the current API level
        // (e.g. NOTIFICATIONS on API < 33, ACTIVITY_RECOGNITION on API < 29).
        if (request.permissions.isEmpty()) {
            dispatchGranted(request, emptyList())
            return
        }

        // Fast-path: everything already granted
        val notGranted = PermissionUtils.filterNotGranted(activity, request.permissions)
        if (notGranted.isEmpty()) {
            dispatchGranted(request, request.permissions)
            return
        }

        // Guard against concurrent requests on the same manager instance
        if (pendingRequest != null && pendingRequest !== request) {
            Log.w(TAG, "A permission request is already in progress. Ignoring concurrent request.")
            return
        }

        pendingRequest = request

        // Always launch the system dialog first.
        // Rationale (if configured) is shown in handlePermissionResults *after* the first denial,
        // never before the system prompt — matching the Android-recommended permission flow.
        launchPermissionRequest(notGranted.toTypedArray())
    }

    internal suspend fun executeRequestSuspend(request: PermissionRequest): PermissionResults =
        suspendCancellableCoroutine { cont ->
            // Use a dedicated internal slot so the user's onResult callback is NOT overwritten.
            request.internalResultCallback = { results ->
                if (cont.isActive) cont.resume(results)
            }
            // Clean up if the calling coroutine is cancelled (e.g. Activity back-pressed,
            // scope destroyed) so pendingRequest doesn't linger and fire stale callbacks.
            cont.invokeOnCancellation {
                request.internalResultCallback = null
                pendingRequest = null
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
        // Partition denied permissions in a single pass (avoids double-invocation of
        // isPermanentlyDenied, which internally calls isGranted + shouldShowRationale).
        val (permanentlyDenied, softDenied) = resultsMap
            .filter { !it.value }
            .keys
            .partition { PermissionUtils.isPermanentlyDenied(activity, it) }

        // retryOnDenied: automatically re-request soft-denied permissions once
        if (request.retryOnDenied && !request.hasRetried && softDenied.isNotEmpty()) {
            request.hasRetried = true
            launchPermissionRequest(softDenied.toTypedArray())
            return
        }

        // Show rationale after first soft denial — only if configured and not yet shown this cycle.
        // The rationale positive button re-launches the system dialog; negative dispatches denied.
        if (softDenied.isNotEmpty() && request.rationaleMessage != null && !request.hasShownRationale) {
            request.hasShownRationale = true
            showRationaleDialog(request, softDenied)
            return
        }

        // Re-check all originally-requested permissions — some may have been granted
        // before the dialog (already-granted members of a group not in resultsMap).
        val allNowGranted = PermissionUtils.filterNotGranted(activity, request.permissions).isEmpty()
        if (allNowGranted) {
            dispatchGranted(request, request.permissions)
            return
        }

        // Build the aggregate result with the full granted list
        val granted = request.permissions.filter { PermissionUtils.isGranted(activity, it) }
        val results = PermissionResults(
            granted           = granted,
            denied            = softDenied,
            permanentlyDenied = permanentlyDenied
        )

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

        dispatchResults(request, results)
    }

    private fun dispatchGranted(request: PermissionRequest, permissions: List<String>) {
        request.onGranted?.invoke(permissions)
        request.callback?.onGranted(permissions)
        val results = PermissionResults(
            granted           = permissions,
            denied            = emptyList(),
            permanentlyDenied = emptyList()
        )
        dispatchResults(request, results)
    }

    /**
     * Clears [pendingRequest] then invokes both the user-facing [PermissionRequest.onResult]
     * and the internal coroutine callback. Order: clear state first so any re-entrant
     * request triggered from within a callback starts with a clean slate.
     */
    private fun dispatchResults(request: PermissionRequest, results: PermissionResults) {
        pendingRequest = null
        request.onResult?.invoke(results)
        request.internalResultCallback?.invoke(results)
        request.internalResultCallback = null
    }

    // ─── Dialog safety ───────────────────────────────────────────

    /**
     * Returns `true` only when it is safe to commit a fragment transaction.
     * Prevents [IllegalStateException] from `show()` after `onSaveInstanceState`.
     */
    private fun canShowDialog(): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        if (fragmentManager.isStateSaved) return false
        return true
    }

    // ─── Dialog helpers ──────────────────────────────────────────

    private fun showRationaleDialog(request: PermissionRequest, permissions: List<String>) {
        if (!canShowDialog()) return
        PermissionDialog.newInstance(
            config = request.dialogConfig.copy(
                title              = request.rationaleTitle   ?: request.dialogConfig.title,
                message            = request.rationaleMessage ?: request.dialogConfig.message,
                positiveButtonText = request.rationalePositiveText
                                         ?: activity.getString(R.string.permlib_btn_continue),
                negativeButtonText = request.rationaleNegativeText
                                         ?: activity.getString(R.string.permlib_btn_cancel)
            ),
            onPositive = { launchPermissionRequest(permissions.toTypedArray()) },
            onNegative = {
                // User declined the rationale — treat as a soft denial and clean up state.
                // Must call dispatchResults so coroutine callers (requestSuspend) are resolved
                // and pendingRequest is cleared; omitting this causes a permanent hang/leak.
                val granted = request.permissions.filter { PermissionUtils.isGranted(activity, it) }
                val results = PermissionResults(
                    granted           = granted,
                    denied            = permissions,
                    permanentlyDenied = emptyList()
                )
                request.onDenied?.invoke(permissions)
                request.callback?.onDenied(permissions)
                dispatchResults(request, results)
            }
        ).show(fragmentManager, TAG_RATIONALE)
    }

    private fun showPermanentlyDeniedDialog(request: PermissionRequest) {
        if (!canShowDialog()) return
        PermissionDialog.newInstance(
            config     = request.dialogConfig,
            onPositive = { openSettingsForResult() },
            onNegative = null
        ).show(fragmentManager, TAG_PERM_DENIED)
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
            dispatchGranted(request, request.permissions)
        } else {
            if (denied.isNotEmpty()) {
                request.onDenied?.invoke(denied)
                request.callback?.onDenied(denied)
            }
            if (permanentlyDenied.isNotEmpty()) {
                request.onPermanentlyDenied?.invoke(permanentlyDenied)
                request.callback?.onPermanentlyDenied(permanentlyDenied)
            }
            dispatchResults(request, results)
        }
    }
}
