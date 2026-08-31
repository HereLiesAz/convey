package compose.conveyance.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.tokens.ConveySize

/**
 * The framework's replacement for empty-state illustrations and their explanatory paragraphs.
 *
 * An empty collection does not display a message about being empty. It displays its creation
 * control, at full size, in the center of the space the collection will occupy. When the first
 * subject is created, that control performs `Create` — and then travels to the corner position
 * where it will live from now on, and shrinks into it.
 *
 * In one motion, with no words, a person learns: what this space is for, how to fill it, and
 * where the button will be for the rest of their life with this product. One element does the
 * job of an illustration, a paragraph, and a FAB — because it is the same element throughout,
 * not three things swapped for each other.
 *
 * ```kotlin
 * ConveyMigration(
 *     isEmpty = notes.isEmpty(),
 *     cornerAlignment = Alignment.BottomEnd,
 *     content = {
 *         LazyColumn { items(notes, key = { it.id }) { NoteRow(it) } }
 *     },
 *     creationControl = { size ->
 *         FloatingActionButton(onClick = { createNote() }, modifier = Modifier.size(size)) {
 *             Icon(Icons.Default.Add, contentDescription = null)
 *         }
 *     },
 * )
 * ```
 *
 * @param isEmpty Whether the collection currently has zero subjects. `true` keeps
 *   [creationControl] centered and full-size; `false` relocates it to [cornerAlignment] at
 *   [compactSize] and shows [content].
 * @param cornerAlignment Where [creationControl] lives once the collection has content — its
 *   permanent position, e.g. [Alignment.BottomEnd] for a FAB-style corner.
 * @param fullSize [creationControl]'s size while the collection is empty.
 * @param compactSize [creationControl]'s size once it has relocated.
 * @param grammar Motion vocabulary. Uses the "navigate" meaning for the relocation — it IS a
 *   move to a new place — and "morph" for the size change.
 * @param content The collection itself, shown once [isEmpty] is false.
 * @param creationControl The single element that is both the empty-state invitation and the
 *   permanent creation affordance. Receives the currently animated size to apply via its own
 *   `Modifier.size`.
 */
@Composable
fun ConveyMigration(
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    cornerAlignment: Alignment = Alignment.BottomEnd,
    fullSize: Dp = 96.dp,
    compactSize: Dp = ConveySize.Component.Fab,
    contentPadding: Dp = 16.dp,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    content: @Composable BoxScope.() -> Unit,
    creationControl: @Composable (size: Dp) -> Unit,
) {
    val navigateSpec = grammar["navigate"]
    val morphSpec = grammar["morph"]
    val (cornerHBias, cornerVBias) = biasOf(cornerAlignment)

    val hBias by animateFloatAsState(
        targetValue = if (isEmpty) 0f else cornerHBias,
        animationSpec = navigateSpec,
        label = "ConveyMigration.hBias",
    )
    val vBias by animateFloatAsState(
        targetValue = if (isEmpty) 0f else cornerVBias,
        animationSpec = navigateSpec,
        label = "ConveyMigration.vBias",
    )
    val animatedSize by animateFloatAsState(
        targetValue = if (isEmpty) fullSize.value else compactSize.value,
        animationSpec = morphSpec,
        label = "ConveyMigration.size",
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (!isEmpty) content()

        Box(
            modifier = Modifier
                .align(BiasAlignment(hBias, vBias))
                .padding(contentPadding),
        ) {
            creationControl(animatedSize.dp)
        }
    }
}

/** [Alignment]'s built-in instances are all [BiasAlignment]s; this reads their bias back out. */
private fun biasOf(alignment: Alignment): Pair<Float, Float> = when (alignment) {
    is BiasAlignment -> alignment.horizontalBias to alignment.verticalBias
    else -> 1f to 1f // an unrecognized custom Alignment defaults to bottom-end, the common FAB spot
}
