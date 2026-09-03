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
        // 첫 공개 배포 버전. versionCode는 테스트판(39)보다 높게 유지해
        // 기존 설치 사용자가 삭제 없이 v1.0으로 업데이트할 수 있게 한다.
        versionCode = 40
        versionName = "1.0"
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation("androidx.compose.ui:ui:1.9.1")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.10.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
