plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "edu.pwr.zpi.netwalk"
    compileSdk = 36

    sourceSets {
        getByName("main") {
            java.directories.add("src/main/kotlin")
        }
    }

    defaultConfig {
        applicationId = "edu.pwr.zpi.netwalk"
        minSdk = 30 // 29 API potrzebne dla requestCellInfoUpdate
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // TODO: change name of produced apk
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.ui.tooling)
}

ktlint {
    android.set(true)
    outputColorName.set("RED")
    verbose.set(true)
}
