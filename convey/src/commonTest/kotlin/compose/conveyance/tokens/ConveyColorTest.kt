package compose.conveyance.tokens

import compose.conveyance.ConveyWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConveyColorTest {

    @Test
    fun heroAndPrimaryUsePrimaryContainers() {
        assertEquals(ConveyColor.Primary, ConveyColor.containerFor(ConveyWeight.Hero))
        assertEquals(ConveyColor.PrimaryContainer, ConveyColor.containerFor(ConveyWeight.Primary))
    }

    @Test
    fun eachWeightGetsADistinctContainer() {
        val containers = ConveyWeight.entries.map { ConveyColor.containerFor(it) }
        assertEquals(containers.size, containers.toSet().size)
    }

    @Test
    fun contentColorPairsWithItsContainer() {
        assertNotEquals(
            ConveyColor.contentFor(ConveyWeight.Ghost),
            ConveyColor.contentFor(ConveyWeight.Hero),
        )
    }
}
