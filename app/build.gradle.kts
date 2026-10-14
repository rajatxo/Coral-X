import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ─── Signing keystore — read from keystore.properties (gitignored) ───
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.rajatxo.coral"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rajatxo.coralx"
        // ★ Bumped from 24 → 29 to support accompanist-lyrics-ui (karaoke lyrics UI).
        //   The library's AAR declares minSdk=29 (Android 10+). Lower minSdk
        //   causes AGP to fail the build. Most active Android devices today are
        //   API 29+, so this is a safe bump.
        minSdk = 29
        targetSdk = 35
        versionCode = 582
        versionName = "0.5.82"
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            manifestPlaceholders["appName"] = "Coral X"
        }

        release {
            versionNameSuffix = "-RELEASE"
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "Coral X"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // ★ Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // ★ Compose
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ★ Coroutines (for UpdateChecker's withContext/Dispatchers.IO)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ★ JSON parsing (for UpdateChecker's GitHub API response)
    implementation("org.json:json:20240303")

    // ★ Kyant backdrop — real AGSL glass blur (same as old Coral)
    implementation("io.github.kyant0:backdrop:1.0.0")

    // ★ Coil3 — image loading (for album art)
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")

    // ★ Palette — extract dominant colors from album art for background gradients
    implementation("androidx.palette:palette-ktx:1.0.0")

    // ★ Media3 — ExoPlayer + MediaSession for music playback
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-decoder:1.5.1")

    // ★ ALAC decoder — pure-Java fallback for 24-bit ALAC files
    //    (hardware decoder on some devices silently produces silence
    //    for 24-bit ALAC; this bypasses it)
    implementation(project(":misc:alacdecoder"))

    // ★ Serialization — for SongCache JSON persistence + Playlist/Favorites JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ★ Lifecycle — for PermissionScreen's DisposableEffect lifecycle observer
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // ★ Accompanist Lyrics Core — pure-Kotlin lyrics parser (LRC, Enhanced LRC,
    //    TTML/Apple Syllable, Lyricify Syllable). Apache 2.0.
    //    Source: github.com/AmanRajAryan/accompanist-lyrics-core (fork of
    //    Mocha-Realm/Accompanist-Lyrics by Simon Scholz / 6xingyv).
    //    Used as the PRIMARY lyrics source parser.
    implementation("com.mocharealm.accompanist:lyrics-core:0.4.7")

    // ★ Accompanist Lyrics UI — karaoke-style Compose lyrics renderer.
    //    Word-by-word highlighting, multi-voice (duets), accompaniment styling.
    //    Apache 2.0. Source: github.com/AmanRajAryan/accompanist-lyrics-ui.
    //    Compose Multiplatform 1.10.2 → maps to androidx.compose 1.10.4 on Android.
    //    Pulls in gaze-capsule (com.mocharealm.gaze:capsule:2.1.1-patch2) transitively.
    implementation("com.mocharealm.accompanist:lyrics-ui:1.0.18")

    // ★ TagLib — JNI bindings for TagLib (C++ audio metadata library).
    //    Reads embedded lyrics (LYRICS, USLT, UNSYNCEDLYRICS, ©LYR), artwork,
    //    codec, bitrate, sample rate from MP3/M4A/FLAC/OGG/OPUS/WAV/AIFF/WMA/APE.
    //    Apache 2.0 (declared in POM). Source: github.com/AmanRajAryan/AndroidTagLib.
    //    Native libs: libtaglib_jni.so (arm64-v8a 1.6MB + armeabi-v7a 1MB).
    //    Used as the PRIMARY lyrics source — reads embedded lyrics straight from
    //    the audio file. No network needed for songs with embedded lyrics.
    implementation("io.github.amanrajaryan:TagLib:1.0.2")
}
