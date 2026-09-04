package compose.conveyance.tokens

import androidx.compose.foundation.shape.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape vocabulary for the Conveyance system: named tokens for a fixed set of corner
 * treatments, so a product's own shape choices are expressed as tokens rather than restated
 * numeric radii at every call site.
 *
 * The prescription for which shape fits which kind of element lives in the `convey`
 * composable library itself, in each concrete component's own default -- not as free-floating
 * documentation here, disconnected from what actually ships.
 * [compose.conveyance.foundation.ConveyFab] defaults its collapsed/expanded shapes to
 * [Circle]/[ExtraLarge]; [compose.conveyance.foundation.ConveyCard] defaults to [Medium];
 * [compose.conveyance.foundation.ConveyChip], [compose.conveyance.foundation.ConveyAvatar],
 * [compose.conveyance.foundation.ConveyBadge],
 * [compose.conveyance.foundation.ConveySegmentedControl],
 * [compose.conveyance.foundation.ConveyNavigationBar] and
 * [compose.conveyance.foundation.ConveySwitch] all clip their own circular affordances (a
 * chip's remove target, an avatar, a status dot, a switch thumb, ...) to [Circle] directly in
 * their own implementation. Use those composables, not a hand-picked token off this object,
 * for anything they already cover -- this vocabulary exists for the composables to draw from,
 * and for the cases they don't cover yet.
 */
object ConveyShape {

    /** 50% radius -- a full circle/pill on a square bounding box. [compose.conveyance.foundation.ConveyFab]'s collapsed shape and every circular affordance in `convey`'s own composables default to this. */
    val Circle: Shape = RoundedCornerShape(percent = 50)

    /** ~35% radius -- a superellipse approximation. */
    val Squircle: Shape = RoundedCornerShape(percent = 35)

    /** 28dp radius. [compose.conveyance.foundation.ConveyFab]'s expanded shape. */
    val ExtraLarge: Shape = RoundedCornerShape(28.dp)

    /** 16dp radius. */
    val Large: Shape = RoundedCornerShape(16.dp)

    /** 12dp radius. [compose.conveyance.foundation.ConveyCard]'s default shape. */
    val Medium: Shape = RoundedCornerShape(12.dp)

    /** 8dp radius. */
    val Small: Shape = RoundedCornerShape(8.dp)

    /** 4dp radius. */
    val XSmall: Shape = RoundedCornerShape(4.dp)

    /** 0dp radius -- a plain rectangle. */
    val None: Shape = RoundedCornerShape(0.dp)

    /** Cut corners, 45-degree chamfer, 12dp. */
    val Cut: Shape = CutCornerShape(12.dp)

    /** Cut corners, 45-degree chamfer, 6dp -- a smaller chamfer than [Cut]. */
    val CutSmall: Shape = CutCornerShape(6.dp)

    /** Rounded on the top two corners only, 16dp. */
    val TopLarge: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    /** Rounded on the top two corners only, 28dp. */
    val TopExtraLarge: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Rounded on the bottom two corners only, 16dp. */
    val BottomLarge: Shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

    // ── Shape scale for programmatic use ─────────────────────────────────────

    /** [None] through [Circle], in ascending radius order -- for interpolating between shape tokens programmatically. */
    val scale: List<Shape> = listOf(None, XSmall, Small, Medium, Large, ExtraLarge, Squircle, Circle)

    /** The next shape up from [shape] in [scale]. Returns [Circle] if [shape] is already [Circle] or isn't in [scale]. */
    fun escalate(shape: Shape): Shape {
        val idx = scale.indexOf(shape)
        return if (idx < 0 || idx >= scale.lastIndex) scale.last() else scale[idx + 1]
    }

    /** The next shape down from [shape] in [scale]. Returns [None] if [shape] is already [None] or isn't in [scale]. */
    fun deescalate(shape: Shape): Shape {
        val idx = scale.indexOf(shape)
        return if (idx <= 0) scale.first() else scale[idx - 1]
    }
}
