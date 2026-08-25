package compose.conveyance.tokens

import androidx.compose.ui.graphics.Color

/**
 * Semantic color system for the Conveyance design system.
 *
 * The Manifesto says: "Dynamic color implicitly prioritizes. Primary/secondary/tertiary
 * contrast implicitly prioritizes actions without literal arrows."
 *
 * This is the implementation. Every color has a ROLE, not a value.
 * You do not pick colors because they are beautiful. You pick them because they
 * communicate a position in the hierarchy.
 *
 * The three-level role system:
 *
 * PRIMARY — The most important interactive role on the surface.
 *   Elements in Primary demand attention. There should be few of them.
 *   The eye should immediately identify Primary elements as "what to do next."
 *
 * SECONDARY — Supporting interactive role.
 *   Elements in Secondary are available but not insistent. "You could also do this."
 *
 * TERTIARY — Accent and emphasis.
 *   Tertiary is not a third primary. It is the emotional color — used for hero moments,
 *   delight states, key achievements. It should appear rarely. When it appears,
 *   it should feel special because it has been rare.
 *
 * Container colors (PrimaryContainer, etc.) are for lower-emphasis surfaces
 * that are associated with the role — not the action itself, but its context.
 *
 * This class is intentionally not a CompositionLocal default.
 * Compose's MaterialTheme handles color theming. [ConveyColor] is a REFERENCE PALETTE —
 * a semantic vocabulary you implement in your product's actual color scheme.
 * Match your brand colors to these roles, not to arbitrary hex values.
 */
object ConveyColor {

    // ── Reference palette (dark-mode optimized) ───────────────────────────────
    // These are reference values. Your product will override them.
    // The names matter more than the hex codes.

    val Primary = Color(0xFF7B56F8)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF1B1050)
    val OnPrimaryContainer = Color(0xFFC4AAFF)
    val PrimaryFixed = Color(0xFFEADDFF)
    val PrimaryFixedDim = Color(0xFFD0BCFF)

    val Secondary = Color(0xFF00CBA9)
    val OnSecondary = Color(0xFF002820)
    val SecondaryContainer = Color(0xFF003D33)
    val OnSecondaryContainer = Color(0xFF6DF5D4)

    val Tertiary = Color(0xFFFF8B5E)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFF4A1800)
    val OnTertiaryContainer = Color(0xFFFFB89A)

    val Error = Color(0xFFFF4D6A)
    val OnError = Color(0xFF690025)
    val ErrorContainer = Color(0xFF3B0013)
    val OnErrorContainer = Color(0xFFFFB3C1)

    val Warning = Color(0xFFFFAD42)
    val OnWarning = Color(0xFF3D2400)
    val WarningContainer = Color(0xFF4A3100)
    val OnWarningContainer = Color(0xFFFFD9A0)

    val Success = Color(0xFF34E89E)
    val OnSuccess = Color(0xFF003923)
    val SuccessContainer = Color(0xFF005237)
    val OnSuccessContainer = Color(0xFF86FAC4)

    val Surface = Color(0xFF04040C)
    val OnSurface = Color(0xFFECEDF5)
    val SurfaceVariant = Color(0xFF080818)
    val OnSurfaceVariant = Color(0xFF9899BC)
    val SurfaceContainer = Color(0xFF0D0D22)
    val SurfaceContainerLow = Color(0xFF08081A)
    val SurfaceContainerHigh = Color(0xFF131330)
    val SurfaceContainerHighest = Color(0xFF1A1A40)

    val Outline = Color(0xFF3A3A5C)
    val OutlineVariant = Color(0xFF1F1F3A)

    val InverseSurface = Color(0xFFE6E0F8)
    val InverseOnSurface = Color(0xFF04040C)
    val InversePrimary = Color(0xFF5433B8)

    val Scrim = Color(0xFF000000)
    val Shadow = Color(0xFF000000)

    // ── Semantic role helpers ──────────────────────────────────────────────────

    /**
     * The container color appropriate for an element of the given [ConveyWeight].
     *
     * Hero and Primary elements use [Primary] and [PrimaryContainer].
     * Secondary elements use [SecondaryContainer].
     * Ghost elements use [SurfaceContainer].
     *
     * This is a starting point. Your product will override this mapping.
     */
    fun containerFor(weight: compose.conveyance.ConveyWeight): Color = when (weight) {
        compose.conveyance.ConveyWeight.Hero -> Primary
        compose.conveyance.ConveyWeight.Primary -> PrimaryContainer
        compose.conveyance.ConveyWeight.Secondary -> SecondaryContainer
        compose.conveyance.ConveyWeight.Ghost -> SurfaceContainer
    }

    fun contentFor(weight: compose.conveyance.ConveyWeight): Color = when (weight) {
        compose.conveyance.ConveyWeight.Hero -> OnPrimary
        compose.conveyance.ConveyWeight.Primary -> OnPrimaryContainer
        compose.conveyance.ConveyWeight.Secondary -> OnSecondaryContainer
        compose.conveyance.ConveyWeight.Ghost -> OnSurfaceVariant
    }
}
