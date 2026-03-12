/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.56
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

    /** Precise and approximate location. */
    val LOCATION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /** Background location (requires LOCATION group first on API 29+). */
    val LOCATION_BACKGROUND = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    /**
     * Storage – scoped to API level.
     * - API 33+: READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO
     * - Below:   READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
     */
    val STORAGE: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            @Suppress("DEPRECATION")
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

    /** Images only (API 33+: READ_MEDIA_IMAGES, below: READ_EXTERNAL_STORAGE). */
    val STORAGE_IMAGES: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            @Suppress("DEPRECATION")
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
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
     * Bluetooth – scoped to API level.
     * - API 31+: BLUETOOTH_SCAN, BLUETOOTH_CONNECT
     * - Below:   BLUETOOTH (deprecated)
     */
    val BLUETOOTH: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            @Suppress("DEPRECATION")
            arrayOf(Manifest.permission.BLUETOOTH)
        }

    /** Notification permission (API 33+ only; returns empty array below). */
    val NOTIFICATIONS: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    /**
     * Body sensors (heart rate, etc.).
     * - API 33+: BODY_SENSORS + BODY_SENSORS_BACKGROUND
     * - Below:   BODY_SENSORS only
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

    /** Activity recognition (step counter, detected activity). Requires API 29+. */
    val ACTIVITY_RECOGNITION: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            emptyArray()
        }
}
