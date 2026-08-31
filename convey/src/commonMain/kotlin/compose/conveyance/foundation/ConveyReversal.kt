package compose.conveyance.foundation

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.tokens.ConveyShape
import kotlinx.coroutines.delay

/**
 * The framework's replacement for confirmation dialogs, undo snackbars, and trash/archive
 * round-trips.
 *
 * A destroyed subject does not vanish behind a modal, and its undo does not live in a bar at
 * the bottom of the screen that steals the space and then leaves. It collapses in place into a
 * compact, reversible residue — occupying the space it held, in the position it held, for a
 * window. Tapping the residue restores the subject. Letting the window elapse lets it go.
 *
 * The reversal is located in the world, which is where a person's hand already is — not in a
 * modal before the fact, and not in an orphaned report after it.
 *
 * Not named `ConveyGhost`: [compose.conveyance.ConveyWeight.Ghost] already names a different,
 * unrelated concept in this library (a present-but-inert element, explicitly non-interactive).
 * This is the framework's *other* "Ghost" — the reversible residue of a destroyed subject — so
 * it gets its own name here to avoid two meanings for one identifier.
 *
 * ```kotlin
 * val state = remember { ConveyReversalState(initial = messages) }
 *
 * LazyColumn {
 *     items(state.items, key = { it.id }) { message ->
 *         ConveyReversal(item = message, state = state) {
 *             MessageRow(message, onDelete = { state.destroy(message) })
 *         }
 *     }
 * }
 * ```
 */
@Stable
class ConveyReversalState<T>(initial: List<T> = emptyList()) {
    var items: List<T> by mutableStateOf(initial)
        private set

    var pending: T? by mutableStateOf(null)
        private set

    /**
     * Marks [item] for destruction: it starts showing its residue instead of its ordinary
     * content. Only one reversal window is open at a time — a prior pending item commits
     * immediately first.
     */
    fun destroy(item: T) {
        commit()
        pending = item
    }

    /** Cancels the pending destruction. [pending] returns to ordinary display. */
    fun restore() {
        pending = null
    }

    /**
     * Commits the pending destruction immediately, removing it from [items]. Called
     * automatically when a [ConveyReversal]'s window elapses; safe to call when nothing is
     * pending.
     */
    fun commit() {
        val toRemove = pending ?: return
        items = items - toRemove
        pending = null
    }
}

/**
 * Wraps a single item's display. When [item] is [ConveyReversalState.pending] in [state], this
 * collapses to [residueContent] (a compact, tappable "undo" residue) for [windowMillis];
 * letting that window elapse without a tap commits the destruction via [ConveyReversalState.commit].
 * Tapping the residue restores the item via [ConveyReversalState.restore].
 *
 * @param windowMillis How long the residue stays reversible. The framework's guidance is that
 *   this should scale with the weight of what was destroyed — a single message is quick to
 *   reconsider, a whole conversation deserves longer — so callers of this composable choose it
 *   per call rather than this defaulting to one fixed value everywhere.
 * @param residueHeight The collapsed height while showing [residueContent].
 * @param residueShape The collapsed shape while showing [residueContent].
 */
@Composable
fun <T> ConveyReversal(
    item: T,
    state: ConveyReversalState<T>,
    windowMillis: Long = 4000L,
    residueHeight: Dp = 40.dp,
    residueShape: Shape = ConveyShape.Small,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    modifier: Modifier = Modifier,
    residueContent: @Composable () -> Unit = { ConveyReversalDefaultResidue() },
    content: @Composable () -> Unit,
) {
    val isPending = state.pending == item

    LaunchedEffect(isPending, item) {
        if (isPending) {
            delay(windowMillis)
            state.commit()
        }
    }

    ConveyStateHost(
        state = isPending,
        targetShape = if (isPending) residueShape else ConveyShape.None,
        targetHeight = if (isPending) residueHeight else null,
        grammar = grammar,
        modifier = modifier.clickable(enabled = isPending) { state.restore() },
    ) { pending ->
        if (pending) residueContent() else content()
    }
}

/** The default residue: a plain "Undo" label. Override via [ConveyReversal]'s `residueContent`. */
@Composable
private fun ConveyReversalDefaultResidue() {
    androidx.compose.material3.Text("Undo")
}
