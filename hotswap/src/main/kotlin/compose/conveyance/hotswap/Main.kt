package compose.conveyance.hotswap

import java.io.File

/**
 * `./gradlew :hotswap:run --args="<package> <binary.class.Name> <path/to/Name.class>"` --
 * redefines one already-loaded class on the given package's single running debuggable process
 * and broadcasts [compose.conveyance.devapp.RELOAD_ACTION] so `android-dev-app`'s `MainActivity`
 * recomposes with the new code. See this module's README for the full explanation and, just as
 * important, what here is unverified (needs a real device -- see that README).
 *
 * This redefines *one class at a time* and is meant to be driven by a file watcher of your
 * choice (e.g. `entr`, `fswatch`) piping changed `.class` paths in, rather than this tool
 * reinventing file watching -- see the README's suggested `entr` one-liner.
 */
fun main(args: Array<String>) {
    require(args.size == 3) {
        "Usage: <package> <binary.class.Name> <path/to/Name.class>"
    }
    val (packageName, binaryClassName, classFilePath) = args
    val classFile = File(classFilePath)
    require(classFile.exists()) { "No such class file: $classFilePath" }

    val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        ?: error("Set ANDROID_HOME or ANDROID_SDK_ROOT")
    val d8 = ClassRedefiner.findD8(File(sdkRoot))

    val pid = adbPidOf(packageName)
    val localPort = 8700 + (pid % 1000)
    adb("forward", "tcp:$localPort", "jdwp:$pid")

    println("Redexing $binaryClassName via d8...")
    val dexBytes = ClassRedefiner.redex(classFile, d8)

    println("Connecting to JDWP on 127.0.0.1:$localPort (pid $pid)...")
    JdwpClient.connect("127.0.0.1", localPort).use { client ->
        val signature = ClassRedefiner.jniSignature(binaryClassName)
        val refType = client.classBySignature(signature)
            ?: error("Class not found on device (is it loaded yet?): $signature")
        println("Sending RedefineClasses for $signature...")
        client.redefineClass(refType, dexBytes)
    }

    println("Broadcasting reload to $packageName...")
    adb("shell", "am", "broadcast", "-a", "compose.conveyance.devapp.RELOAD", "-p", packageName)
    println("Done.")
}

private fun adbPidOf(packageName: String): Int {
    val output = adb("shell", "pidof", packageName).trim()
    return output.toIntOrNull()
        ?: error("No running process for $packageName (is it installed and launched?)")
}

private fun adb(vararg args: String): String {
    val process = ProcessBuilder("adb", *args).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    val exit = process.waitFor()
    check(exit == 0) { "adb ${args.joinToString(" ")} failed (exit $exit):\n$output" }
    return output
}
