package compose.conveyance

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/**
 * The root of a Conveyance surface.
 *
 * [ConveySystem] is not a theme wrapper. It is a contract. By placing it at the root
 * of a composable tree, you are stating: every element inside this boundary will earn
 * its presence, carry a declared motion meaning, and respect visual hierarchy.
 *
 * Without [ConveySystem], Convey composables still render — they use [ConveyGrammar.Default]
 * and skip enforcement. With [ConveySystem], the contract is active. Violations surface
 * as errors in debug builds and as logged warnings in release builds.
 *
 * The system intentionally does not provide colors or typography. Those belong to your
 * product's design system. [ConveySystem] provides the behavioral contract, not the skin.
 *
 * ```kotlin
 * ConveySystem(
 *     grammar = rememberConveyGrammar {
 *         meaning("navigate") { spring(stiffness = 400f, dampingRatio = 0.82f) }
 *         meaning("confirm")  { spring(stiffness = 700f, dampingRatio = 0.68f) }
 *         meaning("dismiss")  { tween(200, easing = FastOutLinearInEasing) }
 *         meaning("morph")    { spring(stiffness = 300f, dampingRatio = 0.76f) }
 *     },
 *     maxPrimaryWeight = 2,
 * ) {
 *     // Your entire UI
 * }
 * ```
 *
 * @param grammar The motion vocabulary for this surface. Every animation inside must use
 *   a declared meaning. Undeclared meanings throw in debug builds.
 * @param maxPrimaryWeight The maximum number of [ConveyWeight.Primary] elements allowed
 *   on screen simultaneously. Defaults to 3. Raise only with good reason.
 * @param enforceHierarchy Whether to actively enforce [ConveyWeight] rules. Disable
 *   only during migration. Do not disable permanently.
 * @param onViolation Called when a hierarchy or grammar violation is detected.
 *   Defaults to throwing in debug, logging in release.
 */
@Composable
fun ConveySystem(
    grammar: ConveyGrammar = ConveyGrammar.Default,
    maxPrimaryWeight: Int = 3,
    enforceHierarchy: Boolean = true,
    onViolation: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val registry = remember(maxPrimaryWeight, enforceHierarchy) {
        ConveyWeightRegistry(
            maxPrimary = maxPrimaryWeight,
            enforceInDebug = enforceHierarchy,
        )
    }

    CompositionLocalProvider(
        LocalConveyGrammar provides grammar,
        LocalConveyWeightRegistry provides registry,
        LocalConveyViolationHandler provides (onViolation ?: defaultViolationHandler()),
    ) {
        content()
    }
}

// ── CompositionLocals ─────────────────────────────────────────────────────────

val LocalConveyGrammar: ProvidableCompositionLocal<ConveyGrammar> =
    staticCompositionLocalOf { ConveyGrammar.Default }

val LocalConveyViolationHandler: ProvidableCompositionLocal<(String) -> Unit> =
    staticCompositionLocalOf { defaultViolationHandler() }

// ── Violation handling ────────────────────────────────────────────────────────

internal expect fun defaultViolationHandler(): (String) -> Unit

/**
 * A [ConveyGrammar] extended with product-specific meanings.
 * Merges [base] with the additions defined in [block].
 * Throws if any meaning in [block] conflicts with [base].
 */
fun ConveyGrammar.extend(block: ConveyGrammar.Builder.() -> Unit): ConveyGrammar {
    val builder = ConveyGrammar.Builder()
    this.entries.values.forEach { entry ->
        builder.meaning(entry.meaning, entry.description) { entry.spec }
    }
    builder.block()
    return builder.build()
}
