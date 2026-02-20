# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ==================== EXTREME SIZE OPTIMIZATION ====================

# Maximum optimization passes
-optimizationpasses 10
-allowaccessmodification
-repackageclasses ''
-mergeinterfacesaggressively
-overloadaggressively
-dontpreverify

# Aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable

# Remove all debug info
-dontwarn **
-dontusemixedcaseclassnames
-flattenpackagehierarchy ''

# Remove ALL logging (including warnings and errors for max size reduction)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
}

# Remove System.out/err prints
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
    public void printf(...);
}

# Remove Kotlin intrinsics completely
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void throwNpe(...);
    public static void throwJavaNpe(...);
    public static void throwUninitializedProperty(...);
    public static void throwUninitializedPropertyAccessException(...);
    public static void throwAssert(...);
    public static void throwIllegalArgument(...);
    public static void throwIllegalState(...);
}

# Remove Kotlin preconditions
-assumenosideeffects class kotlin.PreconditionsKt {
    public static void check(...);
    public static void checkNotNull(...);
    public static void require(...);
    public static void requireNotNull(...);
}

# Remove toString() for non-data classes (aggressive)
-assumenosideeffects class java.lang.Object {
    java.lang.String toString();
}

# Strip Kotlin metadata completely
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }

# Minimal attributes - only keep what's absolutely necessary
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions

# ==================== LIBRARY RULES (MINIMAL) ====================

# LibSU - minimal keep
-keep class com.topjohnwu.superuser.Shell { *; }
-keep class com.topjohnwu.superuser.Shell$* { *; }
-keep interface com.topjohnwu.superuser.** { *; }
-keepclassmembers class * extends com.topjohnwu.superuser.Shell$Initializer { *; }

# DataStore - minimal
-keep class androidx.datastore.preferences.** { *; }

# ==================== ANDROID RULES (MINIMAL) ====================

# Remove unused resources aggressively
-dontwarn org.xmlpull.**
-dontwarn org.kxml2.**

# ==================== COROUTINES OPTIMIZATION ====================
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# ==================== REMOVE REFLECTION OVERHEAD ====================
-dontwarn java.lang.invoke.**
-dontwarn sun.misc.**