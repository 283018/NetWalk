plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "edu.pwr.zpi.netwalk"
    compileSdk = 36

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
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

ktlint {
    android.set(true)
    outputColorName.set("RED")
    verbose.set(true)
}
