package compose.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConveyNounLexiconTest {

    @Test
    fun classifiesPrototypicalAnimalsAsAnimate() {
        assertEquals(ConveyNounAnimacy.Animate, ConveyNounLexicon.classify("cheetah")?.animacy)
        assertEquals(ConveyNounAnimacy.Animate, ConveyNounLexicon.classify("gazelle")?.animacy)
        assertEquals(ConveyNounAnimacy.Animate, ConveyNounLexicon.classify("dog")?.animacy)
        assertEquals(ConveyNounAnimacy.Animate, ConveyNounLexicon.classify("person")?.animacy)
    }

    @Test
    fun classifiesPrototypicalArtifactsAsInanimate() {
        assertEquals(ConveyNounAnimacy.Inanimate, ConveyNounLexicon.classify("rock")?.animacy)
        assertEquals(ConveyNounAnimacy.Inanimate, ConveyNounLexicon.classify("table")?.animacy)
        assertEquals(ConveyNounAnimacy.Inanimate, ConveyNounLexicon.classify("chair")?.animacy)
    }

    @Test
    fun classifiesSubstancesAsMass() {
        assertEquals(ConveyNounCountability.Mass, ConveyNounLexicon.classify("water")?.countability)
        assertEquals(ConveyNounCountability.Mass, ConveyNounLexicon.classify("sand")?.countability)
    }

    @Test
    fun disambiguatesPolysemousNounsViaContext() {
        // "jelly"'s primary (most frequent) WordNet sense is the edible dessert (noun.food,
        // Count); only its third sense ("any substance having the consistency of jelly or
        // gelatin") is noun.substance. Without context the primary sense wins, honestly -- with
        // disambiguating context whose vocabulary overlaps that third sense's own gloss
        // ("consistency"), Simplified Lesk resolves to it instead.
        assertEquals(ConveyNounCountability.Count, ConveyNounLexicon.classify("jelly")?.countability)
        assertEquals(
            ConveyNounCountability.Mass,
            ConveyNounLexicon.classify("jelly", context = "a soft wobbling consistency")?.countability,
        )
    }

    @Test
    fun classifiesOrdinaryObjectsAsCount() {
        assertEquals(ConveyNounCountability.Count, ConveyNounLexicon.classify("cheetah")?.countability)
        assertEquals(ConveyNounCountability.Count, ConveyNounLexicon.classify("gazelle")?.countability)
        assertEquals(ConveyNounCountability.Count, ConveyNounLexicon.classify("table")?.countability)
    }

    @Test
    fun handlesIrregularPluralsViaNounExc() {
        assertEquals(ConveyNounAnimacy.Animate, ConveyNounLexicon.classify("mice")?.animacy)
        assertEquals(ConveyNounAnimacy.Animate, ConveyNounLexicon.classify("children")?.animacy)
    }

    @Test
    fun returnsNullForWordsWithNoWordNetNounEntry() {
        assertNull(ConveyNounLexicon.classify("zzqxblorp"))
    }

    @Test
    fun lemmatizesRegularPlurals() {
        assertEquals("cheetah", ConveyNounLexicon.lemmatize("cheetahs"))
        assertEquals("gazelle", ConveyNounLexicon.lemmatize("gazelles"))
        assertEquals("fox", ConveyNounLexicon.lemmatize("foxes"))
        assertEquals("city", ConveyNounLexicon.lemmatize("cities"))
    }
}
