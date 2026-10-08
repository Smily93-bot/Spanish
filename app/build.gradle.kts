plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bluediamond.spanishblaster.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 36
        versionName = "1.23.1"
    }

    signingConfigs {
        // One fixed key committed to the repo, so every CI build can update the app already
        // installed on a phone (the default debug key is regenerated on each CI runner).
        // It is public, so create a private key (kept out of git) before publishing to Google Play.
        create("shared") {
            storeFile = file("signing.keystore")
            storePassword = "spanishblaster"
            keyAlias = "spanishblaster"
            keyPassword = "spanishblaster"
        }
        // Private Google Play upload key, used for release builds when CI provides it
        // (GitHub secrets PLAY_KEYSTORE_BASE64, PLAY_KEYSTORE_PASSWORD, PLAY_KEY_ALIAS, PLAY_KEY_PASSWORD).
        val playKeystore = System.getenv("PLAY_KEYSTORE_FILE")
        if (!playKeystore.isNullOrBlank()) {
            create("play") {
                storeFile = file(playKeystore)
                storePassword = System.getenv("PLAY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PLAY_KEY_ALIAS")
                keyPassword = System.getenv("PLAY_KEY_PASSWORD")
            }
        }
    }

    // One app per target language. They share all game code; each flavor brings its own content
    // (src/<flavor>/assets), app name and TargetLanguage object, and installs side by side.
    flavorDimensions += "language"
    productFlavors {
        create("spanish") {
            dimension = "language"
            applicationId = "com.bluediamond.spanishblaster.app"
        }
        create("italian") {
            dimension = "language"
            applicationId = "com.bluediamond.italianblaster.app"
            versionCode = 17
            versionName = "1.13.1"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("play") ?: signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        noCompress += "json"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.play.services.ads)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    // Real org.json for unit tests (Android's copy is only a stub off-device).
    testImplementation(libs.org.json)
}
