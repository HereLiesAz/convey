package compose.conveyance.foundation

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A pure-Kotlin 2D force-dynamics simulator — no external physics engine dependency, per
 * `docs/Procedural Animation of Subject-Verb-Object Typography.md`'s "Implementation status"
 * (§"Rigid Body Physics and Force Dynamics"). This is the one thing in that section's blueprint
 * that is genuinely buildable as pure Kotlin: attraction/repulsion vectors between a subject and
 * an object, simple circle-circle collision response, a scalar spring-mass "soft body" wobble,
 * and a periodic gait approximation for animate locomotion. None of this is a general-purpose
 * physics engine — it is exactly the handful of primitives [compose.conveyance.ConveySvoScene]
 * needs and nothing more.
 */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)

    val length: Float get() = hypot(x, y)

    fun normalizedOrZero(): Vec2 {
        val len = length
        return if (len < 1e-4f) Zero else Vec2(x / len, y / len)
    }

    companion object {
        val Zero = Vec2(0f, 0f)
    }
}

/**
 * Interactive force dynamics between two on-screen bodies — the report's own vocabulary
 * (§"Rigid Body Physics and Force Dynamics"): "adjusting the strength of magnetic attraction from
 * the Subject to the Object based on the verb."
 */
object ConveyForceDynamics {

    /** A vector from [from] toward [to], scaled by [strength]. Positive [strength] attracts. */
    fun attraction(from: Vec2, to: Vec2, strength: Float): Vec2 =
        (to - from).normalizedOrZero() * strength

    /** The mirror of [attraction]: a vector from [to] pointing away from [from]. */
    fun repulsion(from: Vec2, to: Vec2, strength: Float): Vec2 =
        (to - from).normalizedOrZero() * -strength

    /** True once two circular bounds (by center + combined radius) actually overlap. */
    fun hasCollided(a: Vec2, b: Vec2, combinedRadius: Float): Boolean =
        (b - a).length <= combinedRadius
}

/**
 * A single moving body: position, velocity, and mass, integrated by semi-implicit (symplectic)
 * Euler — stable enough for the small time steps a Compose frame loop supplies, and simple enough
 * to reason about without a general solver.
 */
class ConveyRigidBody(
    initialPosition: Vec2,
    val mass: Float = 1f,
    val damping: Float = 0.9f,
) {
    var position: Vec2 = initialPosition
        private set
    var velocity: Vec2 = Vec2.Zero
        private set

    fun applyForce(force: Vec2, dtSeconds: Float) {
        velocity = (velocity + force * (dtSeconds / mass)) * damping
        position += velocity * dtSeconds
    }

    fun stop() {
        velocity = Vec2.Zero
    }

    fun snapTo(newPosition: Vec2) {
        position = newPosition
    }
}

/**
 * A one-dimensional damped harmonic oscillator standing in for the report's soft-body spring-mass
 * mesh (§"Soft Body Dynamics and Spring-Mass Deformation"). A true per-vertex spring network needs
 * the glyph mesh access diffvg-style vector morphing would provide, which this library doesn't
 * have (see the SVO doc's "Implementation status" — word-as-image morphing is out of scope). This
 * is the honestly-scoped-down version: one scalar displacement, driven by an [impulse] (impact
 * speed) and relaxed back to rest by [stiffness]/[dampingRatio] — read directly as a squash/stretch
 * scale multiplier by [compose.conveyance.ConveySvoScene].
 */
class ConveySpringMassBody(
    private val stiffness: Float = 220f,
    private val dampingRatio: Float = 0.35f,
) {
    var displacement: Float = 0f
        private set
    private var velocity: Float = 0f

    fun impulse(strength: Float) {
        velocity += strength
    }

    fun step(dtSeconds: Float) {
        val springForce = -stiffness * displacement
        val dampingForce = -2f * dampingRatio * kotlin.math.sqrt(stiffness) * velocity
        velocity += (springForce + dampingForce) * dtSeconds
        displacement += velocity * dtSeconds
    }
}

/**
 * The "much simpler periodic gait approximation" the SVO doc's "Implementation status" section
 * commits to instead of true multi-bone Inverse Kinematics (§"Skeletal Deformation and Inverse
 * Kinematics (IK)" is explicitly out of scope — full skeletal IK solving over arbitrary glyph
 * "limbs" is a large undertaking with no vector-glyph access to attach it to). This is a
 * bob/tilt keyed to translation speed: a vertical sine bob (a running gait's up-down bounce) and
 * a rotational tilt (weight shifting side to side), both scaling toward zero as [speedPxPerSec]
 * approaches zero — the word stands still when it isn't moving, rather than perpetually fidgeting.
 */
class ConveyGaitOscillator(
    private val strideHz: Float = 2.4f,
    private val referenceSpeedPxPerSec: Float = 260f,
) {
    private var phase: Float = 0f

    // A real running gait's cadence, not just its bounce height, quickens with pace -- keyed to
    // the same reference speed bobPx/tiltDegrees use for amplitude, so "intensity" means the same
    // thing in both. Floored at 0.4x so a just-started subject doesn't step in freeze-frame.
    fun step(dtSeconds: Float, speedPxPerSec: Float) {
        val cadence = strideHz * (0.4f + 0.6f * (speedPxPerSec / referenceSpeedPxPerSec).coerceIn(0f, 1f))
        phase += cadence * 2f * PI.toFloat() * dtSeconds
    }

    fun bobPx(speedPxPerSec: Float, amplitudePx: Float = 4f): Float {
        val intensity = (speedPxPerSec / referenceSpeedPxPerSec).coerceIn(0f, 1f)
        return amplitudePx * intensity * kotlin.math.abs(sin(phase))
    }

    fun tiltDegrees(speedPxPerSec: Float, amplitudeDegrees: Float = 6f): Float {
        val intensity = (speedPxPerSec / referenceSpeedPxPerSec).coerceIn(0f, 1f)
        return amplitudeDegrees * intensity * cos(phase)
    }
}
