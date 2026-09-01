package compose.conveyance.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import compose.conveyance.convey.generated.resources.Res
import compose.conveyance.convey.generated.resources.azrienoch_vf
import org.jetbrains.compose.resources.Font

/**
 * Azrienoch — this library's official typeface: a multiplex variable font (SIL OFL 1.1,
 * https://github.com/HereLiesAz/Azrienoch). One family, four live axes (`wght`/`wdth`/`SERF`/
 * `GRAD`) instead of a family per weight or style. See `docs/THIRD_PARTY_NOTICES.md` for the
 * font's license, which travels with it as `docs/Azrienoch-OFL.txt`.
 *
 * **Compose's `TextStyle` has no live `fontVariationSettings` field** (confirmed against the
 * actual `androidx.compose.ui.text.TextStyle` class this project's pinned Compose Multiplatform
 * version compiles against — no such constructor parameter exists) — variable-axis control in
 * Compose works by baking each desired axis point into its own [org.jetbrains.compose.resources.Font]
 * instance and picking a [FontFamily] built from that instance, not by varying one style
 * continuously the way this library's web port's CSS `font-variation-settings` can.
 * [conveyTypeFontFamily] is that: call it with a [ConveyTypeVariation] (a fixed point in the
 * axis space, e.g. one of [ConveyTypePreset]'s named ones), get back a [FontFamily] baked to
 * exactly that point, and set it as `TextStyle`'s `fontFamily`. A UI that wants a live slider
 * (see `dev-app`'s gallery) just calls this composable again on every recomposition with the
 * slider's current value — cheap, since [org.jetbrains.compose.resources.Font] and this
 * function both cache on their own inputs via `remember`.
 *
 * **Platform note, honestly stated rather than assumed:** custom (non-registered) variable
 * font axes — [ConveyTypeAxis.Serif] (`SERF`) and [ConveyTypeAxis.Grade] (`GRAD`) here — were
 * gated behind a real Compose Multiplatform limitation (JetBrains/compose-multiplatform#3127:
 * `FontVariation` arguments worked on Android but were silently dropped on iOS/Desktop) until
 * that issue was fixed upstream. `wght`/`wdth` (registered axes, exposed via
 * [FontVariation.weight]/[FontVariation.width]) have always worked everywhere Compose renders
 * text. This module compiles clean against this project's pinned Compose Multiplatform version
 * on `androidTarget`, `desktop`, and `wasmJs` (checked in this environment); `iosArm64`/
 * `iosSimulatorArm64` compile too (no macOS host here to run them), but **no target's actual
 * rendered output has been visually verified** — treat `wght`/`wdth` as safe everywhere
 * regardless, and `SERF`/`GRAD` as unverified beyond "it compiles" until checked against a real
 * render.
 */
object ConveyType {
    /** The font's registered name, for anywhere a bare family-name string is needed. */
    const val FontFamilyName: String = "Azrienoch"

    const val Source: String = "https://github.com/HereLiesAz/Azrienoch"
    const val License: String = "SIL Open Font License, Version 1.1"
}

/** One of Azrienoch's four variable axes, with its real published range and default. */
data class ConveyTypeAxis(val tag: String, val min: Float, val max: Float, val default: Float) {
    fun clamp(value: Float): Float = value.coerceIn(min, max)

    companion object {
        val Weight = ConveyTypeAxis("wght", 180f, 900f, 400f)
        val Width = ConveyTypeAxis("wdth", 75f, 100f, 100f)
        val Serif = ConveyTypeAxis("SERF", 0f, 100f, 0f)
        val Grade = ConveyTypeAxis("GRAD", -50f, 50f, 0f)
    }
}

/** A single fixed point in Azrienoch's four-axis space. Out-of-range values are clamped when [conveyTypeFontFamily] builds the actual font from this. */
data class ConveyTypeVariation(
    val weight: Float = ConveyTypeAxis.Weight.default,
    val width: Float = ConveyTypeAxis.Width.default,
    val serif: Float = ConveyTypeAxis.Serif.default,
    val grade: Float = ConveyTypeAxis.Grade.default,
)

/** A handful of named points in the axis space — starting points, not a constraint on using the axes directly via [ConveyTypeVariation]. */
object ConveyTypePreset {
    val Thin = ConveyTypeVariation(weight = 180f)
    val Regular = ConveyTypeVariation(weight = 400f)
    val Medium = ConveyTypeVariation(weight = 500f)
    val Bold = ConveyTypeVariation(weight = 700f)
    val Black = ConveyTypeVariation(weight = 900f)
    val Condensed = ConveyTypeVariation(width = 75f)
    val Slab = ConveyTypeVariation(serif = 100f)
}

/**
 * Loads Azrienoch baked to `variation`'s exact axis point, as a [FontFamily] ready for
 * `TextStyle(fontFamily = ...)`. `wght` is expressed via [FontVariation.weight] (an [Int], per
 * that API's own contract) and `wdth` via [FontVariation.width] ([Float]); `SERF`/`GRAD` have
 * no standard Compose accessor, so they go through [FontVariation.Setting]'s raw
 * four-character-tag form. See [ConveyType]'s own doc comment for why this bakes a point rather
 * than exposing a live continuous setting, and for the real per-platform caveat on `SERF`/`GRAD`.
 */
@Composable
fun conveyTypeFontFamily(variation: ConveyTypeVariation = ConveyTypePreset.Regular): FontFamily {
    val weight = ConveyTypeAxis.Weight.clamp(variation.weight)
    val width = ConveyTypeAxis.Width.clamp(variation.width)
    val serif = ConveyTypeAxis.Serif.clamp(variation.serif)
    val grade = ConveyTypeAxis.Grade.clamp(variation.grade)

    val font = Font(
        Res.font.azrienoch_vf,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.toInt()),
            FontVariation.width(width),
            FontVariation.Setting(ConveyTypeAxis.Serif.tag, serif),
            FontVariation.Setting(ConveyTypeAxis.Grade.tag, grade),
        ),
    )
    return remember(font) { FontFamily(font) }
}
