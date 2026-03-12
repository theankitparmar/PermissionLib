/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.56
 */

package com.theankitparmar.permissionlib.dialog

import android.graphics.Color

/**
 * Configuration data class for the permanently-denied permission dialog.
 *
 * All parameters have sensible defaults so the library works
 * out-of-the-box without any configuration.
 */
data class DialogConfig(
    val title: String = "Permission Required",
    val message: String = "This permission is required for the app to function properly. Please grant it from App Settings.",
    val positiveButtonText: String = "Open Settings",
    val negativeButtonText: String = "Cancel",
    val positiveButtonColor: Int = Color.parseColor("#2196F3"),
    val negativeButtonColor: Int = Color.parseColor("#757575"),
    val backgroundColor: Int = Color.WHITE,
    val titleFontSize: Float = 18f,
    val messageFontSize: Float = 14f,
    val titleTextColor: Int = Color.parseColor("#212121"),
    val messageTextColor: Int = Color.parseColor("#616161"),
    val isCancelable: Boolean = true
)