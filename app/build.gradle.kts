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
        versionCode = 8
        versionName = "0.8"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
