/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.57
 */

package com.theankitparmar.permissionlib.core

import android.Manifest
import android.os.Build

/**
 * Pre-defined permission groups for common Android use-cases.
 *
 * Convenience constants that bundle related permissions together.
 * Pass to [PermissionRequest.permissions] just like individual permissions.
 *
 * ```kotlin
 * PermissionManager.with(this)
 *     .permissions(*PermissionGroup.CAMERA_AND_MIC)
 *     .onGranted { startVideoCall() }
 *     .request()
 * ```
 *
 * **Important — empty arrays:** Some groups return an empty array on older API levels
 * (e.g. [NOTIFICATIONS] below API 33, [ACTIVITY_RECOGNITION] below API 29).
 * When spread into [PermissionRequest.permissions], an empty array results in an
 * immediate [PermissionRequest.onGranted] callback — no system dialog is shown.
 */
object PermissionGroup {

    /** Camera access only. */
    val CAMERA = arrayOf(Manifest.permission.CAMERA)

    /** Microphone / audio recording. */
    val MICROPHONE = arrayOf(Manifest.permission.RECORD_AUDIO)

    /** Camera + microphone (video calls, live streaming). */
    val CAMERA_AND_MIC = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    /** Precise and approximate foreground location. */
    val LOCATION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Background location **only**.
     *
     * **Must be requested in a separate, subsequent call AFTER [LOCATION] is already granted.**
     * Since Android 11 (API 30) the system silently ignores [Manifest.permission.ACCESS_BACKGROUND_LOCATION]
     * if it is bundled together with foreground location permissions in the same request.
     *
     * Typical usage:
     * ```kotlin
     * // Step 1 — grant foreground location
     * PermissionManager.with(this)
     *     .permissions(*PermissionGroup.LOCATION)
     *     .onGranted {
     *         // Step 2 — only then request background location
     *         PermissionManager.with(this)
     *             .permissions(*PermissionGroup.LOCATION_BACKGROUND)
     *             .onGranted { startBackgroundTracking() }
     *             .request()
     *     }
     *     .request()
     * ```
     */
    val LOCATION_BACKGROUND = arrayOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    /**
     * Storage — scoped to API level.
     * - API 34+ : READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, READ_MEDIA_VISUAL_USER_SELECTED
     * - API 33  : READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO
     * - Below   : READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
     */
    val STORAGE: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            else -> @Suppress("DEPRECATION") arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

    /**
     * Images only — scoped to API level.
     * - API 34+ : READ_MEDIA_IMAGES + READ_MEDIA_VISUAL_USER_SELECTED (partial photo access)
     * - API 33  : READ_MEDIA_IMAGES
     * - Below   : READ_EXTERNAL_STORAGE
     */
    val STORAGE_IMAGES: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            else -> @Suppress("DEPRECATION") arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

    /** Phone call & state permissions. */
    val PHONE = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )

    /** Contacts read/write. */
    val CONTACTS = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )

    /** Calendar read/write. */
    val CALENDAR = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    /**
     * Bluetooth — scoped to API level.
     * - API 31+ : BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE
     * - Below   : BLUETOOTH (deprecated)
     */
    val BLUETOOTH: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            @Suppress("DEPRECATION")
            arrayOf(Manifest.permission.BLUETOOTH)
        }

    /**
     * Notification permission — API 33+ only.
     * Returns an **empty array** on API 32 and below (notifications are granted by default).
     * Passing an empty array to [PermissionRequest.permissions] triggers [PermissionRequest.onGranted]
     * immediately without showing a system dialog.
     */
    val NOTIFICATIONS: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    /**
     * Body sensors (heart rate, etc.) — scoped to API level.
     * - API 33+ : BODY_SENSORS + BODY_SENSORS_BACKGROUND
     * - Below   : BODY_SENSORS only
     */
    val BODY_SENSORS: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.BODY_SENSORS_BACKGROUND
            )
        } else {
            arrayOf(Manifest.permission.BODY_SENSORS)
        }

    /**
     * Activity recognition (step counter, detected activity) — API 29+ only.
     * Returns an **empty array** on API 28 and below.
     */
    val ACTIVITY_RECOGNITION: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            emptyArray()
        }
}
