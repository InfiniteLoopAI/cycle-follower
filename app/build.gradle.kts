plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// The APK published to GitHub Releases is signed with the keystore checked in at
// signing/cycle-follower.jks. It is NOT a Play Store upload key -- it exists only so that
// every build carries the SAME signature, which is what lets you install an update on top
// of a previously installed build instead of having to uninstall first.
// CI overwrites this file from the KEYSTORE_BASE64 secret when that secret is configured.
val sharedKeystore = rootProject.file("signing/cycle-follower.jks")

// GitHub Actions sets an unconfigured secret to an EMPTY string rather than leaving the variable
// unset, so `System.getenv(...) ?: default` would hand an empty password to the signer.
fun signingEnv(name: String, fallback: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback

// Every published build needs a distinct, increasing versionCode: Android uses it to decide
// whether an APK is an update, and update checkers use it to decide whether one is available.
// CI supplies the run number; local builds fall back to 1.
val baseVersion = "1.0.0"
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0

android {
    namespace = "com.infiniteloop.cyclefollower"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.infiniteloop.cyclefollower"
        minSdk = 26
        targetSdk = 35
        versionCode = if (buildNumber > 0) buildNumber else 1
        versionName = if (buildNumber > 0) "$baseVersion.$buildNumber" else baseVersion
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (sharedKeystore.exists()) {
            create("shared") {
                storeFile = sharedKeystore
                storePassword = signingEnv("KEYSTORE_PASSWORD", "cyclefollower")
                keyAlias = signingEnv("KEY_ALIAS", "cyclefollower")
                keyPassword = signingEnv("KEY_PASSWORD", "cyclefollower")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("shared") ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        // So the app can show which build it is -- otherwise there is no way to tell whether a
        // phone has the latest release.
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.biometric)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
