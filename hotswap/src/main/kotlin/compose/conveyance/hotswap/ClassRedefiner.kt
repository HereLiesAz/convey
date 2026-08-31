package compose.conveyance.hotswap

import java.io.File
import java.nio.file.Files

/**
 * Converts a single compiled `.class` file into a DEX container via the Android SDK's `d8` tool
 * -- ART's JDWP `RedefineClasses` implementation expects Dalvik-executable bytecode, not plain
 * JVM classfile bytes, since ART runs DEX rather than a JVM class loader.
 */
object ClassRedefiner {
    fun redex(classFile: File, d8: File): ByteArray {
        val tmpDir = Files.createTempDirectory("convey-hotswap").toFile()
        try {
            val process = ProcessBuilder(
                d8.absolutePath,
                "--release",
                "--output", tmpDir.absolutePath,
                classFile.absolutePath,
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            check(exit == 0) { "d8 failed (exit $exit):\n$output" }
            val dex = File(tmpDir, "classes.dex")
            check(dex.exists()) { "d8 didn't produce classes.dex:\n$output" }
            return dex.readBytes()
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    /** Locates `d8` under an Android SDK's newest installed build-tools version. */
    fun findD8(sdkRoot: File): File {
        val buildTools = File(sdkRoot, "build-tools").listFiles()?.filter { it.isDirectory }
            ?: error("No build-tools found under $sdkRoot")
        val newest = buildTools.maxByOrNull { it.name } ?: error("No build-tools versions found under $sdkRoot")
        val d8 = File(newest, "d8")
        check(d8.exists()) { "d8 not found at $d8 -- install this build-tools version's d8 binary" }
        return d8
    }

    /** `Lfoo/bar/Baz;` style JNI signature from a class file's binary name (`foo/bar/Baz` or `foo.bar.Baz`). */
    fun jniSignature(binaryName: String): String = "L${binaryName.replace('.', '/')};"
}
