package compose.conveyance.foundation

import compose.conveyance.ConveyWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConveyExpressiveBadgeTest {
    @Test
    fun accentWeightForCyclesThroughThreeDistinctWeights() {
        assertEquals(ConveyWeight.Primary, accentWeightFor(ConveyWeight.Hero))
        assertEquals(ConveyWeight.Secondary, accentWeightFor(ConveyWeight.Primary))
        assertEquals(ConveyWeight.Hero, accentWeightFor(ConveyWeight.Secondary))
    }

    @Test
    fun accentWeightForNeverMatchesItsOwnInput() {
        for (weight in ConveyWeight.entries) {
            assertNotEquals(weight, accentWeightFor(weight))
        }
    }

    @Test
    fun accentWeightForGivesGhostASecondaryFallback() {
        assertEquals(ConveyWeight.Hero, accentWeightFor(ConveyWeight.Ghost))
    }
}
