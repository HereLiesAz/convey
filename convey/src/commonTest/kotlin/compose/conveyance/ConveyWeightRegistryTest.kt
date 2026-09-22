package compose.conveyance

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Exercises [ConveyWeightRegistry] directly, including the path that actually throws.
 *
 * Unlike [ConveyEmploymentRegistryTest], these deliberately do *not* all disable enforcement:
 * the enforcement is the thing under test. Cases that rely on the platform
 * [defaultViolationHandler] unconditionally throwing are *not* here, because `commonTest` is
 * compiled into every Android build variant including `testReleaseUnitTest` — where Android's
 * own `BuildConfig.DEBUG`-gated handler deliberately logs instead of throwing (see
 * `ConveyViolation.android.kt`). Those default-handler-throws cases live in
 * `desktopTest/.../ConveyWeightRegistryDesktopTest.kt` instead, where an unconditional throw is
 * actually guaranteed.
 */
class ConveyWeightRegistryTest {

    @Test
    fun aSuppliedHandlerReceivesTheViolationInsteadOfThrowing() {
        val registry = ConveyWeightRegistry()
        val reported = mutableListOf<String>()
        val handler: (String) -> Unit = { reported += it }

        registry.register(id = "checkout", weight = ConveyWeight.Hero, onViolation = handler)
        registry.register(id = "banner", weight = ConveyWeight.Hero, onViolation = handler)

        assertEquals(1, reported.size)
        assertContains(reported.single(), "CONVEY HIERARCHY VIOLATION")
        assertEquals(2, registry.heroCount)
    }

    @Test
    fun oneHeroAndThreePrimariesIsWithinTheDefaultBudget() {
        val registry = ConveyWeightRegistry()

        registry.register(id = "hero", weight = ConveyWeight.Hero)
        registry.register(id = "p1", weight = ConveyWeight.Primary)
        registry.register(id = "p2", weight = ConveyWeight.Primary)
        registry.register(id = "p3", weight = ConveyWeight.Primary)
        registry.register(id = "s1", weight = ConveyWeight.Secondary)
        registry.register(id = "g1", weight = ConveyWeight.Ghost)

        assertEquals(1, registry.heroCount)
        assertEquals(3, registry.primaryCount)
        assertEquals(1, registry.secondaryCount)
        assertEquals(1, registry.ghostCount)
    }

    @Test
    fun unregisteringAHeroMakesRoomForAnother() {
        val registry = ConveyWeightRegistry()
        registry.register(id = "old", weight = ConveyWeight.Hero)

        registry.unregister("old")
        registry.register(id = "new", weight = ConveyWeight.Hero)

        assertEquals(1, registry.heroCount)
    }

    @Test
    fun enforcementCanBeDisabledForMigration() {
        val registry = ConveyWeightRegistry(enforceInDebug = false)

        registry.register(id = "a", weight = ConveyWeight.Hero)
        registry.register(id = "b", weight = ConveyWeight.Hero)

        assertEquals(2, registry.heroCount)
    }

    @Test
    fun snapshotReportsEveryTierAndTheConfiguredPrimaryMax() {
        val registry = ConveyWeightRegistry(maxPrimary = 2, enforceInDebug = false)
        registry.register(id = "hero", weight = ConveyWeight.Hero)
        registry.register(id = "p1", weight = ConveyWeight.Primary)

        val snapshot = registry.snapshot()

        assertEquals(
            "ConveyWeight Snapshot:\n" +
                "  Hero:      1  (max 1)\n" +
                "  Primary:   1  (max 2)\n" +
                "  Secondary: 0\n" +
                "  Ghost:     0\n",
            snapshot,
        )
        assertTrue(snapshot.isNotBlank())
    }
}
