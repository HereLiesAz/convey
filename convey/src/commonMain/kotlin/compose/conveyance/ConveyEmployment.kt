package compose.conveyance

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * The jobs an element can do, per Part IV ("Channel Economy") of the Conveyance Manifesto
 * framework spec — see [conveyEmployment].
 */
enum class ConveyJob {
    /** Offers an act. */
    Invite,

    /** Tells you where you are. */
    Locate,

    /** Shows work happening. */
    Progress,

    /** Shows current state. */
    Report,

    /** Distinguishes one subject from another. */
    Identify,

    /** Binds things together. */
    Group,

    /** Marks a boundary. */
    Separate,

    /** Shows risk. */
    Warn,

    /** Shows work completed. */
    Confirm,

    /** Moves you. */
    Navigate,

    /** Stops what it started. */
    Interrupt,
}

/**
 * Employment — Law 4: "Every element does at least four jobs. Elements with fewer get merged;
 * elements with none get deleted."
 *
 * This is resourceful minimalism made checkable, the same way [conveyWeight] makes hierarchy
 * checkable. Jobs are enumerable ([ConveyJob]) and every declared element names at least
 * [ConveyEmploymentRegistry]'s minimum, or is honestly [ambient] instead of padded to fit.
 *
 * `Invite`, `Progress`, and `Interrupt` travel together in spirit for an element that offers an
 * act — see [compose.conveyance.foundation.ConveyOffer], which already carries all three for
 * you — but this modifier never infers them: a job the registry credits without the code behind
 * it existing is exactly the failure this law exists to catch, not a shortcut past declaring it.
 *
 * ```kotlin
 * Button(
 *     modifier = Modifier.conveyEmployment(
 *         ConveyJob.Invite, ConveyJob.Progress, ConveyJob.Confirm, ConveyJob.Interrupt,
 *     ),
 *     onClick = { submit() },
 * ) { Text("Submit") }
 * ```
 *
 * An element that honestly can't carry the minimum may be declared [ambient] instead —
 * deliberately exempt rather than padded — and the exemption is budgeted per surface (see
 * [ConveyEmploymentRegistry.ambientBudget]) so it cannot quietly become the norm:
 *
 * ```kotlin
 * Spacer(Modifier.conveyEmployment(ambient = true))
 * ```
 *
 * @param jobs What this element does. Below the registry's minimum (default 4) and not
 *   [ambient] logs (or, in an Android debug build, throws) a violation.
 * @param ambient Declares this element deliberately exempt from the minimum, rather than
 *   under-resourced. Budgeted per surface — see [ConveyEmploymentRegistry.ambientBudget].
 */
fun Modifier.conveyEmployment(
    vararg jobs: ConveyJob,
    ambient: Boolean = false,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "conveyEmployment"
        properties["jobs"] = jobs
        properties["ambient"] = ambient
    }
) {
    val registry = LocalConveyEmploymentRegistry.current
    val id = remember { Any() }
    val jobSet = jobs.toSet()

    DisposableEffect(jobSet, ambient) {
        registry.register(id, jobSet, ambient)
        onDispose { registry.unregister(id) }
    }

    this
}

/**
 * Tracks all [conveyEmployment] registrations within a [ConveySystem] scope. Enforces the
 * minimum-jobs and ambient-budget constraints in debug builds.
 */
@Stable
class ConveyEmploymentRegistry(
    private val minimumJobs: Int = 4,
    val ambientBudget: Int = 3,
    private val enforceInDebug: Boolean = true,
) {
    private class Entry(val jobs: Set<ConveyJob>, val ambient: Boolean)

    private val registry = mutableStateMapOf<Any, Entry>()

    internal fun register(id: Any, jobs: Set<ConveyJob>, ambient: Boolean) {
        registry[id] = Entry(jobs, ambient)
        if (enforceInDebug) validate(jobs, ambient)
    }

    internal fun unregister(id: Any) {
        registry.remove(id)
    }

    /** Elements declared [ConveyJob]-under-resourced (fewer than the minimum, and not ambient). */
    val underEmployedCount: Int get() = registry.values.count { !it.ambient && it.jobs.size < minimumJobs }

    /** Elements declared ambient — deliberately exempt from the minimum. */
    val ambientCount: Int get() = registry.values.count { it.ambient }

    private fun validate(jobs: Set<ConveyJob>, ambient: Boolean) {
        if (ambient) {
            if (ambientCount > ambientBudget) {
                conveyViolation(
                    "CONVEY EMPLOYMENT VIOLATION: $ambientCount Ambient elements (budget $ambientBudget).\n" +
                    "Ambient is an exemption, not a default. If most elements need it, the budget is wrong " +
                    "for this surface -- raise it deliberately in ConveyEmploymentRegistry, don't let it drift."
                )
            }
            return
        }
        if (jobs.size < minimumJobs) {
            conveyViolation(
                "CONVEY EMPLOYMENT VIOLATION: element declares only ${jobs.size} job(s) (min $minimumJobs): $jobs.\n" +
                "Below $minimumJobs means merge this with its neighbor. Zero means delete it.\n" +
                "If it honestly can't carry $minimumJobs, declare Modifier.conveyEmployment(ambient = true) " +
                "instead of padding the list."
            )
        }
    }

    fun snapshot(): String = buildString {
        appendLine("ConveyEmployment Snapshot:")
        appendLine("  Under-employed: $underEmployedCount  (min $minimumJobs jobs)")
        appendLine("  Ambient:        $ambientCount  (budget $ambientBudget)")
    }
}

/**
 * Public so a consumer can read [ConveyEmploymentRegistry.snapshot] for its own debug UI or
 * logging, the same reasoning as [LocalConveyWeightRegistry]. [ConveySystem] does not currently
 * provide a non-default value, unlike [LocalConveyWeightRegistry] -- reading this always gets an
 * unshared registry with the default minimum/budget until a caller provides its own via
 * `CompositionLocalProvider(LocalConveyEmploymentRegistry provides ...)`.
 */
val LocalConveyEmploymentRegistry: ProvidableCompositionLocal<ConveyEmploymentRegistry> =
    staticCompositionLocalOf { ConveyEmploymentRegistry() }
