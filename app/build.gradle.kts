plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.tdvorak.nothingmodes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tdvorak.nothingmodes"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            // Read from environment variables. Set in CI secrets or ~/.gradle/gradle.properties:
            // NOTHING_MODES_KEYSTORE, NOTHING_MODES_KEYSTORE_PASSWORD,
            // NOTHING_MODES_KEY_ALIAS, NOTHING_MODES_KEY_PASSWORD
            val keystorePath = System.getenv("NOTHING_MODES_KEYSTORE")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("NOTHING_MODES_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("NOTHING_MODES_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("NOTHING_MODES_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildFeatures { compose = true }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":core-shizuku"))
    implementation(project(":device-tools"))
    implementation(project(":data"))
    implementation(project(":automation-android"))
    implementation(project(":capabilities"))
    implementation(project(":nothing-integrations"))
    implementation(project(":ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
