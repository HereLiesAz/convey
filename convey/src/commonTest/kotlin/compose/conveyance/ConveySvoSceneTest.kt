package compose.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConveySvoSceneTest {

    @Test
    fun splitsTheReportsOwnWorkedExample() {
        val parts = parseSvoHeuristic("The cheetah hunts the gazelle")
        assertEquals(ConveySvoParts(subject = "cheetah", verb = "hunts", obj = "gazelle"), parts)
    }

    @Test
    fun takesTheHeadNounOfMultiWordPhrases() {
        // "quick" has no WordNet verb entry at all, so the verb scan correctly skips past it to
        // "pursues" rather than mistaking a modifier for the verb.
        val parts = parseSvoHeuristic("The quick cheetah pursues the quick gazelle")
        assertEquals("cheetah", parts?.subject)
        assertEquals("pursues", parts?.verb)
        assertEquals("gazelle", parts?.obj)
    }

    @Test
    fun returnsNullForTooFewWords() {
        assertNull(parseSvoHeuristic("cheetah"))
        assertNull(parseSvoHeuristic("cheetah hunts"))
    }

    @Test
    fun fallsBackToSecondWordAsVerbWhenNothingElseClassifies() {
        // A known, documented heuristic limitation (see parseSvoHeuristic's own doc comment):
        // with exactly three words and no classifiable verb found by the scan, the fallback
        // guesses the second word is the verb regardless of whether that is linguistically
        // correct -- this is not a real parser.
        val parts = parseSvoHeuristic("The cheetah gazelle")
        assertEquals(ConveySvoParts(subject = "The", verb = "cheetah", obj = "gazelle"), parts)
    }
}
