# --- Default Android ProGuard rules ---
-dontwarn java.lang.invoke.StringConcatFactory

# --- Kotlin ---
-dontwarn kotlin.**
-keepclassmembers class kotlin.Metadata { *; }

# --- Jetpack Compose ---
# Compose ships its own consumer rules; keep runtime lambdas intact.
-keep class androidx.compose.runtime.** { *; }
-keep @androidx.compose.runtime.Immutable class * { *; }
-keep @androidx.compose.runtime.Stable class * { *; }

# --- Application ---
-keep class com.anonime.ime.** { *; }
-keep class com.anonime.AnonIMEApplication { *; }
