package compose.conveyance.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.conveyance.ConveyKineticText
import compose.conveyance.ConveyLife
import compose.conveyance.ConveyNounAnimacy
import compose.conveyance.ConveyNounCountability
import compose.conveyance.ConveyNounLexicon
import compose.conveyance.ConveyVerbClass
import compose.conveyance.ConveyVerbLexicon
import compose.conveyance.toConveyLife
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyTypeVariation
import compose.conveyance.tokens.conveyTypeFontFamily

/**
 * `DESIGN`'s (Part XI) sibling for body-level prose — Part XII of the Conveyance Manifesto,
 * "The Body Block" (`docs/CONVEYANCE-FRAMEWORK.md`). Where `DESIGN` composes the small number of
 * dominant, occasional lines that establish a page's hierarchy, `CONVEY BODY` is for the prose
 * read at length beneath them: `Paragraph`, `Quote`, and whatever other body-level role a
 * platform needs.
 *
 * `CONVEY BODY` never chooses a paragraph's semantic role or touches its place in the
 * accessibility tree — it is strictly additive, layering motion and weight on top of a role that
 * was already there. Unlike `DESIGN`'s per-line `motion` (offered, never assumed — §4.2), every
 * effect here applies to everything inside the block, uniformly: prose read at length that
 * sometimes moves and sometimes doesn't reads as broken in a way an occasional heading does not.
 *
 * **One classification, two outputs (§12.2).** [ConveyBodyClassifier] runs the same verb/noun
 * classification already built for kinetic typography ([ConveyVerbLexicon]/[ConveyNounLexicon])
 * once per word, and that single result drives both the emotive motion (§12.3, via the existing
 * [toConveyLife] mapping [compose.conveyance.ConveyKineticSentence] already uses) and a fluid
 * font weight (§12.4) — no longer fixed per semantic level the way `DESIGN`'s `Title`/`Body`
 * weights are.
 *
 * **Mandatory scroll-linked entrance (§12.5).** Every line performs an entrance transform as it
 * scrolls into view, direction keyed to its role: `Paragraph` enters horizontally, `Quote`
 * vertically. [ConveyBody] owns its own scroll container (a `verticalScroll` `Column`) rather
 * than reading an externally supplied scroll state, per the spec's own reasoning: the transform
 * needs authoritative, unshared knowledge of exactly where every line sits relative to the
 * viewport.
 *
 * **Implementation status:** condensation/weight render through real Azrienoch `wght` axes via
 * [conveyTypeFontFamily], one `FontFamily` resolution per word's fluid weight (consistent with
 * [ConveyDesign]'s own per-line resolution — see that file's doc comment for the general caveat
 * on this project's Azrienoch integration). The weight-delta mapping in [ConveyBodyClassifier] is
 * a ratio tool in the same spirit as [ConveyDesignSolver.inkScore] — a deliberate, documented
 * judgment call about which verb classes should read as "heavier," not an empirically validated
 * scale.
 */
enum class ConveyBodyRole(internal val parallaxDirection: ConveyParallaxDirection) {
    Paragraph(ConveyParallaxDirection.Horizontal),
    Quote(ConveyParallaxDirection.Vertical),
}

@Immutable
data class ConveyBodyLine(
    val text: String,
    val role: ConveyBodyRole = ConveyBodyRole.Paragraph,
)

/**
 * The one classification pass §12.2 describes, and the pure weight-delta math it spends on
 * §12.4's fluid weight. Kept free of any Composable/UI dependency for the delta functions so
 * they are directly unit-testable, the same discipline [ConveyDesignSolver] follows;
 * [classifyWord] itself calls into [ConveyVerbLexicon]/[ConveyNounLexicon], which resolve
 * synchronously against data compiled into this library (no async loading step on this
 * platform, unlike the web port).
 */
object ConveyBodyClassifier {

    const val BASE_WEIGHT = 400f

    @Immutable
    data class WordClassification(val word: String, val idle: ConveyLife, val weight: Float)

    /**
     * Heavy: classes describing forceful, physical, or competitive action. Light: classes
     * describing mental/perceptual states or possession with no physical force behind it.
     * Everything else (Change, Communication, Consumption, Creation, Emotion, Social, Weather,
     * StateMetaphor, Unclassified) stays at [BASE_WEIGHT] — a deliberate, documented judgment
     * call in three buckets rather than 22 individually tuned deltas, in the same spirit as
     * [ConveyDesignSolver.inkScore]'s own ratio-tool framing.
     */
    fun verbWeightDelta(verbClass: ConveyVerbClass): Float = when (verbClass) {
        ConveyVerbClass.Competition,
        ConveyVerbClass.MannerAgent,
        ConveyVerbClass.Contact,
        ConveyVerbClass.Punctual,
        ConveyVerbClass.Body,
        ConveyVerbClass.SubtleBody,
        ConveyVerbClass.Motion,
        -> HEAVY_DELTA

        ConveyVerbClass.Cognition,
        ConveyVerbClass.Perception,
        ConveyVerbClass.Possession,
        ConveyVerbClass.Stative,
        ConveyVerbClass.PurePath,
        ConveyVerbClass.Scalar,
        -> LIGHT_DELTA

        else -> 0f
    }

    /** An animate noun reads slightly heavier than an inanimate one; a mass noun slightly lighter than a count noun. Additive, so both can apply. */
    fun nounWeightDelta(animacy: ConveyNounAnimacy, countability: ConveyNounCountability): Float {
        var delta = 0f
        if (animacy == ConveyNounAnimacy.Animate) delta += NOUN_ANIMATE_DELTA
        if (countability == ConveyNounCountability.Mass) delta += NOUN_MASS_DELTA
        return delta
    }

    /**
     * Classifies one word against [context] (its whole containing line, the same
     * disambiguating-context convention [ConveyVerbLexicon.classify]/[ConveyNounLexicon.classify]
     * already use). A word that resolves as a verb drives both outputs from [ConveyVerbClass];
     * failing that, a word that resolves as a noun drives weight alone from
     * [compose.conveyance.ConveyNounProperties] (nouns carry no motion mapping); anything neither
     * lexicon can place renders still, at [BASE_WEIGHT].
     */
    fun classifyWord(word: String, context: String): WordClassification {
        val verbClass = ConveyVerbLexicon.classify(word, context)
        if (verbClass != ConveyVerbClass.Unclassified) {
            return WordClassification(word, verbClass.toConveyLife(), BASE_WEIGHT + verbWeightDelta(verbClass))
        }

        val nounProps = ConveyNounLexicon.classify(word, context)
        if (nounProps != null) {
            return WordClassification(word, ConveyLife.None, BASE_WEIGHT + nounWeightDelta(nounProps.animacy, nounProps.countability))
        }

        return WordClassification(word, ConveyLife.None, BASE_WEIGHT)
    }

    private const val HEAVY_DELTA = 150f
    private const val LIGHT_DELTA = -100f
    private const val NOUN_ANIMATE_DELTA = 50f
    private const val NOUN_MASS_DELTA = -50f
}

/**
 * Renders a `CONVEY BODY` block: a self-scrolling column of [ConveyBodyLine]s, each classified
 * word-by-word and each line entering per §12.5 as it scrolls into view.
 */
@Composable
fun ConveyBody(
    lines: List<ConveyBodyLine>,
    modifier: Modifier = Modifier,
    color: Color = ConveyColor.OnSurface,
    baseSizeSp: Float = 16f,
    parallaxDistance: Dp = 48.dp,
    entranceZoneFraction: Float = ConveyScrollParallax.DEFAULT_ENTRANCE_ZONE_FRACTION,
) {
    val scrollState = rememberScrollState()
    var viewportCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val distancePx = with(density) { parallaxDistance.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { viewportCoordinates = it }
            .verticalScroll(scrollState),
    ) {
        for (line in lines) {
            ConveyBodyLineRow(
                line = line,
                color = color,
                baseSizeSp = baseSizeSp,
                distancePx = distancePx,
                entranceZoneFraction = entranceZoneFraction,
                viewportCoordinates = { viewportCoordinates },
            )
        }
    }
}

@Composable
private fun ConveyBodyLineRow(
    line: ConveyBodyLine,
    color: Color,
    baseSizeSp: Float,
    distancePx: Float,
    entranceZoneFraction: Float,
    viewportCoordinates: () -> LayoutCoordinates?,
) {
    val words = remember(line.text) { line.text.split(Regex("\\s+")).filter { it.isNotEmpty() } }
    val classifications = remember(line.text, words) { words.map { ConveyBodyClassifier.classifyWord(it, line.text) } }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .conveyScrollParallax(
                direction = line.role.parallaxDirection,
                viewportCoordinates = viewportCoordinates,
                distancePx = distancePx,
                entranceZoneFraction = entranceZoneFraction,
            ),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        for (classification in classifications) {
            val style = TextStyle(
                color = color,
                fontSize = baseSizeSp.sp,
                fontFamily = conveyTypeFontFamily(ConveyTypeVariation(weight = classification.weight)),
            )
            ConveyKineticText(text = classification.word, idle = classification.idle, style = style)
        }
    }
}
