plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

val commitCount = try {
    providers
        .exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText
        .get()
        .trim()
} catch (e: Exception) {
    "1"
}

val describe = try {
    providers
        .exec {
            commandLine("git", "describe", "--tags", "--always")
        }.standardOutput.asText
        .get()
        .trim()
} catch (e: Exception) {
    "0.0.0"
}

android {
    namespace = "edu.pwr.zpi.netwalk"
    compileSdk = 36
    ndkVersion = libs.versions.ndk.get()

    sourceSets {
        getByName("main") {
            java.directories.add("src/main/kotlin")
        }
    }

    defaultConfig {
        applicationId = "edu.pwr.zpi.netwalk"
        minSdk = 30 // 29 API potrzebne dla requestCellInfoUpdate
        targetSdk = 36

        versionCode = commitCount.toInt() // using commits count as code version
        versionName = describe.substringBefore("-").removePrefix("v") // using last tag as app version

        externalNativeBuild {
            cmake {
                cFlags += listOf("-std=c11", "-D__STDC_NO_ATOMICS__=0")
            }
        }

        // tu trzeba wksazać architekture procesora
        // arm64 i armeabi powinno wystarczyć dla wszystkiego z androidem 12+
        // ale w przypadku emulatora trzeba kopilować dla architektury hosta
        ndk {
            abiFilters += listOf(
                "arm64-v8a", // modern phones
                // "armeabi-v7a", // legacy x32 architecture (may require disabling atomics in iperf_config_android.h )
                "x86_64", // x64 emulator
            )
        }
    }

    // prevent some names optimizations
    buildTypes {
        // not used anyway
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.play.services.location)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)
}

ktlint {
    android.set(true)
    outputColorName.set("RED")
    verbose.set(true)
}
