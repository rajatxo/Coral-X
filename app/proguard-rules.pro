# =============================================================================
# Coral X — ProGuard / R8 rules
# =============================================================================
# Clean slate. Rules will be added as dependencies are introduced.
# =============================================================================

# --- Kotlin metadata ---
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.Metadata *;
}

# --- Compose ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Generic safe fallbacks ---
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.lang.model.**
