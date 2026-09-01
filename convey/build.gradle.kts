import org.jetbrains.dokka.gradle.formats.DokkaFormatPlugin
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dokka)
    `maven-publish`
}

// GitHub wikis render Markdown (via Gollum), not a static HTML site with its own CSS/JS assets --
// Dokka's default output. This registers GitHub-Flavored-Markdown as an additional Dokka output
// format (`dokkaGenerateMarkdown`) so the API reference can be pushed straight into the wiki repo
// (see .github/workflows/publish-docs.yml). HTML output (`dokkaGenerateHtml`) is untouched.
@OptIn(InternalDokkaGradlePluginApi::class)
abstract class DokkaMarkdownPlugin : DokkaFormatPlugin(formatName = "markdown") {
    override fun DokkaFormatPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("gfm-plugin"))
            formatDependencies.dokkaPublicationPluginClasspathApiOnly.dependencies.addLater(
                dokka("gfm-template-processing-plugin")
            )
        }
    }
}
apply<DokkaMarkdownPlugin>()

group = "compose.conveyance"
version = "1.0.0"

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    // iosX64 (the Intel simulator target) dropped out with this Compose Multiplatform bump --
    // 1.12.0 doesn't publish org.jetbrains.compose.*:*-iosx64 artifacts at all (confirmed: 404 on
    // Maven Central where iosarm64/iossimulatorarm64 both 200), consistent with the wider industry
    // having moved off Intel Macs. iosArm64 (real device) + iosSimulatorArm64 (Apple Silicon
    // simulator) is the modern KMP pair.
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.animationGraphics)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "compose.conveyance"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        buildConfigField("Boolean", "DEBUG", "false")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "DEBUG", "true")
        }
    }

    // KMP's `publishLibraryVariants("release")` (below, in the `kotlin {}` block) requires the
    // Android library plugin to actually expose a "release" library component -- without this,
    // com.android.library does not publish one on its own, and the build fails at configuration
    // time with "tried to set up publishing for Android build variants that are not library
    // variants or do not exist: release".
    publishing {
        singleVariant("release")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("Convey")
            description.set(
                "A Compose Multiplatform design system built on the Conveyance Manifesto. " +
                "Every element earns its place. Motion is grammar. Shape is signal."
            )
            url.set("https://github.com/HereLiesAz/convey")
            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                }
            }
        }
    }
}
