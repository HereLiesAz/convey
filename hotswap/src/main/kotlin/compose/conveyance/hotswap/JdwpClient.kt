package compose.conveyance.hotswap

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

private const val HANDSHAKE = "JDWP-Handshake"

/**
 * A minimal client for the parts of the standard [JDWP wire
 * protocol](https://docs.oracle.com/javase/8/docs/platform/jpda/jdwp/jdwp-protocol.html) this
 * tool needs: the handshake, `VirtualMachine.IDSizes`, `VirtualMachine.ClassesBySignature`, and
 * `VirtualMachine.RedefineClasses`. ART implements the same standard command set (it's how
 * Android Studio's own "Apply Changes" works) -- this is not an Android- or ART-specific
 * protocol, just a small enough subset of a general one that a from-scratch client is tractable.
 *
 * Only [VirtualMachine.RedefineClasses] itself is genuinely ART-specific in its *payload*: the
 * bytes must be a class re-dexed for ART (see the `d8`-based pipeline in [ClassRedefiner]), not
 * plain JVM classfile bytecode -- ART executes DEX, not JVM bytecode, even though it answers the
 * same JDWP command a standard JVM would.
 */
class JdwpClient private constructor(
    private val socket: Socket,
    private val input: DataInputStream,
    private val output: DataOutputStream,
) : AutoCloseable {
    private val nextId = AtomicInteger(1)
    private lateinit var idSizes: IdSizes

    data class IdSizes(
        val fieldIdSize: Int,
        val methodIdSize: Int,
        val objectIdSize: Int,
        val referenceTypeIdSize: Int,
        val frameIdSize: Int,
    )

    /** `refTypeTag`, the referenceTypeID (raw bytes, sized per [IdSizes.referenceTypeIdSize]), and the class's JDWP status. */
    data class ReferenceType(val tag: Byte, val id: ByteArray, val status: Int)

    private fun readHandshake() {
        val buf = ByteArray(HANDSHAKE.length)
        input.readFully(buf)
        check(String(buf, Charsets.US_ASCII) == HANDSHAKE) { "Unexpected JDWP handshake reply: ${String(buf)}" }
    }

    private fun writeHandshake() {
        output.write(HANDSHAKE.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun sendCommand(commandSet: Int, command: Int, data: ByteArray): ByteArray {
        val id = nextId.getAndIncrement()
        val length = 11 + data.size
        output.writeInt(length)
        output.writeInt(id)
        output.writeByte(0) // flags: 0 = command packet
        output.writeByte(commandSet)
        output.writeByte(command)
        output.write(data)
        output.flush()
        return readReplyFor(id)
    }

    private fun readReplyFor(expectedId: Int): ByteArray {
        // JDWP allows event packets to interleave with replies; a fully general client would
        // buffer/dispatch those. This tool only ever awaits its own outstanding reply.
        while (true) {
            val length = input.readInt()
            val id = input.readInt()
            val flags = input.readUnsignedByte()
            val errorCode = input.readUnsignedShort()
            val payload = ByteArray(length - 11)
            input.readFully(payload)
            if (flags and 0x80 == 0) continue // a command packet from the VM (an event) -- skip
            if (id != expectedId) continue
            check(errorCode == 0) { "JDWP command failed with error code $errorCode" }
            return payload
        }
    }

    private fun fetchIdSizes(): IdSizes {
        val reply = sendCommand(commandSet = 1, command = 7, data = ByteArray(0))
        val buf = java.nio.ByteBuffer.wrap(reply)
        return IdSizes(buf.int, buf.int, buf.int, buf.int, buf.int)
    }

    private fun writeId(out: java.io.ByteArrayOutputStream, value: ByteArray, size: Int) {
        // JDWP object/reference IDs are opaque, VM-sized byte strings; we only ever round-trip
        // ones the VM itself gave us, so no numeric interpretation is needed here.
        require(value.size == size) { "Expected a ${size}-byte ID, got ${value.size} bytes" }
        out.write(value)
    }

    /** Looks up a loaded class's reference type by its JNI signature, e.g. `"Lcompose/conveyance/devapp/MainActivity;"`. */
    fun classBySignature(jniSignature: String): ReferenceType? {
        val sigBytes = jniSignature.toByteArray(Charsets.UTF_8)
        val request = java.io.ByteArrayOutputStream().apply {
            java.nio.ByteBuffer.allocate(4).putInt(sigBytes.size).array().let(::write)
            write(sigBytes)
        }
        val reply = sendCommand(commandSet = 1, command = 2, data = request.toByteArray())
        val buf = java.nio.ByteBuffer.wrap(reply)
        val count = buf.int
        if (count == 0) return null
        val tag = buf.get()
        val id = ByteArray(idSizes.referenceTypeIdSize).also { buf.get(it) }
        val status = buf.int
        return ReferenceType(tag, id, status)
    }

    /**
     * Redefines a single already-loaded class's method bodies in place. [dexBytes] must be a
     * complete DEX container holding the (re-dexed) new version of the class -- see
     * [ClassRedefiner] for how that's produced. Field/method signatures and the class hierarchy
     * must be unchanged; ART rejects anything else, the same restriction as any JVMTI-class
     * redefinition (including HotSwan and Apply Changes).
     */
    fun redefineClass(refType: ReferenceType, dexBytes: ByteArray) {
        val request = java.io.ByteArrayOutputStream().apply {
            write(java.nio.ByteBuffer.allocate(4).putInt(1).array()) // classCount
            writeId(this, refType.id, idSizes.referenceTypeIdSize)
            write(java.nio.ByteBuffer.allocate(4).putInt(dexBytes.size).array())
            write(dexBytes)
        }
        sendCommand(commandSet = 1, command = 18, data = request.toByteArray())
    }

    companion object {
        /** Connects to a JDWP port already exposed locally, e.g. via `adb forward tcp:<port> jdwp:<pid>`. */
        fun connect(host: String, port: Int): JdwpClient {
            val socket = Socket(host, port)
            return connect(socket.getInputStream(), socket.getOutputStream(), socket)
        }

        internal fun connect(rawIn: InputStream, rawOut: OutputStream, socket: Socket? = null): JdwpClient {
            val input = DataInputStream(rawIn)
            val output = DataOutputStream(rawOut)
            val client = JdwpClient(socket ?: Socket(), input, output)
            client.writeHandshake()
            client.readHandshake()
            client.idSizes = client.fetchIdSizes()
            return client
        }
    }

    override fun close() {
        socket.close()
    }
}
