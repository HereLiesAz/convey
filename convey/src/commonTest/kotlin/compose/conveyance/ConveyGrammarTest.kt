package compose.conveyance

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConveyGrammarTest {

    @Test
    fun getReturnsTheDeclaredSpec() {
        val grammar = buildConveyGrammar {
            meaning("navigate") { spring(stiffness = 100f) }
        }
        // Just needs to resolve without throwing -- the spec itself isn't structurally comparable.
        grammar["navigate"]
    }

    @Test
    fun getThrowsOnUnknownMeaning() {
        val grammar = buildConveyGrammar {
            meaning("navigate") { spring() }
        }
        val error = assertFailsWith<IllegalStateException> { grammar["missing"] }
        assertTrue("missing" in error.message.orEmpty())
        assertTrue("navigate" in error.message.orEmpty())
    }

    @Test
    fun entryThrowsOnUnknownMeaning() {
        val grammar = buildConveyGrammar {
            meaning("navigate") { spring() }
        }
        assertFailsWith<IllegalStateException> { grammar.entry("missing") }
    }

    @Test
    fun meaningRejectsBlankNames() {
        assertFailsWith<IllegalStateException> {
            buildConveyGrammar { meaning("") { spring() } }
        }
    }

    @Test
    fun meaningRejectsDuplicateDeclaration() {
        assertFailsWith<IllegalStateException> {
            buildConveyGrammar {
                meaning("navigate") { spring() }
                meaning("navigate") { tween(200) }
            }
        }
    }

    @Test
    fun vocabularyReflectsDeclaredMeanings() {
        val grammar = buildConveyGrammar {
            meaning("navigate") { spring() }
            meaning("confirm") { spring() }
        }
        assertEquals(setOf("navigate", "confirm"), grammar.vocabulary)
    }

    @Test
    fun defaultGrammarDeclaresTheDocumentedMeanings() {
        val expected = setOf("navigate", "reveal", "confirm", "dismiss", "morph", "load", "error", "delight")
        assertEquals(expected, ConveyGrammar.Default.vocabulary)
    }

    @Test
    fun auditListsEveryMeaning() {
        val grammar = buildConveyGrammar {
            meaning("navigate", "moves the user") { spring() }
        }
        val report = grammar.audit()
        assertTrue("navigate" in report)
        assertTrue("moves the user" in report)
    }

    @Test
    fun auditWarnsPastEightMeanings() {
        val grammar = buildConveyGrammar {
            repeat(9) { i -> meaning("m$i") { spring() } }
        }
        assertTrue("a lot" in grammar.audit())
    }

    @Test
    fun extendAddsMeaningsWithoutLosingTheBase() {
        val extended = ConveyGrammar.Default.extend {
            meaning("celebrate") { spring(stiffness = 900f) }
        }
        assertTrue(ConveyGrammar.Default.vocabulary.all { it in extended.vocabulary })
        assertTrue("celebrate" in extended.vocabulary)
    }

    @Test
    fun extendRejectsAMeaningThatConflictsWithTheBase() {
        assertFailsWith<IllegalStateException> {
            ConveyGrammar.Default.extend {
                meaning("navigate") { spring(stiffness = 1f) }
            }
        }
    }
}
