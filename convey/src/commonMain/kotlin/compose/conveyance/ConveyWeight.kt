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
 * Rules enforced at runtime (see [defaultViolationHandler] for the per-platform behaviour:
 * Android differentiates debug from release via `BuildConfig.DEBUG`; every other target always
 * enforces, because none of them expose a reliable release-mode signal):
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
     *
     * **Name collision, deliberately not renamed.** This is *not* the Conveyance Manifesto's
     * "Ghost" — the destruction-residue/undo concept, where a destroyed item collapses to a
     * reversible trace in place instead of raising a confirm dialog. That idea is implemented
     * separately in this library as [compose.conveyance.foundation.ConveyReversal], named
     * differently precisely because this enum entry already claimed the word. The two are
     * unrelated and differently scoped: this one is a visual-hierarchy tier (how loudly an
     * element speaks), the manifesto's is a lifecycle state (what a deleted thing leaves
     * behind). Renaming this entry would be a source-incompatible API break, so the
     * disambiguation lives here instead.
     */
    Ghost,
}

/**
 * Registers this composable's weight in the ambient [ConveyWeightRegistry].
 *
 * This modifier validates hierarchy constraints on every composition. A violation is reported
 * through [LocalConveyViolationHandler] — which is whatever `ConveySystem(onViolation = ...)`
 * was given, or [defaultViolationHandler] when nothing was provided. On Android that default is
 * a no-op cost in release builds (it logs); on every other target it always throws.
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
    // Read in composition, where the CompositionLocal is actually resolvable, and thread it into
    // the registry: `register` runs inside a DisposableEffect, which is not a composable context.
    val onViolation = LocalConveyViolationHandler.current
    val id = remember { Any() }

    DisposableEffect(weight, onViolation) {
        registry.register(id, weight, onViolation)
        onDispose { registry.unregister(id) }
    }

    this
}

/**
 * Tracks all [ConveyWeight] registrations within a [ConveySystem] scope.
 *
 * Enforces hierarchy constraints, reporting through the handler
 * [Modifier.conveyWeight] hands it (see [register]) — `ConveySystem(onViolation = ...)`'s
 * lambda when one was supplied, otherwise [defaultViolationHandler].
 */
@Stable
class ConveyWeightRegistry(
    private val maxPrimary: Int = 3,
    private val enforceInDebug: Boolean = true,
) {
    private val registry = mutableStateMapOf<Any, ConveyWeight>()

    /**
     * @param onViolation where a constraint violation is reported. [Modifier.conveyWeight] passes
     *   [LocalConveyViolationHandler]'s current value, which is what makes
     *   `ConveySystem(onViolation = ...)` actually fire. `null` falls back to the platform default
     *   ([conveyViolation]).
     */
    internal fun register(
        id: Any,
        weight: ConveyWeight,
        onViolation: ((String) -> Unit)? = null,
    ) {
        registry[id] = weight
        if (enforceInDebug) validate(onViolation)
    }

    internal fun unregister(id: Any) {
        registry.remove(id)
    }

    val heroCount: Int get() = registry.values.count { it == ConveyWeight.Hero }
    val primaryCount: Int get() = registry.values.count { it == ConveyWeight.Primary }
    val secondaryCount: Int get() = registry.values.count { it == ConveyWeight.Secondary }
    val ghostCount: Int get() = registry.values.count { it == ConveyWeight.Ghost }

    private fun validate(onViolation: ((String) -> Unit)?) {
        val report: (String) -> Unit = onViolation ?: { conveyViolation(it) }
        if (heroCount > 1) {
            report(
                "CONVEY HIERARCHY VIOLATION: $heroCount Hero elements on one surface.\n" +
                "A surface with multiple heroes has no hero. Demote all but one to Primary.\n" +
                "The hero is the answer to: 'What is the single most important thing here?'"
            )
        }
        if (primaryCount > maxPrimary) {
            report(
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
