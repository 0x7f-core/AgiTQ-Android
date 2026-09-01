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
        versionCode = 24
        versionName = "4.23"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("AGITQ_KEYSTORE_PATH") ?: "agitq-release.p12"
            storeFile = file(keystorePath)
            storePassword = System.getenv("AGITQ_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("AGITQ_KEY_ALIAS")
            keyPassword = System.getenv("AGITQ_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
