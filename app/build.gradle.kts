import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // No `kotlin-android` plugin — AGP 9.0+ ships Kotlin support built-in.
    // See https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.dsqueez"
    // compileSdk = 37 follows the AndroidX 1.19/Compose 1.12 alpha line we're on;
    // targetSdk stays at 36 (Android 16) since that's what your Pixel actually runs.
    // Decoupling these is the supported pattern (see Android API decoupling docs).
    compileSdk = 37

    defaultConfig {
        applicationId = "app.dsqueez"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += "arm64-v8a"
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fvisibility=hidden", "-O3")
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "/META-INF/{AL2.0,LGPL2.1}",
            )
        }
        jniLibs {
            // libvips ships transitive .so deps; don't dedupe.
            useLegacyPackaging = false
        }
    }
}

// AGP 9.0+ exposes the Kotlin extension at the top level. Configure compiler
// options here rather than inside `android.kotlinOptions` (deprecated/removed).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose BOM aligns transitive versions of every other Compose artifact.
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.text.google.fonts)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.exifinterface)

    implementation(libs.kotlinx.coroutines.android)
}
