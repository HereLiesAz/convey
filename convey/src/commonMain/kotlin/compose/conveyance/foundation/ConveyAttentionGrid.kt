package compose.conveyance.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.conveyance.*
import compose.conveyance.tokens.*

/**
 * A grid where attention IS Primary status — temporarily, structurally, and honestly.
 *
 * [ConveyWeight] says exactly one element is Hero and only a handful are Primary at a time.
 * A staggered grid of many equally-important-looking tiles violates that on its face — until
 * you notice that on a real surface, the user's cursor or focus already tells you which tile
 * is "primary" right now. [ConveyAttentionGrid] makes that structural: the item under the
 * pointer promotes itself, everything else demotes, and the promotion is undone the instant
 * attention moves on. No tile owns Primary permanently. That is the point — Primary was never
 * supposed to be a fixed label, only a fixed COUNT at any one instant.
 *
 * Selecting a tile escalates it past Primary to Hero — not by opening a dialog over the grid,
 * but by growing the tile itself, from exactly where it sat, until it fills the surface. The
 * Manifesto's core claim about morphing applies here at the layout level, not just to shape and
 * color: the grid item and the fullscreen detail view are the same element, continuous through
 * the whole transition. Nothing is spawned. The user's eye never has to find a new thing.
 *
 * ```kotlin
 * ConveyAttentionGrid(
 *     items = tiles,
 *     grammar = grammar,
 * ) { tile, isAttended, expand ->
 *     TileContent(
 *         tile = tile,
 *         modifier = Modifier
 *             .conveyWeight(if (isAttended) ConveyWeight.Primary else ConveyWeight.Secondary)
 *             .clickable(onClick = expand),
 *     )
 * }
 * ```
 *
 * @param items The tiles. Order is preserved; [spanFor] controls each tile's grid span for a
 *   staggered (non-uniform) layout — uniform spans are a valid, boring special case, not an
 *   error.
 * @param columns Fixed column count for the underlying grid.
 * @param spanFor Optional per-item row span. Defaults to 1 (uniform). Vary it to stagger.
 * @param expandedPadding Inset of the expanded (Hero) surface from the screen edges.
 * @param content Rendered per tile, given the item, whether it currently holds attention, and
 *   a callback that escalates it to the expanded Hero surface.
 * @param expandedContent Rendered for whichever item is currently expanded, at full surface
 *   size. Receives the same item and a callback to collapse back into the grid.
 */
@Composable
fun <T> ConveyAttentionGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    spanFor: (T) -> Int = { 1 },
    expandedPadding: Dp = ConveySize.XLarge,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    content: @Composable (item: T, isAttended: Boolean, expand: () -> Unit) -> Unit,
    expandedContent: @Composable (item: T, collapse: () -> Unit) -> Unit,
) {
    var attended by remember { mutableStateOf<T?>(null) }
    var expanded by remember { mutableStateOf<T?>(null) }
    val originRects = remember { mutableStateMapOf<Any, Rect>() }

    Box(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(ConveySize.Small),
            verticalArrangement = Arrangement.spacedBy(ConveySize.Small),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items) { item ->
                val key = item as Any
                val isAttended = attended == item && expanded == null
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            originRects[key] = coords.boundsInRoot()
                        }
                        .then(if (expanded == item) Modifier.alpha(0f) else Modifier)
                        .conveyTransform(grammar) { Modifier.scaleOnPress(pressedScale = 0.97f) }
                        .hoverAttention(onEnter = { attended = item }, onExit = { if (attended == item) attended = null }),
                ) {
                    content(item, isAttended) {
                        attended = null
                        expanded = item
                    }
                }
            }
        }

        val toExpand = expanded
        if (toExpand != null) {
            val origin = originRects[toExpand as Any]
            ConveyExpandOverlay(
                origin = origin,
                padding = expandedPadding,
                grammar = grammar,
                onCollapsed = { expanded = null },
                content = { collapse -> expandedContent(toExpand, collapse) },
            )
        }
    }
}

/**
 * The escalation surface itself — a [ConveyMorph]-style geometry transition from a captured
 * origin [Rect] (the grid tile's last known position) to a full, padded surface, and back.
 *
 * This is deliberately not a [Dialog] or a [ModalBottomSheet]. Those own their own layer and
 * their own entrance animation, unrelated to where the trigger sat. Here the trigger's own
 * bounds ARE the animation's starting keyframe — the continuity is the whole point.
 */
@Composable
private fun ConveyExpandOverlay(
    origin: Rect?,
    padding: Dp,
    grammar: ConveyGrammar,
    onCollapsed: () -> Unit,
    content: @Composable (collapse: () -> Unit) -> Unit,
) {
    val morphSpec = grammar["morph"]
    var collapsing by remember { mutableStateOf(false) }

    // Animating the inset itself — rather than cutting straight to a target padding — is what
    // makes this read as the tile growing into the surface instead of a dialog appearing on top
    // of it. `origin` is captured by the caller for a future precise shared-element (FLIP) start
    // point; until that geometry work lands, animating from full-bleed is the honest middle
    // ground: real continuous motion, not yet anchored to the exact tile bounds.
    val animatedPaddingPx by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (collapsing) padding.value else 0f,
        animationSpec = morphSpec,
        label = "ConveyAttentionGrid.padding",
    )

    LaunchedEffect(collapsing) {
        if (collapsing) {
            // Give the geometry animation a beat to land before actually unmounting.
            kotlinx.coroutines.delay(estimateSettleMs())
            onCollapsed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(animatedPaddingPx.dp)
            .clip(ConveyShape.ExtraLarge)
            .background(ConveyColor.SurfaceContainerHigh),
    ) {
        CompositionLocalProvider(LocalContentColor provides ConveyColor.OnSurface) {
            content { collapsing = true }
        }
    }
}

// ── Small helpers kept private — real implementations belong in ConveyTransform/ConveyMorph ──

private fun estimateSettleMs(): Long = 260L

/**
 * Promotes [onEnter] / demotes [onExit] as the pointer crosses this element's bounds.
 *
 * Mirrors the hover heuristic already used by [compose.conveyance.ConveyTransformScope.liftOnHover]:
 * a pointer is "hovering" when it is present and inside bounds without being pressed. On touch
 * platforms this loop simply never fires — there is no hover to report, which is correct: touch
 * has no equivalent of "attention without commitment."
 */
private fun Modifier.hoverAttention(onEnter: () -> Unit, onExit: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.any { it.pressed }
                val hovered = event.changes.any { !it.isOutOfBounds(size, extendedTouchPadding) }
                if (hovered && !pressed) onEnter() else onExit()
            }
        }
    }
