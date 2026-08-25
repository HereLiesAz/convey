package compose.conveyance.tokens

import androidx.compose.foundation.shape.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape vocabulary for the Conveyance system.
 *
 * Shape is not decoration. Shape is a signal.
 *
 * The Manifesto says: "Use varied corner radii and forms to direct focus without literal arrows."
 * This is the implementation of that principle as tokens.
 *
 * Hierarchy of shape signals (most to least interactive-seeming):
 *   Circle (50%) > Squircle (35%) > ExtraLarge (28dp) > Large (16dp) > Medium (12dp)
 *   > Small (8dp) > None (0dp) > Cut (mechanical)
 *
 * A circle invites touch more than a rounded rectangle.
 * A rounded rectangle invites touch more than a sharp rectangle.
 * A cut corner signals precision and structure over warmth.
 *
 * Use these as semantic tokens, not as numeric radius preferences.
 * "I want 14dp radius" is not a design decision. "This element should feel approachable
 * but not primary" — that's a design decision. The answer might be Medium.
 *
 * Every token carries a rationale. Know the rationale before choosing the token.
 */
object ConveyShape {

    /**
     * 50% radius.
     * Maximum interactivity signal. "Touch me."
     * Use for: FABs, icon buttons, avatar indicators, the single most touchable element.
     * Do not use for anything that is not strongly interactive.
     */
    val Circle: Shape = RoundedCornerShape(percent = 50)

    /**
     * ~35% radius. Superellipse approximation.
     * Friendly and approachable without commanding primary attention.
     * Use for: chips, tags, status badges, pill-shaped secondary actions.
     */
    val Squircle: Shape = RoundedCornerShape(percent = 35)

    /**
     * 28dp radius.
     * Prominent cards and large interactive surfaces.
     * Use for: bottom sheets, dialogs, hero cards, feature tiles.
     */
    val ExtraLarge: Shape = RoundedCornerShape(28.dp)

    /**
     * 16dp radius.
     * Standard cards and most containers.
     * Use for: list cards, input containers, navigation items.
     */
    val Large: Shape = RoundedCornerShape(16.dp)

    /**
     * 12dp radius.
     * Medium interactive elements.
     * Use for: chips, filters, segmented buttons, small cards.
     */
    val Medium: Shape = RoundedCornerShape(12.dp)

    /**
     * 8dp radius.
     * Small elements and compact surfaces.
     * Use for: badges, small chips, snackbars.
     */
    val Small: Shape = RoundedCornerShape(8.dp)

    /**
     * 4dp radius.
     * Near-sharp elements that still feel slightly finished.
     * Use for: dense data tables, compact list items, code blocks.
     */
    val XSmall: Shape = RoundedCornerShape(4.dp)

    /**
     * 0dp radius.
     * Full-bleed and structural.
     * Use for: edge-to-edge surfaces, structural dividers, backgrounds.
     * Not for interactive elements — sharp corners signal "this is not for touching."
     */
    val None: Shape = RoundedCornerShape(0.dp)

    /**
     * Cut corners (45° chamfer).
     * Mechanical, precise, systematic. Not warm.
     * Use for: settings panels, developer tools, system UI, anything that signals
     * "this is infrastructure, not content." If your product is warm and human,
     * you probably do not need this.
     */
    val Cut: Shape = CutCornerShape(12.dp)

    /**
     * Cut corners, small chamfer.
     * Subtle mechanical signal.
     */
    val CutSmall: Shape = CutCornerShape(6.dp)

    /**
     * Top-rounded only. For elements attached to a bottom edge.
     * Use for: bottom sheets before they fully expand, sticky bottom panels.
     */
    val TopLarge: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val TopExtraLarge: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /**
     * Bottom-rounded only. For elements attached to a top edge.
     */
    val BottomLarge: Shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

    // ── Shape scale for programmatic use ─────────────────────────────────────

    /**
     * All shapes in ascending radius order.
     * Use for interpolating between shape tokens programmatically.
     */
    val scale: List<Shape> = listOf(None, XSmall, Small, Medium, Large, ExtraLarge, Squircle, Circle)

    /**
     * The shape that sits one level above [shape] in the hierarchy.
     * Returns [Circle] if [shape] is already [Circle] or not in the scale.
     * Used for expanding elements — a card expanding to full-screen should
     * progress from Large toward ExtraLarge, not toward Circle.
     */
    fun escalate(shape: Shape): Shape {
        val idx = scale.indexOf(shape)
        return if (idx < 0 || idx >= scale.lastIndex) scale.last() else scale[idx + 1]
    }

    fun deescalate(shape: Shape): Shape {
        val idx = scale.indexOf(shape)
        return if (idx <= 0) scale.first() else scale[idx - 1]
    }
}
