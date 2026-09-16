import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = "compose.conveyance"
version = (findProperty("conveyVersion") as String?) ?: "1.0.0"

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

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

        val expressiveShapeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("androidx.graphics:graphics-shapes:1.1.0")
                implementation("androidx.collection:collection:1.5.0")
            }
        }

        val androidMain by getting {
            dependsOn(expressiveShapeMain)
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }

        val desktopMain by getting {
            dependsOn(expressiveShapeMain)
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

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/HereLiesAz/convey")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
