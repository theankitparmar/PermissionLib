# PermissionLib — ProGuard rules for the library AAR build

# Keep all public API classes so they survive minification in the library build
-keep public class com.theankitparmar.permissionlib.core.** { public *; }
-keep public class com.theankitparmar.permissionlib.dialog.** { public *; }
-keep public class com.theankitparmar.permissionlib.utils.** { public *; }
-keep public class com.theankitparmar.permissionlib.extensions.** { public *; }

# Keep the PermissionCallback interface so consumer implementations are not stripped
-keep interface com.theankitparmar.permissionlib.core.PermissionCallback { *; }

# Keep DialogFragment subclass (required for FragmentManager restoration)
-keep class com.theankitparmar.permissionlib.dialog.PermissionDialog { *; }

# Keep Kotlin data classes used as public result types
-keep class com.theankitparmar.permissionlib.core.PermissionResult { *; }
-keep class com.theankitparmar.permissionlib.core.PermissionResults { *; }
-keep class com.theankitparmar.permissionlib.dialog.DialogConfig { *; }

# Preserve source file names and line numbers for readable stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
