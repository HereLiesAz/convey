package compose.conveyance.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConveyReversalStateTest {

    @Test
    fun destroyMarksAnItemPendingWithoutRemovingItYet() {
        val state = ConveyReversalState(initial = listOf("a", "b", "c"))

        state.destroy("b")

        assertEquals("b", state.pending)
        assertEquals(listOf("a", "b", "c"), state.items)
    }

    @Test
    fun restoreClearsThePendingItemWithoutRemovingIt() {
        val state = ConveyReversalState(initial = listOf("a", "b"))
        state.destroy("a")

        state.restore()

        assertNull(state.pending)
        assertEquals(listOf("a", "b"), state.items)
    }

    @Test
    fun commitRemovesThePendingItemAndClearsPending() {
        val state = ConveyReversalState(initial = listOf("a", "b", "c"))
        state.destroy("b")

        state.commit()

        assertNull(state.pending)
        assertEquals(listOf("a", "c"), state.items)
    }

    @Test
    fun commitWithNothingPendingIsANoOp() {
        val state = ConveyReversalState(initial = listOf("a", "b"))

        state.commit()

        assertNull(state.pending)
        assertEquals(listOf("a", "b"), state.items)
    }

    @Test
    fun destroyingASecondItemCommitsTheFirstPendingOneImmediately() {
        // Only one reversal window is open at a time -- see ConveyReversalState.destroy's doc.
        val state = ConveyReversalState(initial = listOf("a", "b", "c"))
        state.destroy("a")

        state.destroy("b")

        assertEquals("b", state.pending)
        assertEquals(listOf("b", "c"), state.items) // "a" was committed, "b" stays pending
    }
}
