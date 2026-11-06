plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

android {
    namespace = "org.nwdl.cast"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "org.nwdl.cast"
        minSdk = 32 // Horizon OS
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        ndk { abiFilters += "arm64-v8a" } // Quest
    }

    buildTypes {
        debug {
            // Local dev stack runs on plain http/ws
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
    }

    buildFeatures {
        buildConfig = true // BuildConfig.DEBUG: release builds only use cast.nwdl.org
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("io.livekit:livekit-android:2.30.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("com.google.zxing:core:3.5.3") // start and stop codes
}
