package compose.conveyance

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlin.math.pow

/**
 * Practice-decay (§6.3): "The person who has seen it four thousand times wants speed, not
 * pedagogy... its ceremony attenuates with familiarity, in the same way a skilled musician's
 * motions get smaller." Elements track their own operation count; [ConveyPracticeRegistry] is
 * where that count lives, [conveyPracticeDecay] turns a count into a `1f..floor` multiplier, and
 * [decayed]/[conveyPracticedAffordance] are the two places that multiplier gets spent: shortening
 * a motion's ceremony, and silencing a Tell that already taught its lesson.
 *
 * Scoped to the current session — in-memory only, the same as every other Convey registry
 * ([ConveyWeightRegistry], [ConveyEmploymentRegistry]). Whether practice should survive an app
 * restart (so a returning user's first minute back doesn't reset ceremony to first-time levels)
 * is a real product decision this library does not make for you — [seed] a key's count from your
 * own persistence layer if you want that.
 */
@Stable
class ConveyPracticeRegistry {
    private val counts = mutableStateMapOf<Any, Int>()

    /**
     * Records one genuine operation of the element identified by [key]. Call this from the
     * actual interaction handler (e.g. inside `onClick`) — not from composition, since a
     * recomposition is not a person doing the thing.
     */
    fun recordOperation(key: Any) {
        counts[key] = (counts[key] ?: 0) + 1
    }

    /** The number of recorded operations for [key]. Zero for a key that has never operated. */
    fun operationCount(key: Any): Int = counts[key] ?: 0

    /** Sets [key]'s operation count directly, e.g. restored from your own persistence layer. */
    fun seed(key: Any, count: Int) {
        counts[key] = count
    }
}

val LocalConveyPracticeRegistry: ProvidableCompositionLocal<ConveyPracticeRegistry> =
    staticCompositionLocalOf { ConveyPracticeRegistry() }

/** Reads [key]'s current operation count from the ambient [ConveyPracticeRegistry]. */
@Composable
fun conveyPracticeCount(
    key: Any,
    registry: ConveyPracticeRegistry = LocalConveyPracticeRegistry.current,
): Int = registry.operationCount(key)

/**
 * The decay curve: exponential falloff from `1f` (first time, full ceremony) toward [floor] as
 * [operationCount] grows, roughly halfway there at [halfLife] operations. [floor] is where a
 * skilled musician's motions stop getting smaller — practice-decay attenuates ceremony, it does
 * not remove motion's meaning entirely (see [ConveyGrammar] — the world's grammar never changes).
 */
fun conveyPracticeDecay(operationCount: Int, floor: Float = 0.4f, halfLife: Int = 5): Float {
    require(floor in 0f..1f) { "floor must be in 0f..1f, was $floor" }
    require(halfLife > 0) { "halfLife must be positive, was $halfLife" }
    val raw = 0.5f.pow(operationCount.toFloat() / halfLife.toFloat())
    return floor + (1f - floor) * raw
}

/**
 * Applies [decay] (see [conveyPracticeDecay]) to this animation spec's ceremony: a [TweenSpec]
 * gets proportionally shorter, never below [minDurationMillis]; a [SpringSpec] gets
 * proportionally stiffer (snappier), never above [maxStiffness]. `decay = 1f` (unpracticed)
 * leaves the spec unchanged. Other spec kinds (e.g. a snap, which has no ceremony to remove) are
 * returned unchanged — there is no universal "less ceremony" transform for every spec shape.
 */
fun AnimationSpec<Float>.decayed(
    decay: Float,
    minDurationMillis: Int = 80,
    maxStiffness: Float = Spring.StiffnessHigh * 4,
): AnimationSpec<Float> = when (this) {
    is TweenSpec<Float> -> tween(
        durationMillis = (durationMillis * decay).toInt().coerceAtLeast(minDurationMillis),
        delayMillis = delay,
        easing = easing,
    )
    is SpringSpec<Float> -> spring(
        dampingRatio = dampingRatio,
        stiffness = (stiffness / decay).coerceAtMost(maxStiffness),
    )
    else -> this
}

/**
 * [Modifier.conveyAffordance], gated by practice: once [key]'s recorded operation count is
 * greater than zero, [affordance] is silently replaced with [ConveyAffordance.None] — the Tell
 * already taught its lesson once; teaching it again is not compassion, it's noise.
 */
fun Modifier.conveyPracticedAffordance(
    key: Any,
    affordance: ConveyAffordance,
    registry: ConveyPracticeRegistry? = null,
    grammar: ConveyGrammar = ConveyGrammar.Default,
): Modifier = this.composed {
    val resolvedRegistry = registry ?: LocalConveyPracticeRegistry.current
    val practiced = resolvedRegistry.operationCount(key) > 0
    val effectiveAffordance = if (practiced) ConveyAffordance.None else affordance
    this.conveyAffordance(effectiveAffordance, key = key, grammar = grammar)
}
