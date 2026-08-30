package compose.conveyance

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Continuous idle motion for chrome that should never look inert.
 *
 * [ConveyAffordance] teaches once, then stops — that is correct for a control the user
 * is about to touch. But some elements are not controls. A live counter, a status badge,
 * a presence indicator: these are not teaching interactivity, they are reporting that the
 * system is alive right now. [ConveyLife] is that second category, made structural.
 *
 * The Manifesto's physics-first stance applies here too: idle motion is not a canned
 * "pulse" you bolt onto anything. Each profile below has a distinct amplitude/period
 * signature so that two different [ConveyLife] profiles never read as the same thing
 * moving. If you need a new idle behavior, add a profile — do not tune an existing one
 * until it means something else.
 *
 * Unlike [ConveyAffordance], [ConveyLife] never stops on its own. Stop it explicitly
 * (e.g. by removing the modifier, or driving [enabled] to false) when the thing it
 * represents actually goes quiet — a badge that breathes forever after its subject has
 * gone offline is a lie the UI is telling.
 *
 * ```kotlin
 * Badge(
 *     modifier = Modifier.conveyLife(ConveyLife.Breathe(period = 2600L)),
 * ) { Text("LIVE") }
 * ```
 */
sealed interface ConveyLife {
    /** No idle motion. The element is chrome, not a living indicator. */
    data object None : ConveyLife

    /** Gentle scale/opacity breathing. Communicates: "present, unhurried." */
    data class Breathe(
        val period: Long = 2600L,
        val peakScale: Float = 1.12f,
        val minOpacity: Float = 0.82f,
    ) : ConveyLife

    /** Opacity + glow flicker, non-uniform. Communicates: "distant, still transmitting." */
    data class Twinkle(
        val period: Long = 2200L,
        val minOpacity: Float = 0.5f,
    ) : ConveyLife

    /** Skew/scaleY tension, never settling fully. Communicates: "under pressure, held." */
    data class Wobble(
        val period: Long = 4500L,
        val skewDegrees: Float = 4f,
    ) : ConveyLife

    /**
     * A one-shot amplified motion, played on demand via [Modifier.conveyLifeBurst] rather
     * than continuously — for the "delight" moment when a living element is deliberately
     * struck (tapped, achieved, completed). Composes on top of any idle [ConveyLife] profile.
     */
    data class Burst(
        val meaning: String = "delight",
    ) : ConveyLife
}

/**
 * Applies continuous idle motion described by [profile]. No-op for [ConveyLife.None].
 *
 * [phaseOffset] staggers multiple elements sharing one profile (e.g. each letter of a
 * word, each item in a live list) so they don't move in unison — synchronized idle motion
 * reads as one puppet, staggered idle motion reads as several living things.
 */
@Stable
fun Modifier.conveyLife(
    profile: ConveyLife,
    phaseOffset: Long = 0L,
    enabled: Boolean = true,
): Modifier = when {
    !enabled || profile is ConveyLife.None || profile is ConveyLife.Burst -> this
    profile is ConveyLife.Breathe -> this.composed {
        val t = rememberInfiniteLoop(profile.period, phaseOffset)
        val wave = sinWave(t)
        this
            .scale(1f + (profile.peakScale - 1f) * wave)
            .graphicsLayer { alpha = profile.minOpacity + (1f - profile.minOpacity) * wave }
    }
    profile is ConveyLife.Twinkle -> this.composed {
        val t = rememberInfiniteLoop(profile.period, phaseOffset)
        val wave = sinWave(t)
        this.graphicsLayer { alpha = profile.minOpacity + (1f - profile.minOpacity) * wave }
    }
    profile is ConveyLife.Wobble -> this.composed {
        val t = rememberInfiniteLoop(profile.period, phaseOffset)
        val wave = sinWave(t, phase = 0.0)
        this.graphicsLayer {
            rotationZ = profile.skewDegrees * wave
            scaleY = 1f + 0.05f * wave
        }
    }
    else -> this
}

/**
 * Plays [ConveyLife.Burst]'s declared grammar meaning once, driven by [trigger] changing.
 * Compose this after [conveyLife] so the burst reads as an amplification of the idle
 * motion, not a replacement for it.
 *
 * ```kotlin
 * var pulses by remember { mutableStateOf(0) }
 * Modifier
 *     .conveyLife(ConveyLife.Breathe())
 *     .conveyLifeBurst(trigger = pulses, peakScale = 1.4f)
 * ```
 */
@Stable
fun Modifier.conveyLifeBurst(
    trigger: Any,
    peakScale: Float = 1.35f,
    grammar: ConveyGrammar = ConveyGrammar.Default,
    meaning: String = "delight",
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == 0 || trigger == false) return@LaunchedEffect
        scale.animateTo(peakScale, animationSpec = tween(160, easing = FastOutSlowInEasing))
        scale.animateTo(1f, animationSpec = grammar[meaning])
    }
    this.scale(scale.value)
}

// ── Shared idle-loop primitive ────────────────────────────────────────────────

/** A [0, 2π) phase clock, looping every [periodMs], offset by [phaseOffsetMs]. */
@Composable
private fun rememberInfiniteLoop(periodMs: Long, phaseOffsetMs: Long): Double {
    val transition = rememberInfiniteTransition(label = "conveyLife")
    val raw by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs.toInt(), easing = LinearEasing)),
        label = "conveyLife.phase",
    )
    val offsetFraction = (phaseOffsetMs % periodMs).toFloat() / periodMs
    return ((raw + offsetFraction) % 1f) * 2.0 * kotlin.math.PI
}

private fun sinWave(t: Double, phase: Double = 0.0): Float =
    ((kotlin.math.sin(t + phase) + 1.0) / 2.0).toFloat()
