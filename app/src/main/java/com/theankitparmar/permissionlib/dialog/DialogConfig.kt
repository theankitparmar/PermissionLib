/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.57
 */

package com.theankitparmar.permissionlib.dialog

import android.graphics.Color

/**
 * Configuration data class for the permanently-denied permission dialog.
 *
 * All parameters have sensible defaults so the library works
 * out-of-the-box without any configuration.
 *
 * Color defaults are stored as pre-computed integer constants in [Defaults]
 * so that [Color.parseColor] string parsing is never performed at instantiation time.
 */
data class DialogConfig(
    val title: String               = "Permission Required",
    val message: String             = "This permission is required for the app to function properly. Please grant it from App Settings.",
    val positiveButtonText: String  = "Open Settings",
    val negativeButtonText: String  = "Cancel",
    val positiveButtonColor: Int    = Defaults.COLOR_PRIMARY,
    val negativeButtonColor: Int    = Defaults.COLOR_GRAY,
    val backgroundColor: Int        = Color.WHITE,
    val titleFontSize: Float        = 18f,
    val messageFontSize: Float      = 14f,
    val titleTextColor: Int         = Defaults.COLOR_TEXT_PRIMARY,
    val messageTextColor: Int       = Defaults.COLOR_TEXT_SECONDARY,
    val isCancelable: Boolean       = true
) {
    /**
     * Pre-computed color constants.
     * Evaluated once at class-load time — avoids [Color.parseColor] overhead on every
     * [DialogConfig] instantiation or [copy] call.
     */
    object Defaults {
        /** Material Blue 500 — #2196F3 */
        @JvmField val COLOR_PRIMARY        = Color.rgb(33, 150, 243)
        /** Material Gray 600 — #757575 */
        @JvmField val COLOR_GRAY           = Color.rgb(117, 117, 117)
        /** Near-black text — #212121 */
        @JvmField val COLOR_TEXT_PRIMARY   = Color.rgb(33, 33, 33)
        /** Medium-gray text — #616161 */
        @JvmField val COLOR_TEXT_SECONDARY = Color.rgb(97, 97, 97)
    }
}
