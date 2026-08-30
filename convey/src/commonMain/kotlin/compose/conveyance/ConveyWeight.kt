package compose.conveyance

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Visual weight in the Conveyance hierarchy.
 *
 * The Manifesto says: "Dynamic color implicitly prioritizes." That's incomplete.
 * Color alone can't enforce hierarchy — a developer can paint everything Primary.
 * Weight makes the hierarchy structural.
 *
 * Rules enforced at runtime (debug builds):
 *   - [Hero]: Only one per ConveySystem scope. This IS the product's defining moment.
 *             A screen with two heroes has no hero. The system will tell you.
 *   - [Primary]: Strongly interactive. Limited to [ConveySystem.maxPrimary] per scope.
 *             Defaults to 3. A screen with twelve primary actions has no primary action.
 *   - [Secondary]: Supporting. Unlimited. The system trusts you here.
 *   - [Ghost]: Present but inert. Marks decorative elements explicitly — they are not
 *             forgotten, they are acknowledged as intentionally passive.
 *
 * The most important weight is [Hero]. A product that cannot identify its hero moment
 * has not finished designing itself.
 */
enum class ConveyWeight {
    /**
     * The single most important action or element on this surface.
     * The product's identity lives here. If you cannot decide what is Hero,
     * that is a design problem, not a parameter to fudge.
     */
    Hero,

    /**
     * Strongly interactive. Draws the eye. Communicates "this is what you probably want next."
     * Use for primary CTAs, key navigation, the action that advances the user's goal.
     */
    Primary,

    /**
     * Supporting interactive. Visible but not commanding. Offers options, not directives.
     */
    Secondary,

    /**
     * Present but inert. Explicitly declared as non-interactive or decorative.
     * This is not laziness — it is honesty. Ghost elements that behave like Primary
     * elements are the source of the "construction zone" problem.
     */
    Ghost,
}

/**
 * Registers this composable's weight in the ambient [ConveyWeightRegistry].
 *
 * In debug builds, this modifier validates hierarchy constraints on every composition.
 * In release builds, it is a no-op — the enforcement cost is zero in production.
 *
 * ```kotlin
 * Button(
 *     modifier = Modifier.conveyWeight(ConveyWeight.Hero),
 *     onClick = { startCheckout() }
 * ) {
 *     Text("Complete Purchase")
 * }
 * ```
 */
fun Modifier.conveyWeight(weight: ConveyWeight): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "conveyWeight"
        value = weight
    }
) {
    val registry = LocalConveyWeightRegistry.current
    val id = remember { Any() }

    DisposableEffect(weight) {
        registry.register(id, weight)
        onDispose { registry.unregister(id) }
    }

    this
}

/**
 * Tracks all [ConveyWeight] registrations within a [ConveySystem] scope.
 * Enforces hierarchy constraints in debug builds.
 */
@Stable
class ConveyWeightRegistry(
    private val maxPrimary: Int = 3,
    private val enforceInDebug: Boolean = true,
) {
    private val registry = mutableStateMapOf<Any, ConveyWeight>()

    internal fun register(id: Any, weight: ConveyWeight) {
        registry[id] = weight
        if (enforceInDebug) validate()
    }

    internal fun unregister(id: Any) {
        registry.remove(id)
    }

    val heroCount: Int get() = registry.values.count { it == ConveyWeight.Hero }
    val primaryCount: Int get() = registry.values.count { it == ConveyWeight.Primary }
    val secondaryCount: Int get() = registry.values.count { it == ConveyWeight.Secondary }
    val ghostCount: Int get() = registry.values.count { it == ConveyWeight.Ghost }

    private fun validate() {
        if (heroCount > 1) {
            conveyViolation(
                "CONVEY HIERARCHY VIOLATION: $heroCount Hero elements on one surface.\n" +
                "A surface with multiple heroes has no hero. Demote all but one to Primary.\n" +
                "The hero is the answer to: 'What is the single most important thing here?'"
            )
        }
        if (primaryCount > maxPrimary) {
            conveyViolation(
                "CONVEY HIERARCHY VIOLATION: $primaryCount Primary elements (max $maxPrimary).\n" +
                "When everything is primary, nothing is primary. Demote some to Secondary.\n" +
                "Ask: which actions advance the user's goal? Those are Primary. Others are Secondary."
            )
        }
    }

    fun snapshot(): String = buildString {
        appendLine("ConveyWeight Snapshot:")
        appendLine("  Hero:      $heroCount  (max 1)")
        appendLine("  Primary:   $primaryCount  (max $maxPrimary)")
        appendLine("  Secondary: $secondaryCount")
        appendLine("  Ghost:     $ghostCount")
    }
}

/**
 * Public so a consumer can read [ConveyWeightRegistry.snapshot] for its own debug UI or logging --
 * LIBRARY.md's whole audit story is unreachable from outside this module otherwise. [ConveySystem]
 * is still the only thing that provides a non-default value; reading this without one just gets an
 * empty, unshared registry.
 */
val LocalConveyWeightRegistry = staticCompositionLocalOf<ConveyWeightRegistry> {
    ConveyWeightRegistry()
}

// ── Internal ──────────────────────────────────────────────────────────────────

internal expect fun conveyViolation(message: String)

/** Thrown by debug-build violation handlers (see [conveyViolation], [defaultViolationHandler]). */
internal class ConveyViolationException(message: String) : IllegalStateException(message)
