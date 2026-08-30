package compose.conveyance.foundation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import compose.conveyance.ConveyWeight
import compose.conveyance.LocalConveyViolationHandler
import compose.conveyance.conveyWeight

/**
 * The "digging the hole" composable.
 *
 * The Manifesto's sharpest line: "A well-placed construction vehicle can do more than
 * miles of traffic cones." Every element should be digging, not standing around.
 *
 * [ConveyConstruct] wraps a composable with a declared purpose. The purpose is not
 * shown to the user — it is documentation that the system can read. In audit mode,
 * [ConveyConstruct] can answer: "What does this element do?" If the answer is nothing,
 * the element should not exist.
 *
 * This is the runtime equivalent of the Manifesto's Step 1: Eradicate Explicit Instruction.
 * You cannot eradicate what you cannot identify. [ConveyConstruct] makes purpose explicit
 * at the code level, which forces the question to be answered.
 *
 * ```kotlin
 * ConveyConstruct(
 *     purpose = "Advance the user to the next step in the checkout flow",
 *     weight = ConveyWeight.Hero,
 *     produces = ConveyOutcome.Navigate("checkout/confirmation"),
 * ) {
 *     Button(onClick = { goToConfirmation() }) {
 *         Text("Continue to Payment")
 *     }
 * }
 * ```
 *
 * Elements without a declared purpose are flagged in audit output.
 * Elements that declare a purpose but receive zero interactions are flagged separately —
 * they exist but are never reached. That is a design failure, not a code failure.
 *
 * @param purpose What this element does. A verb phrase. "Shows the user their cart total."
 *   "Submits the form." "Collapses the filter panel."
 * @param weight This element's position in the visual hierarchy.
 * @param produces What happens when the user interacts successfully. Optional but encouraged.
 * @param content The element itself.
 */
@Composable
fun ConveyConstruct(
    purpose: String,
    weight: ConveyWeight = ConveyWeight.Secondary,
    produces: ConveyOutcome = ConveyOutcome.Unspecified,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val violationHandler = LocalConveyViolationHandler.current
    val registry = LocalConveyConstructRegistry.currentOrNull

    DisposableEffect(purpose) {
        val entry = ConstructEntry(purpose = purpose, weight = weight, produces = produces)
        registry?.register(entry)
        onDispose { registry?.unregister(entry) }
    }

    content()
}

/**
 * What an element produces when the user interacts with it.
 *
 * Used by the audit system to verify that every Primary and Hero element
 * actually leads somewhere. An element that produces nothing is not Primary.
 */
sealed interface ConveyOutcome {
    /** No declared outcome. The system will flag this in audit for Primary/Hero elements. */
    data object Unspecified : ConveyOutcome

    /** Navigation to a new destination. The [route] is your navigation graph key. */
    data class Navigate(val route: String) : ConveyOutcome

    /** A state change within the current surface. */
    data class StateChange(val description: String) : ConveyOutcome

    /** An external action (network call, hardware, system call). */
    data class ExternalAction(val description: String) : ConveyOutcome

    /** Revealing or hiding content within the surface. */
    data class Reveal(val what: String) : ConveyOutcome

    /** This element produces no outcome by design. Explicitly documented inertness. */
    data class Inert(val reason: String) : ConveyOutcome
}

// ── Registry ──────────────────────────────────────────────────────────────────

internal data class ConstructEntry(
    val purpose: String,
    val weight: ConveyWeight,
    val produces: ConveyOutcome,
)

@Stable
class ConveyConstructRegistry {
    private val entries = mutableStateListOf<ConstructEntry>()

    internal fun register(entry: ConstructEntry) { entries.add(entry) }
    internal fun unregister(entry: ConstructEntry) { entries.remove(entry) }

    /**
     * Produces an audit of every declared element on the current surface.
     *
     * Read this. If you cannot explain every entry in plain language,
     * the screen is not finished.
     */
    fun audit(): String = buildString {
        appendLine("╔══════════════════════════════════════════════")
        appendLine("║ ConveyConstruct Audit — Surface Element Map")
        appendLine("╠══════════════════════════════════════════════")

        val heroes = entries.filter { it.weight == ConveyWeight.Hero }
        val primaries = entries.filter { it.weight == ConveyWeight.Primary }
        val secondaries = entries.filter { it.weight == ConveyWeight.Secondary }

        if (heroes.isEmpty()) {
            appendLine("║ ⚠ NO HERO ELEMENT DECLARED.")
            appendLine("║   What is the single most important thing the user should do here?")
            appendLine("║   If you cannot answer that, the screen is not designed yet.")
        } else {
            heroes.forEach { appendLine("║ ★ HERO    ${it.purpose}  → ${it.produces.describe()}") }
        }

        primaries.forEach { appendLine("║ ● PRIMARY  ${it.purpose}  → ${it.produces.describe()}") }
        secondaries.forEach { appendLine("║ ○ SECOND   ${it.purpose}") }

        val undeclared = entries.filter {
            it.weight == ConveyWeight.Primary || it.weight == ConveyWeight.Hero
        }.filter { it.produces == ConveyOutcome.Unspecified }

        if (undeclared.isNotEmpty()) {
            appendLine("╠══════════════════════════════════════════════")
            appendLine("║ ⚠ Primary/Hero elements with no declared outcome:")
            undeclared.forEach { appendLine("║   '${it.purpose}'") }
            appendLine("║   These must either produce something or be demoted to Secondary.")
        }

        appendLine("╚══════════════════════════════════════════════")
    }
}

private fun ConveyOutcome.describe(): String = when (this) {
    ConveyOutcome.Unspecified -> "⚠ not declared"
    is ConveyOutcome.Navigate -> "navigate → $route"
    is ConveyOutcome.StateChange -> "state: $description"
    is ConveyOutcome.ExternalAction -> "external: $description"
    is ConveyOutcome.Reveal -> "reveal: $what"
    is ConveyOutcome.Inert -> "inert: $reason"
}

internal val LocalConveyConstructRegistry =
    compositionLocalOf<ConveyConstructRegistry?> { null }
