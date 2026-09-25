plugins {
    // AGP 9+ has built-in Kotlin support, so no kotlin.android plugin here.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.astrochat.insufficient"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.astrochat.insufficient"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // Coin shower + confetti. The web prototype plays dotLottie via a web component;
    // lottie-compose reads the same Lottie JSON, so the motion is shared not re-authored.
    implementation(libs.lottie.compose)
}
