package compose.conveyance.foundation

import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

/**
 * The scroll-linked-animation infrastructure Part XII (§12.5, "The Body Block") of the
 * Conveyance Manifesto calls for — neither Compose nor this library has a native primitive for
 * continuously mapping scroll position to a transform (the closest analog CSS has,
 * `scroll-timeline`, has no Compose counterpart). This is genuinely new infrastructure, not a
 * wrapper over something that already existed.
 *
 * Which axis a [conveyScrollParallax] entrance travels along, per §12.5's role-directed rule
 * (a `Paragraph` enters horizontally, a `Quote` vertically).
 */
enum class ConveyParallaxDirection { Horizontal, Vertical }

/**
 * Pure, platform-independent math for §12.5's scroll-linked entrance. Kept free of any
 * Composable/UI dependency so it is directly unit-testable, the same discipline
 * [ConveyDesignSolver] follows.
 */
object ConveyScrollParallax {

    /** How much of the viewport's own height counts as the "entering" zone, measured up from its bottom edge. */
    const val DEFAULT_ENTRANCE_ZONE_FRACTION = 0.5f

    /**
     * 0 when [itemTopInViewport] sits at or below the bottom of the entrance zone (not yet
     * entered), 1 once it has crossed entirely into the settled zone above it, linear in
     * between. Linear, not eased — matches the rest of the framework's preference for a
     * legible, literal relationship between the input (scroll position) and the output
     * (progress) over a stylized curve. [itemTopInViewport] is the item's top edge measured
     * downward from the viewport's own top edge (negative once the item has scrolled above the
     * viewport entirely).
     */
    fun entranceProgress(
        itemTopInViewport: Float,
        viewportHeight: Float,
        entranceZoneFraction: Float = DEFAULT_ENTRANCE_ZONE_FRACTION,
    ): Float {
        if (viewportHeight <= 0f) return 1f
        val zoneHeight = viewportHeight * entranceZoneFraction
        val zoneBottom = viewportHeight
        val zoneTop = viewportHeight - zoneHeight
        return when {
            itemTopInViewport <= zoneTop -> 1f
            itemTopInViewport >= zoneBottom -> 0f
            else -> (zoneBottom - itemTopInViewport) / zoneHeight
        }
    }

    /** The entrance transform's magnitude at [progress] — [distance] at progress 0, 0 at progress 1. */
    fun translation(progress: Float, distance: Float): Float =
        distance * (1f - progress.coerceIn(0f, 1f))
}

/**
 * Applies §12.5's scroll-linked entrance: as this element's live position (tracked via
 * [onGloballyPositioned], which Compose re-invokes on every scroll-driven layout pass) crosses
 * into [viewportCoordinates]'s entrance zone, it translates in from [distancePx] along
 * [direction] and settles to identity. The position reads happen inside
 * [androidx.compose.ui.graphics.graphicsLayer]'s draw-phase lambda rather than the composable's
 * own body, so a scroll gesture re-layers the affected elements without recomposing them — the
 * standard Compose pattern for a smooth, per-frame scroll-linked visual effect.
 *
 * @param viewportCoordinates the scrolling container's own [LayoutCoordinates] (captured via its
 *   own `onGloballyPositioned`, outside the scrolled content) — not the item's own coordinates.
 */
fun Modifier.conveyScrollParallax(
    direction: ConveyParallaxDirection,
    viewportCoordinates: () -> LayoutCoordinates?,
    distancePx: Float,
    entranceZoneFraction: Float = ConveyScrollParallax.DEFAULT_ENTRANCE_ZONE_FRACTION,
): Modifier = composed {
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    this
        .onGloballyPositioned { itemCoordinates = it }
        .graphicsLayer {
            val item = itemCoordinates
            val viewport = viewportCoordinates()
            if (item == null || viewport == null || !item.isAttached || !viewport.isAttached) {
                return@graphicsLayer
            }
            val itemTopInViewport = item.positionInWindow().y - viewport.positionInWindow().y
            val progress = ConveyScrollParallax.entranceProgress(itemTopInViewport, viewport.size.height.toFloat(), entranceZoneFraction)
            val offset = ConveyScrollParallax.translation(progress, distancePx)
            alpha = progress.coerceIn(0f, 1f)
            when (direction) {
                ConveyParallaxDirection.Horizontal -> translationX = offset
                ConveyParallaxDirection.Vertical -> translationY = offset
            }
        }
}
