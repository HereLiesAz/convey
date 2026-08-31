package compose.conveyance.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar

/**
 * Law 2 — Continuity: "Nothing appears from nowhere and nothing goes nowhere."
 *
 * If tapping a row opens a detail view, the row *becomes* the detail view. Cross-fades and
 * teleports destroy the mental map a person builds of a system they have never seen; a
 * destination cannot be declared without naming the element it grows out of.
 *
 * [ConveyEnter] is that declaration for navigation between composables/destinations, the way
 * [ConveyMorphControl] is it for one control becoming another and [ConveyMigration] is it for
 * an empty state's control becoming its permanent corner position. The element marked with
 * [Modifier.conveyOrigin] records where it was on screen; the [ConveyEnter] that follows it
 * grows from that recorded position to fill its own space, instead of appearing from nowhere.
 *
 * ```kotlin
 * // In the list:
 * MessageRow(
 *     message,
 *     modifier = Modifier
 *         .conveyOrigin(message.id)
 *         .clickable { selected = message },
 * )
 *
 * // In the destination:
 * selected?.let { message ->
 *     ConveyEnter(key = message.id) {
 *         MessageDetail(message)
 *     }
 * }
 * ```
 *
 * This is a scale/translate approximation of a shared-element transition — the destination's
 * whole box grows from the origin element's last recorded bounds to its own, rather than a true
 * per-element content morph. It has not been visually verified against a real display in the
 * environment this was built in; try it on a real target before trusting the feel of it.
 *
 * @param key Identifies which origin to grow from. Must match the key passed to
 *   [Modifier.conveyOrigin] on the element that led here.
 * @param registry Where origins are recorded. Defaults to the ambient one; only override this
 *   if [Modifier.conveyOrigin] elsewhere is also using a non-default registry.
 * @param grammar Motion vocabulary. Uses the "navigate" meaning — this literally is one.
 * @param content The destination's content. Composed once and grown into; it does not receive
 *   the animation progress, since [ConveyEnter] applies it to the whole box, not per-element.
 */
@Composable
fun ConveyEnter(
    key: Any,
    registry: ConveyOriginRegistry = LocalConveyOriginRegistry.current,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val navigateSpec = grammar["navigate"]
    val progress = remember { Animatable(0f) }
    var ownBounds by remember { mutableStateOf<Rect?>(null) }
    val originBounds = remember(key) { registry.boundsFor(key) }

    LaunchedEffect(ownBounds) {
        if (ownBounds != null && originBounds != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, navigateSpec)
        } else {
            // No recorded origin (e.g. a deep link straight to this destination) -- nothing to
            // grow from, so there is nothing dishonest about just appearing.
            progress.snapTo(1f)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                if (ownBounds == null) ownBounds = coordinates.boundsInRoot()
            }
            .graphicsLayer {
                val own = ownBounds
                val origin = originBounds
                if (own != null && origin != null && own.width > 0f && own.height > 0f) {
                    val p = progress.value
                    scaleX = lerp(origin.width / own.width, 1f, p)
                    scaleY = lerp(origin.height / own.height, 1f, p)
                    translationX = lerp(origin.center.x - own.center.x, 0f, p)
                    translationY = lerp(origin.center.y - own.center.y, 0f, p)
                }
            },
    ) {
        content()
    }
}

/**
 * Marks this element as the place [ConveyEnter] with a matching [key] grows out of. Records
 * this element's root-relative bounds on every layout pass, so a later [ConveyEnter] can start
 * its transition from wherever this element currently is.
 */
fun Modifier.conveyOrigin(
    key: Any,
    registry: ConveyOriginRegistry? = null,
): Modifier = this.composed {
    val resolvedRegistry = registry ?: LocalConveyOriginRegistry.current
    this.onGloballyPositioned { coordinates ->
        resolvedRegistry.register(key, coordinates.boundsInRoot())
    }
}

/** Tracks the last recorded on-screen bounds of every [Modifier.conveyOrigin]-marked element. */
@Stable
class ConveyOriginRegistry {
    private val bounds = mutableMapOf<Any, Rect>()

    internal fun register(key: Any, rect: Rect) {
        bounds[key] = rect
    }

    /** The last recorded bounds for [key], or null if nothing marked with it has laid out yet. */
    fun boundsFor(key: Any): Rect? = bounds[key]
}

val LocalConveyOriginRegistry = staticCompositionLocalOf { ConveyOriginRegistry() }

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
