plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pulseflow.wallpaper"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pulseflow.wallpaper"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("PULSEFLOW_KEYSTORE") ?: "pulseflow-release.jks")
            storePassword = System.getenv("PULSEFLOW_STORE_PASSWORD") ?: "pulseflow-test-2026"
            keyAlias = System.getenv("PULSEFLOW_KEY_ALIAS") ?: "pulseflow"
            keyPassword = System.getenv("PULSEFLOW_KEY_PASSWORD") ?: "pulseflow-test-2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
