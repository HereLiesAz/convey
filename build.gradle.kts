// Root build file. `convey/` is the only module this repo publishes; this file exists mainly so
// JitPack (which looks for a Gradle/Maven build file at the repository root before it will even
// attempt a build) finds one -- see settings.gradle.kts for the actual module wiring.
//
// Every plugin shared by more than one subproject (`convey` and `dev-app` both apply the Kotlin
// Multiplatform + Compose plugins) MUST be declared here once, unapplied, rather than only in
// each subproject's own `plugins {}` block. Otherwise Gradle loads a second, separate instance
// of the Kotlin Gradle Plugin per subproject -- harmless for plain Kotlin compilation, but it
// breaks Compose Hot Reload specifically: its `hasComposePluginAccess` check does a class-identity
// comparison against `org.jetbrains.compose.ComposePlugin`, which fails across two different
// plugin classloaders even at identical versions, silently skipping hotRun* task registration
// with only a logged "Cannot access 'org.jetbrains.compose' plugin" warning (no build failure) --
// see the real, verified-in-this-repo Gradle warning: "The Kotlin Gradle plugin was loaded
// multiple times in different subprojects... add the Kotlin plugin to the common parent project".
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.composeHotReload) apply false
}
