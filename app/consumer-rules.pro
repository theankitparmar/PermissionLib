# PermissionLib — Consumer ProGuard rules
# These rules are embedded in the AAR and applied automatically to any app
# that depends on PermissionLib. No manual configuration needed.

# Keep all public PermissionLib classes from being renamed or removed
-keep public class com.theankitparmar.permissionlib.** { public protected *; }

# Keep the PermissionCallback interface and all its implementors
-keep interface com.theankitparmar.permissionlib.core.PermissionCallback { *; }
-keep class * implements com.theankitparmar.permissionlib.core.PermissionCallback { *; }

# Keep DialogFragment so FragmentManager can restore it after config changes
-keep class com.theankitparmar.permissionlib.dialog.PermissionDialog { *; }
