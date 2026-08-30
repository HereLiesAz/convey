package compose.conveyance.foundation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import compose.conveyance.ConveyTopographicalCategory
import compose.conveyance.ConveyVerbLexicon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A procedural rule for placing successive words of a sentence in static 2D space (px,
 * y-down) — the report's §"Static Topographical Alignment and Concrete Poetry" generalized
 * from its one worked example (a downward staircase) into a pluggable shape, not a single
 * hardcoded layout. [ConveyTopographicalPaths] supplies the built-in shapes; write your own
 * for anything else — it is one function.
 *
 * @param index This word's position in the sentence, 0-based.
 * @param total Total word count in the sentence.
 * @param previous The previous word's own resolved placement, or null for the first word.
 * @param size This word's own measured size.
 * @return This word's top-left placement, in the same coordinate space as [previous].
 */
fun interface ConveyTopographicalPath {
    fun next(index: Int, total: Int, previous: PlacedWord?, size: IntSize): IntOffset
}

/** One word's resolved static placement — see [ConveyTopographicalPath]. */
data class PlacedWord(val position: IntOffset, val size: IntSize)

/**
 * The built-in procedural shapes, each grounded in a [ConveyTopographicalCategory] — see
 * [ConveyVerbLexicon.topographicalCategory] for how a sentence's own verb resolves to one.
 */
object ConveyTopographicalPaths {

    /**
     * The report's own worked example, verbatim: each word starts exactly where the previous
     * word's own measured bounds ended — "incrementally shifting the horizontal offset...
     * rightward just beyond the end of the preceding word."
     */
    fun descent(extraLineGapPx: Float = 0f): ConveyTopographicalPath =
        ConveyTopographicalPath { _, _, previous, _ ->
            previous?.let {
                IntOffset(it.position.x + it.size.width, it.position.y + it.size.height + extraLineGapPx.roundToInt())
            } ?: IntOffset.Zero
        }

    /** [descent]'s mirror: the same rightward continuation, but each line climbs upward. */
    fun ascent(extraLineGapPx: Float = 0f): ConveyTopographicalPath =
        ConveyTopographicalPath { _, _, previous, size ->
            previous?.let {
                IntOffset(it.position.x + it.size.width, it.position.y - size.height - extraLineGapPx.roundToInt())
            } ?: IntOffset.Zero
        }

    val Descent: ConveyTopographicalPath = descent()
    val Ascent: ConveyTopographicalPath = ascent()

    /**
     * Words spiral outward from a shared origin, growing farther apart as the sentence
     * proceeds — the visual reading of "scatter": nothing stays where it started.
     */
    val Scatter: ConveyTopographicalPath = ConveyTopographicalPath { index, _, _, size ->
        val angle = index * 2.4 // golden-angle-adjacent spread -- successive words don't stack radially
        val radius = index * ((size.width + size.height) / 2.0)
        IntOffset((radius * cos(angle)).roundToInt(), (radius * sin(angle)).roundToInt())
    }

    /**
     * Words distributed evenly around a shared center, radius sized so the ring roughly fits
     * the sentence's own total length — the visual reading of "encircle": every word equidistant
     * from one shared point, none ahead of another.
     */
    val Encircle: ConveyTopographicalPath = ConveyTopographicalPath { index, total, _, size ->
        val n = total.coerceAtLeast(1)
        val angle = 2.0 * PI * index / n
        val radius = (n * (size.width + size.height) / 2.0) / (2.0 * PI) + size.width
        IntOffset((radius * cos(angle)).roundToInt(), (radius * sin(angle)).roundToInt())
    }

    fun forCategory(category: ConveyTopographicalCategory): ConveyTopographicalPath = when (category) {
        ConveyTopographicalCategory.Descent -> Descent
        ConveyTopographicalCategory.Ascent -> Ascent
        ConveyTopographicalCategory.Scatter -> Scatter
        ConveyTopographicalCategory.Encircle -> Encircle
    }
}

/**
 * A sentence laid out by a procedural [ConveyTopographicalPath] instead of ordinary text flow —
 * static, non-animated positioning where the sentence's own physical arrangement on the page IS
 * the meaning, exactly as concrete poetry uses layout instead of motion.
 *
 * This overload takes the shape explicitly. For a sentence that should pick its own shape from
 * its own verb, use the other [ConveyTopographicalLayout] overload below.
 *
 * @param text The sentence. Split on whitespace; each word is placed independently by [path].
 */
@Composable
fun ConveyTopographicalLayout(
    text: String,
    path: ConveyTopographicalPath,
    style: TextStyle = TextStyle.Default,
    modifier: Modifier = Modifier,
) {
    val words = remember(text) { text.split(Regex("\\s+")).filter { it.isNotEmpty() } }

    Layout(
        content = { words.forEach { word -> Text(text = word, style = style) } },
        modifier = modifier,
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(looseConstraints) }

        var previous: PlacedWord? = null
        val placed = placeables.mapIndexed { index, placeable ->
            val size = IntSize(placeable.width, placeable.height)
            val position = path.next(index, placeables.size, previous, size)
            PlacedWord(position, size).also { previous = it }
        }

        if (placed.isEmpty()) {
            layout(0, 0) {}
        } else {
            val minX = placed.minOf { it.position.x }
            val minY = placed.minOf { it.position.y }
            val maxX = placed.maxOf { it.position.x + it.size.width }
            val maxY = placed.maxOf { it.position.y + it.size.height }

            layout((maxX - minX).coerceAtMost(constraints.maxWidth), (maxY - minY).coerceAtMost(constraints.maxHeight)) {
                placeables.forEachIndexed { index, placeable ->
                    val p = placed[index]
                    placeable.placeRelative(p.position.x - minX, p.position.y - minY)
                }
            }
        }
    }
}

/**
 * The procedural version: [text]'s own verb is looked up
 * ([ConveyVerbLexicon.topographicalCategory], with [text] itself as disambiguating context) to
 * pick a [ConveyTopographicalPaths] shape automatically. A sentence whose verb suggests no
 * category (most sentences) renders as ordinary flowing [Text] instead — this composable decides
 * for itself whether the sentence's own geometry deserves a shape; it does not apply one blindly.
 *
 * ```kotlin
 * ConveyTopographicalLayout("The leaves fell to the ground")   // -> descent staircase
 * ConveyTopographicalLayout("The crowd scattered in panic")    // -> outward spiral
 * ConveyTopographicalLayout("The wolves surrounded the camp")  // -> a ring
 * ConveyTopographicalLayout("She walked to the store")         // -> ordinary text; no category
 * ```
 */
@Composable
fun ConveyTopographicalLayout(
    text: String,
    style: TextStyle = TextStyle.Default,
    modifier: Modifier = Modifier,
) {
    val words = remember(text) { text.split(Regex("\\s+")).filter { it.isNotEmpty() } }
    val category = remember(text, words) {
        words.firstNotNullOfOrNull { word -> ConveyVerbLexicon.topographicalCategory(word, text) }
    }

    if (category == null) {
        Text(text = text, style = style, modifier = modifier)
    } else {
        ConveyTopographicalLayout(text = text, path = ConveyTopographicalPaths.forCategory(category), style = style, modifier = modifier)
    }
}
