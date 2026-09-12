plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pulseflow.wallpaper"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pulseflow.wallpaper.dev"
        minSdk = 26
        targetSdk = 35
        versionCode = 26
        versionName = "0.26"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
