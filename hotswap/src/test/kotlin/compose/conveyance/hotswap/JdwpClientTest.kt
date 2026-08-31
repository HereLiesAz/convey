package compose.conveyance.hotswap

import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Exercises the JDWP wire encoding/decoding against a scripted fake "server" (a pair of piped
 * streams, not a real ART process) -- this is the part of `:hotswap` genuinely verifiable without
 * an Android device: that we speak the documented JDWP packet format correctly. It does NOT
 * verify that ART actually accepts and applies a real RedefineClasses request; see this module's
 * README for what that would take.
 */
class JdwpClientTest {
    private class FakeJdwpServer {
        // Two independent pipes, one per direction: the client reads [clientInput] (fed by
        // [serverWritesToClient]) and writes [clientOutput] (drained by [serverReadsClient]).
        private val clientToServer = PipedOutputStream()
        private val serverToClient = PipedOutputStream()

        val clientOutput: PipedOutputStream = clientToServer
        val clientInput = PipedInputStream(serverToClient)

        private val serverReadsClient = java.io.DataInputStream(PipedInputStream(clientToServer))
        private val serverWritesToClient = java.io.DataOutputStream(serverToClient)

        fun readClientPacketOrHandshake(expectHandshake: Boolean): ByteArray {
            if (expectHandshake) {
                val buf = ByteArray(14)
                serverReadsClient.readFully(buf)
                return buf
            }
            val length = serverReadsClient.readInt()
            val rest = ByteArray(length - 4)
            serverReadsClient.readFully(rest)
            return ByteBuffer.allocate(length).putInt(length).put(rest).array()
        }

        fun writeHandshake() {
            serverWritesToClient.write("JDWP-Handshake".toByteArray(Charsets.US_ASCII))
            serverWritesToClient.flush()
        }

        fun writeReply(id: Int, data: ByteArray) {
            serverWritesToClient.writeInt(11 + data.size)
            serverWritesToClient.writeInt(id)
            serverWritesToClient.writeByte(0x80)
            serverWritesToClient.writeShort(0) // error code
            serverWritesToClient.write(data)
            serverWritesToClient.flush()
        }
    }

    @Test
    fun `handshake and IDSizes round-trip`() {
        val server = FakeJdwpServer()
        val serverThread = Thread {
            server.readClientPacketOrHandshake(expectHandshake = true)
            server.writeHandshake()
            val idSizesRequest = server.readClientPacketOrHandshake(expectHandshake = false)
            val id = ByteBuffer.wrap(idSizesRequest, 4, 4).int
            val sizes = ByteBuffer.allocate(20).putInt(4).putInt(8).putInt(8).putInt(8).putInt(8).array()
            server.writeReply(id, sizes)
        }.apply { isDaemon = true; start() }

        JdwpClient.connect(server.clientInput, server.clientOutput).use { }
        serverThread.join(2000)
    }

    @Test
    fun `classBySignature returns null when the VM reports zero matches`() {
        val server = FakeJdwpServer()
        val serverThread = Thread {
            server.readClientPacketOrHandshake(expectHandshake = true)
            server.writeHandshake()
            val idSizesRequest = server.readClientPacketOrHandshake(expectHandshake = false)
            val idSizesId = ByteBuffer.wrap(idSizesRequest, 4, 4).int
            server.writeReply(idSizesId, ByteBuffer.allocate(20).putInt(4).putInt(8).putInt(8).putInt(8).putInt(8).array())

            val lookupRequest = server.readClientPacketOrHandshake(expectHandshake = false)
            val lookupId = ByteBuffer.wrap(lookupRequest, 4, 4).int
            server.writeReply(lookupId, ByteBuffer.allocate(4).putInt(0).array()) // classCount = 0
        }.apply { isDaemon = true; start() }

        val client = JdwpClient.connect(server.clientInput, server.clientOutput)
        val result = client.classBySignature("Lcompose/conveyance/devapp/MainActivity;")
        assertNull(result)
        client.close()
        serverThread.join(2000)
    }

    @Test
    fun `classBySignature parses a single match`() {
        val server = FakeJdwpServer()
        val refTypeId = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 42)
        val serverThread = Thread {
            server.readClientPacketOrHandshake(expectHandshake = true)
            server.writeHandshake()
            val idSizesRequest = server.readClientPacketOrHandshake(expectHandshake = false)
            val idSizesId = ByteBuffer.wrap(idSizesRequest, 4, 4).int
            server.writeReply(idSizesId, ByteBuffer.allocate(20).putInt(4).putInt(8).putInt(8).putInt(8).putInt(8).array())

            val lookupRequest = server.readClientPacketOrHandshake(expectHandshake = false)
            val lookupId = ByteBuffer.wrap(lookupRequest, 4, 4).int
            val reply = ByteBuffer.allocate(4 + 1 + 8 + 4)
                .putInt(1) // classCount
                .put(1) // refTypeTag = CLASS
                .put(refTypeId)
                .putInt(2) // status
                .array()
            server.writeReply(lookupId, reply)
        }.apply { isDaemon = true; start() }

        val client = JdwpClient.connect(server.clientInput, server.clientOutput)
        val result = client.classBySignature("Lcompose/conveyance/devapp/MainActivity;")
        client.close()
        serverThread.join(2000)

        assertEquals(1, result?.tag)
        assertContentEquals(refTypeId, result?.id)
        assertEquals(2, result?.status)
    }

    @Test
    fun `redefineClass sends the class count, refType id, and dex bytes in order`() {
        val server = FakeJdwpServer()
        val refTypeId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val dexBytes = byteArrayOf(0x64, 0x65, 0x78, 0x0a) // "dex\n" magic, stand-in payload
        var capturedRequest: ByteArray? = null

        val serverThread = Thread {
            server.readClientPacketOrHandshake(expectHandshake = true)
            server.writeHandshake()
            val idSizesRequest = server.readClientPacketOrHandshake(expectHandshake = false)
            val idSizesId = ByteBuffer.wrap(idSizesRequest, 4, 4).int
            server.writeReply(idSizesId, ByteBuffer.allocate(20).putInt(4).putInt(8).putInt(8).putInt(8).putInt(8).array())

            val redefineRequest = server.readClientPacketOrHandshake(expectHandshake = false)
            capturedRequest = redefineRequest.copyOfRange(11, redefineRequest.size)
            val redefineId = ByteBuffer.wrap(redefineRequest, 4, 4).int
            server.writeReply(redefineId, ByteArray(0))
        }.apply { isDaemon = true; start() }

        val client = JdwpClient.connect(server.clientInput, server.clientOutput)
        val refType = JdwpClient.ReferenceType(tag = 1, id = refTypeId, status = 2)
        client.redefineClass(refType, dexBytes)
        client.close()
        serverThread.join(2000)

        val request = requireNotNull(capturedRequest)
        val buf = ByteBuffer.wrap(request)
        assertEquals(1, buf.int) // classCount
        val gotRefTypeId = ByteArray(8).also { buf.get(it) }
        assertContentEquals(refTypeId, gotRefTypeId)
        assertEquals(dexBytes.size, buf.int)
        val gotDex = ByteArray(dexBytes.size).also { buf.get(it) }
        assertContentEquals(dexBytes, gotDex)
    }
}
