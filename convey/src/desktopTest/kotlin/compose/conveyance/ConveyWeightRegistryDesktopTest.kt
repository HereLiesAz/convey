package compose.conveyance

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

/**
 * Exercises [ConveyWeightRegistry]'s no-handler path against the real platform
 * [defaultViolationHandler]. This only belongs on `desktop` (or wasmJs/iOS, which share the
 * same unconditional-throw implementation): Android's own `defaultViolationHandler` is
 * `BuildConfig.DEBUG`-gated and deliberately logs instead of throwing on a release build, so
 * this exact assertion would fail if run as part of `commonTest` (which compiles into every
 * Android build variant, including `testReleaseUnitTest`) — see `ConveyWeightRegistryTest.kt`'s
 * own class doc for the fuller account.
 */
class ConveyWeightRegistryDesktopTest {

    @Test
    fun aSecondHeroThrowsThroughThePlatformDefaultHandler() {
        val registry = ConveyWeightRegistry()

        registry.register(id = "checkout", weight = ConveyWeight.Hero)

        val failure = assertFailsWith<ConveyViolationException> {
            registry.register(id = "banner", weight = ConveyWeight.Hero)
        }
        assertContains(failure.message ?: "", "2 Hero elements")
    }

    @Test
    fun exceedingMaxPrimaryThrowsThroughThePlatformDefaultHandler() {
        val registry = ConveyWeightRegistry(maxPrimary = 2)

        registry.register(id = "a", weight = ConveyWeight.Primary)
        registry.register(id = "b", weight = ConveyWeight.Primary)

        val failure = assertFailsWith<ConveyViolationException> {
            registry.register(id = "c", weight = ConveyWeight.Primary)
        }
        assertContains(failure.message ?: "", "3 Primary elements (max 2)")
    }
}
