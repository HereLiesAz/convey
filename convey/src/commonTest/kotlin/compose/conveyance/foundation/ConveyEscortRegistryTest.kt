package compose.conveyance.foundation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConveyEscortRegistryTest {

    @Test
    fun escortToInvokesTheRegisteredTravelCallback() = runTest {
        val registry = ConveyEscortRegistry()
        var travelled = false
        registry.register("email") { travelled = true }

        registry.escortTo("email")

        assertTrue(travelled)
    }

    @Test
    fun escortToDoesNothingForAnUnregisteredIdentity() = runTest {
        val registry = ConveyEscortRegistry()

        // Must not throw.
        registry.escortTo("nothing-registered-here")
    }

    @Test
    fun unregisterStopsFutureEscorts() = runTest {
        val registry = ConveyEscortRegistry()
        var calls = 0
        registry.register("email") { calls++ }
        registry.unregister("email")

        registry.escortTo("email")

        assertEquals(0, calls)
    }

    @Test
    fun reRegisteringAnIdentityReplacesItsTravelCallback() = runTest {
        val registry = ConveyEscortRegistry()
        registry.register("email") { error("stale callback should not run") }
        var calls = 0
        registry.register("email") { calls++ }

        registry.escortTo("email")

        assertEquals(1, calls)
    }
}
