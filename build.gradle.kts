plugins {
    // ★ Bumped AGP 8.7.3 → 8.13.2 so we can use compileSdk=37 (Android 16).
    //   Required because TagLib (io.github.amanrajaryan:TagLib:1.0.2) declares
    //   minCompileSdk=37 in its AAR metadata. AGP 8.7.3 max recommended was 35.
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
}
