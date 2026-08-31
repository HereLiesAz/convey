// Pure-JVM host-side tool: a minimal JDWP (Java Debug Wire Protocol) client plus a watcher CLI
// that redefines changed classes on an already-running, debuggable Android process -- the
// on-device counterpart to `:dev-app:hotRunJvm`'s desktop Compose Hot Reload. See README.md in
// this module for the protocol details, what's unit-tested vs. what still needs verification on
// a real device, and why this exists instead of a paid tool (HotSwan) or Android Studio's own
// Apply Changes (which does the equivalent of this by hand, from the IDE, not from a dev loop).
plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

application {
    mainClass = "compose.conveyance.hotswap.MainKt"
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${libs.versions.kotlin.get()}")
}
