import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Dev-only scaffolding: a plain JVM application module (not part of what `:convey` publishes)
// that exists solely to give Compose Hot Reload the application-module shape it requires --
// JetBrains' own plugin docs are explicit that Hot Reload targets an app module with a `main()`
// and doesn't work applied inside a Kotlin Multiplatform *library* module (which `:convey` is).
// `./gradlew :dev-app:hotRunJvm` launches a live-reloadable window over the composables
// src/main/kotlin/.../dev/Dev.kt exercises: edit a composable in `:convey` or this gallery
// itself, save, and Compose Hot Reload swaps the changed code into the running JVM instead of
// restarting it.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets.commonMain.dependencies {
        implementation(project(":convey"))
        implementation(compose.desktop.currentOs)
        implementation(compose.material3)
    }
}

compose.desktop {
    application {
        mainClass = "compose.conveyance.dev.DevKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "conveyance-dev"
            packageVersion = "1.0.0"
        }
    }
}
