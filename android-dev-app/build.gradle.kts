// A real, installable Android application module -- not published, exists solely to run the
// kinetic typography gallery on an actual Android device/emulator. `:convey` itself is a Kotlin
// Multiplatform *library* module (`com.android.library`), which is not something you can `adb
// install`; on-device hot-swap needs a debuggable APK to attach to, same structural requirement
// HotSwan's own docs describe ("the plugin applies exclusively to the module producing the
// Android APK... do not apply to shared KMP library modules").
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "compose.conveyance.devapp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "compose.conveyance.devapp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        // debuggable=true is the default for the debug build type; stated explicitly here
        // because the whole point of this module is that the hot-swap tool (see :hotswap)
        // needs a JDWP port to attach to, which ART only opens for a debuggable process.
        debug {
            isDebuggable = true
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Matches convey/build.gradle.kts's own androidTarget { compilerOptions { jvmTarget = JVM_11 } }
// -- keeping both android modules on the same JVM target avoids the "Inconsistent JVM Target
// Compatibility" failure between this module's compileDebugJavaWithJavac (11, from
// compileOptions above) and its compileDebugKotlin (which otherwise defaults to the toolchain's
// own JDK version, 21 here).
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":convey"))
    implementation(compose.material3)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
}
