plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.agitq.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.agitq.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "4.3"
    }

    buildTypes {
        getByName("release") {
            // Sign the release APK with Android's standard debug key so the
            // GitHub Actions artifact can be installed directly on a device.
            // This is suitable for personal/testing distribution, not Play Store publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
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
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui:1.9.1")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.10.5")
}
