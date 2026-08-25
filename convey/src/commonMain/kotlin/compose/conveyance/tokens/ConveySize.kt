package compose.conveyance.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Proportional spacing and sizing scale.
 *
 * This is a geometric progression, not an arbitrary list.
 * Each step is perceptibly different from its neighbors.
 * Values that are "almost the same" are not different enough to mean anything.
 * Values that are "very different" communicate different structural levels.
 *
 * Use these tokens to answer: "How far apart should these things be?"
 * The answer is not a pixel value. The answer is a relationship:
 * "These are in the same group" (Small) or "These are in different groups" (Large).
 */
object ConveySize {

    // ── Spacing scale ─────────────────────────────────────────────────────────

    val None: Dp = 0.dp

    /** Hairline gap. Same visual group. */
    val Hairline: Dp = 1.dp

    /** Tight. Elements that belong together. */
    val XSmall: Dp = 4.dp

    /** Close. Related elements. */
    val Small: Dp = 8.dp

    /** Standard internal padding. Most content within cards. */
    val Medium: Dp = 16.dp

    /** Comfortable. Related sections. */
    val Large: Dp = 24.dp

    /** Generous. Different but adjacent sections. */
    val XLarge: Dp = 32.dp

    /** Spacious. Major divisions within a screen. */
    val XXLarge: Dp = 48.dp

    /** Expansive. Between structural page sections. */
    val Huge: Dp = 64.dp

    /** Maximum. For hero sections and major visual breaks. */
    val Hero: Dp = 96.dp

    // ── Component size tokens ─────────────────────────────────────────────────

    object Component {
        // Icon sizes
        val IconSmall: Dp = 16.dp
        val IconMedium: Dp = 24.dp
        val IconLarge: Dp = 32.dp
        val IconXLarge: Dp = 40.dp

        // Touch targets (accessibility floor: 48.dp)
        val MinTouchTarget: Dp = 48.dp

        // Button heights
        val ButtonSmall: Dp = 32.dp
        val ButtonMedium: Dp = 40.dp
        val ButtonLarge: Dp = 48.dp

        // FAB sizes
        val FabSmall: Dp = 40.dp
        val Fab: Dp = 56.dp
        val FabLarge: Dp = 96.dp

        // Navigation
        val NavigationBar: Dp = 80.dp
        val TopAppBar: Dp = 64.dp
        val TopAppBarLarge: Dp = 152.dp

        // List items
        val ListItemSmall: Dp = 48.dp
        val ListItem: Dp = 56.dp
        val ListItemLarge: Dp = 72.dp
        val ListItemXLarge: Dp = 88.dp

        // Cards
        val CardMinHeight: Dp = 64.dp

        // Input fields
        val InputHeight: Dp = 56.dp

        // Bottom sheet
        val BottomSheetPeek: Dp = 88.dp
    }

    // ── Elevation scale ───────────────────────────────────────────────────────

    object Elevation {
        /** No elevation. Flat, part of the surface. */
        val None: Dp = 0.dp

        /** Barely lifted. Cards at rest. */
        val XSmall: Dp = 1.dp

        /** Slightly lifted. Hover state. */
        val Small: Dp = 3.dp

        /** Clearly lifted. Raised cards. */
        val Medium: Dp = 6.dp

        /** Floating. Navigation bars, app bars. */
        val Large: Dp = 8.dp

        /** High. FABs, modal surfaces. */
        val XLarge: Dp = 12.dp

        /** Maximum. Menus, dialogs, tooltips. */
        val XXLarge: Dp = 16.dp
    }

    // ── Border widths ─────────────────────────────────────────────────────────

    object Stroke {
        val Hairline: Dp = 0.5.dp
        val Thin: Dp = 1.dp
        val Regular: Dp = 1.5.dp
        val Thick: Dp = 2.dp
        val Heavy: Dp = 3.dp
    }
}
