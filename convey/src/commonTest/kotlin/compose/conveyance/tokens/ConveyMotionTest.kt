package compose.conveyance.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConveyMotionTest {

    @Test
    fun criticallyDampedSpringsAreNotElastic() {
        val criticallyDamped = ConveyMotion.Standard.withDampingRatio(1f)
        assertFalse(criticallyDamped.isElastic)
        assertTrue(criticallyDamped.isCriticallyDamped)
    }

    @Test
    fun elasticAndHeroicSpringsOvershoot() {
        assertTrue(ConveyMotion.Elastic.isElastic)
        assertTrue(ConveyMotion.Heroic.isElastic)
    }

    @Test
    fun criticallyDampedIsTheComplementOfElastic() {
        assertEquals(ConveyMotion.Standard.isElastic, !ConveyMotion.Standard.isCriticallyDamped)
        assertEquals(ConveyMotion.Elastic.isElastic, !ConveyMotion.Elastic.isCriticallyDamped)
    }

    @Test
    fun withDampingRatioPreservesStiffness() {
        val retuned = ConveyMotion.Standard.withDampingRatio(0.5f)
        assertEquals(ConveyMotion.Standard.stiffness, retuned.stiffness)
        assertEquals(0.5f, retuned.dampingRatio)
    }

    @Test
    fun withStiffnessPreservesDampingRatio() {
        val retuned = ConveyMotion.Standard.withStiffness(999f)
        assertEquals(ConveyMotion.Standard.dampingRatio, retuned.dampingRatio)
        assertEquals(999f, retuned.stiffness)
    }

    @Test
    fun estimatedDurationIsPositiveForAnUnderdampedSpring() {
        assertTrue(ConveyMotion.Standard.estimatedDurationMs() > 0)
    }

    @Test
    fun estimatedDurationIsFiniteForACriticallyDampedSpring() {
        val critical = ConveyMotion.Standard.withDampingRatio(1f)
        assertTrue(critical.estimatedDurationMs() < Int.MAX_VALUE)
    }
}
