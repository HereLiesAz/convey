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
include(":dev-app")
