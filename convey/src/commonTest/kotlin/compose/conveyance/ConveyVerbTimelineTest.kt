package compose.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConveyVerbTimelineTest {

    @Test
    fun contactVerbsApproachAndCollide() {
        val timeline = ConveyVerbClass.Contact.toEventTimeline()
        assertTrue(timeline.approaches)
        assertTrue(timeline.contactAtEnd)
        assertFalse(timeline.continuousNoContact)
    }

    @Test
    fun locomotionVerbsMoveWithoutContact() {
        val timeline = ConveyVerbClass.MannerAgent.toEventTimeline()
        assertTrue(timeline.approaches)
        assertFalse(timeline.contactAtEnd)
        assertTrue(timeline.continuousNoContact)
    }

    @Test
    fun stativeAndEmotionVerbsHaveNoPhysicalMotion() {
        assertEquals(
            ConveyVerbEventTimeline(approaches = false, contactAtEnd = false, continuousNoContact = false, possessionTransfer = false),
            ConveyVerbClass.Stative.toEventTimeline(),
        )
        assertEquals(
            ConveyVerbEventTimeline(approaches = false, contactAtEnd = false, continuousNoContact = false, possessionTransfer = false),
            ConveyVerbClass.Emotion.toEventTimeline(),
        )
    }

    @Test
    fun possessionApproximatesAsAttractionWithContact() {
        val timeline = ConveyVerbClass.Possession.toEventTimeline()
        assertTrue(timeline.possessionTransfer)
        assertTrue(timeline.contactAtEnd)
    }

    @Test
    fun realWordNetVerbsResolveExpectedTimelines() {
        // "hunt" -> a real WordNet/VerbNet sense classifying under Competition or Contact,
        // both of which approach-and-collide.
        val hunt = ConveyVerbLexicon.classify("hunts", "The cheetah hunts the gazelle").toEventTimeline()
        assertTrue(hunt.approaches)

        // "run" resolves to a manner-of-motion class (continuous, no contact).
        val run = ConveyVerbLexicon.classify("runs", "The cheetah runs across the field").toEventTimeline()
        assertTrue(run.approaches)
    }
}
