/**
 * PermissionLib — Android Runtime Permission Library
 * Author  : Ankit Parmar
 * GitHub  : https://github.com/theankitparmar
 * Email   : codewithankit@gmail.com
 * Version : 1.0.56
 */

package com.theankitparmar.permissionlib.core

/**
 * Callback interface for permission request results.
 * Implement this to handle all permission states in a single callback.
 */
interface PermissionCallback {
    fun onGranted(permissions: List<String>)
    fun onDenied(permissions: List<String>)
    fun onPermanentlyDenied(permissions: List<String>)
}