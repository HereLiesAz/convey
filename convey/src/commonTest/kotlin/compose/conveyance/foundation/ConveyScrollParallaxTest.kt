package compose.conveyance.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConveyScrollParallaxTest {

    @Test
    fun entranceProgressIsZeroWhenItemSitsAtOrBelowTheViewportBottom() {
        assertEquals(0f, ConveyScrollParallax.entranceProgress(itemTopInViewport = 600f, viewportHeight = 600f))
        assertEquals(0f, ConveyScrollParallax.entranceProgress(itemTopInViewport = 900f, viewportHeight = 600f))
    }

    @Test
    fun entranceProgressIsOneOnceTheItemHasFullyCrossedTheEntranceZone() {
        // Default zone fraction is 0.5, so the zone's top edge sits at half the viewport height.
        assertEquals(1f, ConveyScrollParallax.entranceProgress(itemTopInViewport = 300f, viewportHeight = 600f))
        assertEquals(1f, ConveyScrollParallax.entranceProgress(itemTopInViewport = 0f, viewportHeight = 600f))
        assertEquals(1f, ConveyScrollParallax.entranceProgress(itemTopInViewport = -200f, viewportHeight = 600f))
    }

    @Test
    fun entranceProgressIsLinearInsideTheZone() {
        // Zone spans [300, 600) for a 600-tall viewport at the default 0.5 fraction; the
        // midpoint of the zone should read as roughly half-entered.
        val progress = ConveyScrollParallax.entranceProgress(itemTopInViewport = 450f, viewportHeight = 600f)
        assertTrue(progress in 0.4f..0.6f, "expected roughly 0.5, got $progress")
    }

    @Test
    fun entranceProgressRespectsACustomZoneFraction() {
        // A narrower zone (0.25) compresses the 0->1 transition into a smaller span near the
        // viewport's bottom edge, so at the same item position it reads as further along than
        // a wider zone does.
        val wideZoneProgress = ConveyScrollParallax.entranceProgress(itemTopInViewport = 500f, viewportHeight = 600f, entranceZoneFraction = 0.5f)
        val narrowZoneProgress = ConveyScrollParallax.entranceProgress(itemTopInViewport = 500f, viewportHeight = 600f, entranceZoneFraction = 0.25f)
        assertTrue(narrowZoneProgress > wideZoneProgress, "expected $narrowZoneProgress > $wideZoneProgress")
    }

    @Test
    fun degenerateViewportHeightIsTreatedAsFullyEntered() {
        assertEquals(1f, ConveyScrollParallax.entranceProgress(itemTopInViewport = 100f, viewportHeight = 0f))
    }

    @Test
    fun translationIsFullDistanceAtZeroProgressAndZeroAtFullProgress() {
        assertEquals(48f, ConveyScrollParallax.translation(progress = 0f, distance = 48f))
        assertEquals(0f, ConveyScrollParallax.translation(progress = 1f, distance = 48f))
    }

    @Test
    fun translationIsLinearInProgress() {
        assertEquals(24f, ConveyScrollParallax.translation(progress = 0.5f, distance = 48f))
    }

    @Test
    fun translationClampsProgressOutsideZeroToOne() {
        assertEquals(48f, ConveyScrollParallax.translation(progress = -1f, distance = 48f))
        assertEquals(0f, ConveyScrollParallax.translation(progress = 2f, distance = 48f))
    }
}
