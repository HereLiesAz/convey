package compose.conveyance.foundation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A sentence laid out as a static, non-animated descending staircase — one word per line, each
 * line starting exactly where the previous word's own rendered width ended.
 *
 * This is the report's §"Static Topographical Alignment and Concrete Poetry": "the algorithm
 * calculates a static DOM layout where each successive word is rendered on a new line,
 * incrementally shifting the horizontal offset... rightward just beyond the end of the preceding
 * word. This constraint-based procedural layout constructs a visual staircase." No animation
 * timeline drives this — the sentence's own physical arrangement on the page IS the meaning,
 * exactly as concrete poetry uses static layout instead of motion.
 *
 * Deliberately unconditional: this composable does not itself decide whether a sentence
 * "deserves" the staircase. Callers decide that — typically by checking
 * [compose.conveyance.ConveyVerbLexicon.isDescent] against the sentence's own verb first (see the
 * example below). Applying the layout is a design choice, not something to infer silently on
 * every render.
 *
 * ```kotlin
 * val sentence = "The leaves fell to the ground"
 * val verb = "fell"
 * if (ConveyVerbLexicon.isDescent(verb, context = sentence)) {
 *     ConveyDescentLayout(text = sentence, style = MaterialTheme.typography.bodyLarge)
 * } else {
 *     Text(sentence, style = MaterialTheme.typography.bodyLarge)
 * }
 * ```
 *
 * @param text The sentence. Split on whitespace; each word becomes one line of the staircase.
 * @param lineSpacing Extra vertical gap added between each line, beyond the text's own line height.
 */
@Composable
fun ConveyDescentLayout(
    text: String,
    style: TextStyle = TextStyle.Default,
    lineSpacing: Dp = 4.dp,
    modifier: Modifier = Modifier,
) {
    val words = remember(text) { text.split(Regex("\\s+")).filter { it.isNotEmpty() } }

    Layout(
        content = { words.forEach { word -> Text(text = word, style = style) } },
        modifier = modifier,
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(looseConstraints) }
        val lineSpacingPx = lineSpacing.roundToPx()

        var x = 0
        var y = 0
        var maxWidth = 0
        val positions = placeables.map { placeable ->
            val position = x to y
            x += placeable.width
            y += placeable.height + lineSpacingPx
            maxWidth = maxOf(maxWidth, x)
            position
        }
        val totalHeight = if (placeables.isEmpty()) 0 else y - lineSpacingPx

        layout(maxWidth.coerceAtMost(constraints.maxWidth), totalHeight.coerceAtMost(constraints.maxHeight)) {
            placeables.forEachIndexed { index, placeable ->
                val (px, py) = positions[index]
                placeable.placeRelative(px, py)
            }
        }
    }
}
