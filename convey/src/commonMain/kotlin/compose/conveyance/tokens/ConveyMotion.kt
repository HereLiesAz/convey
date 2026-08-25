package compose.conveyance.tokens

import androidx.compose.animation.core.*

/**
 * Physics-first motion system.
 *
 * Every spring here has a name that describes what it communicates to the user,
 * not how it behaves mathematically. "Snappy" because the user feels their touch
 * acknowledged immediately. "Deliberate" because slow motion signals weight and
 * consequence. "Elastic" because the overshoot communicates playfulness and life.
 *
 * The parameters are deliberate. Do not adjust them for aesthetic reasons.
 * If you need different motion, add a new named spec to your [ConveyGrammar].
 * If you adjust these, you are changing what the library communicates — you are
 * editing the meaning, not the styling.
 *
 * Duration tokens follow Material 3's scale, because that scale was researched.
 * We do not invent duration scales. We use the one that works.
 */
object ConveyMotion {

    // ── Springs — named for what they communicate ─────────────────────────────

    /**
     * Immediate acknowledgment. No perceptible latency between touch and response.
     * Use for: pressed states, toggle switches, immediate feedback.
     * Communicates: "I heard you."
     */
    val Snappy: SpringSpec<Float> = spring(
        stiffness = 620f,
        dampingRatio = 0.74f,
    )

    /**
     * Purposeful but unhurried. Confident motion that doesn't rush.
     * Use for: navigation, layout shifts, most state transitions.
     * Communicates: "Something changed, and this is where you are now."
     */
    val Standard: SpringSpec<Float> = spring(
        stiffness = 380f,
        dampingRatio = 0.82f,
    )

    /**
     * Slow and weighted. Implies the element has mass or consequence.
     * Use for: revealing important content, dialogs, heavy data.
     * Communicates: "Pay attention. This matters."
     */
    val Deliberate: SpringSpec<Float> = spring(
        stiffness = 180f,
        dampingRatio = 0.88f,
    )

    /**
     * Moderate overshoot. Suggests life, personality, delight.
     * Use SPARINGLY. Reserve for hero moments and first-run experiences.
     * Communicates: "This system is alive and glad you're here."
     * Warning: overuse destroys the effect. One elastic spring per screen, maximum.
     */
    val Elastic: SpringSpec<Float> = spring(
        stiffness = 280f,
        dampingRatio = 0.38f,
    )

    /**
     * Strong overshoot. Maximum expressiveness.
     * Use ONCE per significant user achievement. Not per screen. Per achievement.
     * Communicates: "You did something important. The system is celebrating with you."
     */
    val Heroic: SpringSpec<Float> = spring(
        stiffness = 220f,
        dampingRatio = 0.26f,
    )

    // ── Tweens — for cases where duration matters more than physics ───────────

    /**
     * Fast linear exit. Content leaving doesn't need spring — it just goes.
     * Springs on exit slow down the thing the user has already moved past.
     */
    val Exit: TweenSpec<Float> = tween(
        durationMillis = 180,
        easing = FastOutLinearInEasing,
    )

    /**
     * Deliberate entrance for content that wants to be read.
     * Decelerates into position — fast start, slow settle.
     */
    val Enter: TweenSpec<Float> = tween(
        durationMillis = 300,
        easing = LinearOutSlowInEasing,
    )

    /**
     * Error states do not animate. They interrupt.
     * An animated error message says "this error is part of the flow."
     * A snap says "stop. something is wrong."
     */
    val Interrupt: SnapSpec<Float> = snap()

    // ── Duration scale (milliseconds) ─────────────────────────────────────────

    object Duration {
        const val Short1 = 50
        const val Short2 = 100
        const val Short3 = 150
        const val Short4 = 200

        const val Medium1 = 250
        const val Medium2 = 300
        const val Medium3 = 350
        const val Medium4 = 400

        const val Long1 = 450
        const val Long2 = 500
        const val Long3 = 550
        const val Long4 = 600

        const val ExtraLong1 = 700
        const val ExtraLong2 = 800
        const val ExtraLong3 = 900
        const val ExtraLong4 = 1000
    }

    // ── Easing curves ─────────────────────────────────────────────────────────

    object Easing {
        /** Standard curve. Appropriate for most transitions. */
        val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

        /** Emphasized. For elements that should draw attention. */
        val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        /** Decelerate into final position. For incoming elements. */
        val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

        /** Accelerate out of view. For departing elements. */
        val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }
}

// ── Convenience extensions on AnimationSpec ───────────────────────────────────

fun <T> SpringSpec<T>.withDampingRatio(dampingRatio: Float): SpringSpec<T> =
    spring(stiffness = stiffness, dampingRatio = dampingRatio)

fun <T> SpringSpec<T>.withStiffness(stiffness: Float): SpringSpec<T> =
    spring(stiffness = stiffness, dampingRatio = dampingRatio)

/** True if this spring will overshoot its target before settling. */
val SpringSpec<*>.isElastic: Boolean
    get() = dampingRatio < 1f

/** True if this spring is critically damped (no overshoot, fastest possible settling). */
val SpringSpec<*>.isCriticallyDamped: Boolean
    get() = dampingRatio >= 1f

/** Estimated time to settle within [tolerance] of the target, in milliseconds. */
fun SpringSpec<Float>.estimatedDurationMs(tolerance: Float = 0.01f): Int {
    if (dampingRatio >= 1f) return (3000f / stiffness).toInt()
    val omega = kotlin.math.sqrt(stiffness.toDouble())
    val decay = dampingRatio * omega
    return if (decay == 0.0) Int.MAX_VALUE
    else (-kotlin.math.ln(tolerance.toDouble()) / decay * 1000).toInt()
}
