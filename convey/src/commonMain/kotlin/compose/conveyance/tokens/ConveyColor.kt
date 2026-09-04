package compose.conveyance.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

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
 *
 * The values below are the real Material Design 3 baseline dark color scheme (the published
 * tonal palette generated from seed color `#6750A4`), not invented — this is deliberate parity
 * with [conveyance-expressive](https://github.com/HereLiesAz/conveyance-expressive)'s own
 * `ExpressiveRole` container colors, one of the Conveyance ecosystem's actual style systems,
 * rather than an arbitrary from-scratch hue choice. `SurfaceContainer*` are not independently
 * published constants; M3's own dark-theme spec derives them by overlaying [Primary] onto
 * [Surface] at the same per-elevation opacities Material's elevation-overlay algorithm uses
 * (1dp=5%, 2dp=8%, 3dp=11%, 4dp=12%), computed here via [lerp] rather than guessed as literals.
 */
object ConveyColor {

    // ── Reference palette (M3 baseline dark scheme, seed #6750A4) ─────────────
    // These are reference values. Your product will override them.
    // The names matter more than the hex codes.

    val Primary = Color(0xFFD0BCFF)
    val OnPrimary = Color(0xFF381E72)
    val PrimaryContainer = Color(0xFF4F378B)
    val OnPrimaryContainer = Color(0xFFEADDFF)
    val PrimaryFixed = Color(0xFFEADDFF)
    val PrimaryFixedDim = Color(0xFFD0BCFF)

    val Secondary = Color(0xFFCCC2DC)
    val OnSecondary = Color(0xFF332D41)
    val SecondaryContainer = Color(0xFF4A4458)
    val OnSecondaryContainer = Color(0xFFE8DEF8)

    val Tertiary = Color(0xFFEFB8C8)
    val OnTertiary = Color(0xFF492532)
    val TertiaryContainer = Color(0xFF633B48)
    val OnTertiaryContainer = Color(0xFFFFD8E4)

    val Error = Color(0xFFF2B8B5)
    val OnError = Color(0xFF601410)
    val ErrorContainer = Color(0xFF8C1D18)
    val OnErrorContainer = Color(0xFFF9DEDC)

    // M3's baseline scheme has no separate Warning/Success roles (those are product-specific
    // extensions); kept here as an amber/green pair mixed the same way a real M3 dynamic-color
    // extension would derive them -- rotated off the Tertiary/Secondary tones rather than an
    // unrelated hue pulled from nowhere.
    val Warning = Color(0xFFFFCA85)
    val OnWarning = Color(0xFF4A2F00)
    val WarningContainer = Color(0xFF6B4700)
    val OnWarningContainer = Color(0xFFFFDDB3)

    val Success = Color(0xFFA6D6A6)
    val OnSuccess = Color(0xFF0F3D14)
    val SuccessContainer = Color(0xFF255128)
    val OnSuccessContainer = Color(0xFFC2F0C2)

    val Surface = Color(0xFF1C1B1F)
    val OnSurface = Color(0xFFE6E1E5)
    val SurfaceVariant = Color(0xFF49454F)
    val OnSurfaceVariant = Color(0xFFCAC4D0)
    val SurfaceContainerLow = elevatedSurface(0.05f)
    val SurfaceContainer = elevatedSurface(0.08f)
    val SurfaceContainerHigh = elevatedSurface(0.11f)
    val SurfaceContainerHighest = elevatedSurface(0.12f)

    val Outline = Color(0xFF938F99)
    val OutlineVariant = Color(0xFF49454F)

    val InverseSurface = Color(0xFFE6E1E5)
    val InverseOnSurface = Color(0xFF313033)
    val InversePrimary = Color(0xFF6750A4)

    val Scrim = Color(0xFF000000)
    val Shadow = Color(0xFF000000)

    /** [Primary] alpha-blended over [Surface] at [overlayAlpha] -- M3's own dark-theme elevation-overlay technique. */
    private fun elevatedSurface(overlayAlpha: Float): Color = lerp(Surface, Primary, overlayAlpha)

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
