plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.wizlabs.photofit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wizlabs.photofit"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        // Ship only English resources (drops dozens of unused library translations).
        resourceConfigurations += listOf("en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val ks = System.getenv("KEYSTORE_FILE")
            if (!ks.isNullOrBlank()) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(ks)
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEY_ALIAS")
                    keyPassword = System.getenv("KEY_PASSWORD")
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    // Installs bundled baseline profiles on sideloaded installs too: faster start, smoother first scrolls.
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // Core icon set only (no icons-extended): rarer icons are drawn in code.
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.exifinterface:exifinterface:1.4.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
