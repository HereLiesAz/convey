pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    // Compose Hot Reload needs the JetBrains Runtime (an OpenJDK fork with enhanced class
    // redefinition) to actually reload code live; this lets Gradle auto-provision it rather than
    // requiring a JBR already installed on the machine running `:convey:hotRunDesktop`.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "conveyance-convey"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":convey")
// Dev-loop modules are never published. JitPack sets JITPACK=true; skipping them there keeps the
// publishing build from configuring an application module and the Hot Reload plugin it never uses.
if (System.getenv("JITPACK") == null) {
    include(":dev-app")
    include(":android-dev-app")
    include(":hotswap")
}
