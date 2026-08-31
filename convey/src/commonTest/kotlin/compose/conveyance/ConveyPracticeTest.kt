package compose.conveyance

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConveyPracticeTest {

    @Test
    fun decayIsFullCeremonyAtZeroOperations() {
        assertEquals(1f, conveyPracticeDecay(operationCount = 0))
    }

    @Test
    fun decayApproachesTheFloorAsOperationsGrowWithoutGoingBelowIt() {
        val decay = conveyPracticeDecay(operationCount = 1000, floor = 0.4f, halfLife = 5)
        assertTrue(decay >= 0.4f)
        assertTrue(decay < 0.41f)
    }

    @Test
    fun decayIsMonotonicallyNonIncreasingWithMoreOperations() {
        var previous = conveyPracticeDecay(operationCount = 0)
        for (count in 1..20) {
            val current = conveyPracticeDecay(operationCount = count)
            assertTrue(current <= previous, "decay increased from $previous to $current at count $count")
            previous = current
        }
    }

    @Test
    fun decayRejectsAnOutOfRangeFloor() {
        assertFailsWith<IllegalArgumentException> { conveyPracticeDecay(operationCount = 0, floor = 1.5f) }
    }

    @Test
    fun decayRejectsANonPositiveHalfLife() {
        assertFailsWith<IllegalArgumentException> { conveyPracticeDecay(operationCount = 0, halfLife = 0) }
    }

    @Test
    fun decayedLeavesATweenUnchangedAtFullCeremony() {
        val original = tween<Float>(durationMillis = 300, delayMillis = 20, easing = FastOutSlowInEasing)
        val result = original.decayed(decay = 1f) as TweenSpec<Float>
        assertEquals(300, result.durationMillis)
        assertEquals(20, result.delay)
    }

    @Test
    fun decayedShortensATweenProportionallyButNotBelowTheMinimum() {
        val original = tween<Float>(durationMillis = 300)
        val result = original.decayed(decay = 0.5f) as TweenSpec<Float>
        assertEquals(150, result.durationMillis)

        val tiny = tween<Float>(durationMillis = 100)
        val floored = tiny.decayed(decay = 0.1f, minDurationMillis = 80) as TweenSpec<Float>
        assertEquals(80, floored.durationMillis)
    }

    @Test
    fun decayedStiffensASpringProportionallyButNotAboveTheMaximum() {
        val original = spring<Float>(stiffness = 300f)
        val result = original.decayed(decay = 0.5f) as SpringSpec<Float>
        assertEquals(600f, result.stiffness)

        val alreadyStiff = spring<Float>(stiffness = 10000f)
        val capped = alreadyStiff.decayed(decay = 0.1f, maxStiffness = 20000f) as SpringSpec<Float>
        assertEquals(20000f, capped.stiffness)
    }

    @Test
    fun registryTracksOperationCountsPerKeyIndependently() {
        val registry = ConveyPracticeRegistry()
        registry.recordOperation("a")
        registry.recordOperation("a")
        registry.recordOperation("b")

        assertEquals(2, registry.operationCount("a"))
        assertEquals(1, registry.operationCount("b"))
        assertEquals(0, registry.operationCount("never touched"))
    }

    @Test
    fun registrySeedOverridesTheCountDirectly() {
        val registry = ConveyPracticeRegistry()
        registry.recordOperation("a")

        registry.seed("a", 42)

        assertEquals(42, registry.operationCount("a"))
    }
}
