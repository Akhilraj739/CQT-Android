# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }

# Coil
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# Keep the QuickTileService classes since they are referenced by name in the code and Manifest
-keep class com.artic.cqt.QuickTileService* { *; }

# Keep Activity/Service classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Material Icons Extended (to prevent stripping if used via reflection or string names)
-keep class androidx.compose.material.icons.** { *; }
