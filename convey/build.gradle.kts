plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    `maven-publish`
}

group = "compose.conveyance"
version = "1.0.0"

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    androidLibrary {
        namespace = "compose.conveyance"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.animationGraphics)
            implementation(compose.ui)
            // Only for LocalContentColor -- Convey does not otherwise depend on Material.
            implementation(compose.material3)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        val desktopTest by getting {
            dependencies {
                // The real Skia bindings for the current OS -- without this, any test touching an
                // actual Path/PathMeasure (Compose Multiplatform's own real graphics, as opposed to
                // a stub) fails to load the native library rather than running.
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Convey")
            description.set(
                "A Compose Multiplatform design system built on the Conveyance Manifesto. " +
                "Every element earns its place. Motion is grammar. Shape is signal.",
            )
            url.set("https://github.com/HereLiesAz/conveyance-convey")
            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                }
            }
        }
    }
}
