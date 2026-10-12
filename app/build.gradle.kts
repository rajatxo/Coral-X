import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
        minSdk = 24
        targetSdk = 35
        versionCode = 502
        versionName = "0.5.2"
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

    // ★ Coil3 — image loading (for album art when music scanning is added)
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
}
