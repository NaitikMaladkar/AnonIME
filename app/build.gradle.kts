plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.anonime"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anonime"
        minSdk = 29
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-phase2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            // R8 minification (isMinifyEnabled) and resource shrinking
            // (isShrinkResources) were aggressively stripping runtime-needed
            // classes from Compose, Navigation, and reflection-based loaders,
            // causing an instant crash on launch even with expanded ProGuard
            // keep rules. Per user request, disabled both so the APK ships at
            // its natural ~40 MB size and runs cleanly. We can re-enable
            // shrinking later once we have a complete ProGuard ruleset that
            // preserves every Compose/Navigation entry point.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign release builds with the project keystore so the APK is
            // installable on real devices. Credentials come from
            // local.properties (or gradle.properties) so they're not checked
            // into git.
            val storeFilePath = providers
                .gradleProperty("ANONIME_STORE_FILE").orNull
            if (storeFilePath != null) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(storeFilePath)
                    storePassword = providers
                        .gradleProperty("ANONIME_STORE_PASSWORD").get()
                    keyAlias = providers
                        .gradleProperty("ANONIME_KEY_ALIAS").get()
                    keyPassword = providers
                        .gradleProperty("ANONIME_KEY_PASSWORD").get()
                }
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.navigation.compose)

    // Compose (BOM-managed versions)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Debug tooling only
    debugImplementation(libs.androidx.ui.tooling)

    // Unit tests
    testImplementation("junit:junit:4.13.2")
}
