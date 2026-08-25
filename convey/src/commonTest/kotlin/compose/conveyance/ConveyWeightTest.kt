package compose.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConveyWeightTest {

    @Test
    fun countsStartAtZero() {
        val registry = ConveyWeightRegistry()
        assertEquals(0, registry.heroCount)
        assertEquals(0, registry.primaryCount)
        assertEquals(0, registry.secondaryCount)
        assertEquals(0, registry.ghostCount)
    }

    @Test
    fun registerTracksEachWeightIndependently() {
        val registry = ConveyWeightRegistry()
        registry.register(Any(), ConveyWeight.Hero)
        registry.register(Any(), ConveyWeight.Secondary)
        registry.register(Any(), ConveyWeight.Secondary)
        registry.register(Any(), ConveyWeight.Ghost)

        assertEquals(1, registry.heroCount)
        assertEquals(0, registry.primaryCount)
        assertEquals(2, registry.secondaryCount)
        assertEquals(1, registry.ghostCount)
    }

    @Test
    fun unregisterRemovesTheEntry() {
        val registry = ConveyWeightRegistry()
        val id = Any()
        registry.register(id, ConveyWeight.Hero)
        registry.unregister(id)
        assertEquals(0, registry.heroCount)
    }

    @Test
    fun aSecondHeroViolatesTheHierarchy() {
        val registry = ConveyWeightRegistry()
        registry.register(Any(), ConveyWeight.Hero)
        assertFailsWith<ConveyViolationException> {
            registry.register(Any(), ConveyWeight.Hero)
        }
    }

    @Test
    fun primaryCountBeyondTheConfiguredMaxViolatesTheHierarchy() {
        val registry = ConveyWeightRegistry(maxPrimary = 2)
        registry.register(Any(), ConveyWeight.Primary)
        registry.register(Any(), ConveyWeight.Primary)
        assertFailsWith<ConveyViolationException> {
            registry.register(Any(), ConveyWeight.Primary)
        }
    }

    @Test
    fun disablingEnforcementAllowsExcessPrimaries() {
        val registry = ConveyWeightRegistry(maxPrimary = 1, enforceInDebug = false)
        registry.register(Any(), ConveyWeight.Primary)
        registry.register(Any(), ConveyWeight.Primary)
        assertEquals(2, registry.primaryCount)
    }

    @Test
    fun snapshotReportsEveryCount() {
        val registry = ConveyWeightRegistry(maxPrimary = 3)
        registry.register(Any(), ConveyWeight.Hero)
        val report = registry.snapshot()
        assertEquals(true, "Hero:      1" in report)
        assertEquals(true, "max 3" in report)
    }
}
