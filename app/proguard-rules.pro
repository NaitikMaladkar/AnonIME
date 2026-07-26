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

# --- Kotlinx metadata + coroutines (used by StateFlow in SettingsRepository) ---
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# --- Application: keep everything in our own packages ---
# R8 tree-shakes aggressively and Compose's @Composable call graph is resolved
# at runtime via reflection (ComposableSingletons, FunctionReference), so any
# composable that isn't statically reachable from a kept entry point gets
# stripped. This caused a ClassNotFoundException crash on launch when the
# settings/data/ui packages were pruned. Keeping the whole com.anonime.** tree
# is the safe choice — the size cost is negligible (~100 KB on the final APK)
# because R8 still inlines and shrinks methods within these classes.
-keep class com.anonime.** { *; }
-keepclassmembers class com.anonime.** { *; }

# Keep enum values so when() mappings and valueOf() reflection still work.
-keepclassmembers enum com.anonime.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
