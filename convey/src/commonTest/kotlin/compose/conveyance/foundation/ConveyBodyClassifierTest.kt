package compose.conveyance.foundation

import compose.conveyance.ConveyLife
import compose.conveyance.ConveyNounAnimacy
import compose.conveyance.ConveyNounCountability
import compose.conveyance.ConveyVerbClass
import compose.conveyance.ConveyVerbLexicon
import compose.conveyance.toConveyLife
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConveyBodyClassifierTest {

    @Test
    fun verbWeightDeltaIsPositiveForForcefulPhysicalClasses() {
        assertTrue(ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.Competition) > 0f)
        assertTrue(ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.Contact) > 0f)
        assertTrue(ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.MannerAgent) > 0f)
    }

    @Test
    fun verbWeightDeltaIsNegativeForMentalOrStativeClasses() {
        assertTrue(ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.Cognition) < 0f)
        assertTrue(ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.Stative) < 0f)
        assertTrue(ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.Possession) < 0f)
    }

    @Test
    fun verbWeightDeltaIsNeutralForEverythingElse() {
        assertEquals(0f, ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.Communication))
        assertEquals(0f, ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.Emotion))
        assertEquals(0f, ConveyBodyClassifier.verbWeightDelta(ConveyVerbClass.Unclassified))
    }

    @Test
    fun nounWeightDeltaAddsForAnimacyAndSubtractsForMass() {
        assertEquals(0f, ConveyBodyClassifier.nounWeightDelta(ConveyNounAnimacy.Inanimate, ConveyNounCountability.Count))
        assertTrue(ConveyBodyClassifier.nounWeightDelta(ConveyNounAnimacy.Animate, ConveyNounCountability.Count) > 0f)
        assertTrue(ConveyBodyClassifier.nounWeightDelta(ConveyNounAnimacy.Inanimate, ConveyNounCountability.Mass) < 0f)
    }

    @Test
    fun nounWeightDeltaBucketsCombineAdditively() {
        val animateOnly = ConveyBodyClassifier.nounWeightDelta(ConveyNounAnimacy.Animate, ConveyNounCountability.Count)
        val massOnly = ConveyBodyClassifier.nounWeightDelta(ConveyNounAnimacy.Inanimate, ConveyNounCountability.Mass)
        val both = ConveyBodyClassifier.nounWeightDelta(ConveyNounAnimacy.Animate, ConveyNounCountability.Mass)
        assertEquals(animateOnly + massOnly, both)
    }

    @Test
    fun classifyWordResolvesARealVerbConsistentlyWithVerbWeightDeltaAndToConveyLife() {
        val context = "The cheetah sprints across the plain"
        val word = "sprints"
        val verbClass = ConveyVerbLexicon.classify(word, context)
        assertTrue(verbClass != ConveyVerbClass.Unclassified, "expected '$word' to resolve to a real WordNet verb sense")

        val result = ConveyBodyClassifier.classifyWord(word, context)
        assertEquals(ConveyBodyClassifier.BASE_WEIGHT + ConveyBodyClassifier.verbWeightDelta(verbClass), result.weight)
        assertEquals(verbClass.toConveyLife(), result.idle)
    }

    @Test
    fun classifyWordResolvesARealNounWithNoVerbSenseToWeightOnlyNoMotion() {
        // "cheetah" has no WordNet verb entry, so classifyWord falls through to the noun
        // lexicon -- confirmed animate, count in ConveyNounLexiconTest already.
        val result = ConveyBodyClassifier.classifyWord("cheetah", context = "The cheetah ran across the plain")
        assertEquals(ConveyLife.None, result.idle)
        assertEquals(ConveyBodyClassifier.BASE_WEIGHT + ConveyBodyClassifier.nounWeightDelta(ConveyNounAnimacy.Animate, ConveyNounCountability.Count), result.weight)
    }

    @Test
    fun classifyWordFallsBackToBaseWeightAndNoMotionForAnUnrecognizedToken() {
        val result = ConveyBodyClassifier.classifyWord("xyzzy123", context = "xyzzy123 is not a word")
        assertEquals(ConveyBodyClassifier.BASE_WEIGHT, result.weight)
        assertEquals(ConveyLife.None, result.idle)
    }
}
