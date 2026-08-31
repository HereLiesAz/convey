package compose.conveyance.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import compose.conveyance.ConveyGrammar
import compose.conveyance.ConveyWeight
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.tokens.ConveyShape
import kotlinx.coroutines.launch

/**
 * The states an offered act moves through. One element renders all of them, via
 * [ConveyStateHost] — there is no separate spinner spawned for [Progress], no separate toast
 * for [Success].
 */
enum class ConveyOfferPhase {
    /** Waiting to be invoked. */
    Invite,

    /** Engaged; the underlying work is in flight. Pair with [ConveyYield] inside [content] for
     * the deformation itself — [ConveyOffer] only owns which phase is showing, not how it looks. */
    Progress,

    /** The act completed. */
    Success,

    /** The act failed. Re-invocable, same as [Invite]. */
    Failure,

    /** The act was cancelled mid-[Progress] via [ConveyOffer]'s `onInterrupt`. */
    Interrupted,
}

/**
 * The framework's `Act`, offered — one declaration carrying what four separate primitives in
 * this library only offer piecemeal:
 * - [purpose]/[weight]/[outcome] are [ConveyConstruct]'s audit trail: what this is for, and
 *   what it produces.
 * - [gate], if set, means this act is blocked until [ConveyGate.isSatisfied]; invoking it while
 *   blocked performs the Refuse-and-escort sequence (see [ConveyEscort]) instead of calling
 *   [onInvoke].
 * - [onInterrupt], if set, is what Law 4 calls the act's owed fourth job: a way to stop it while
 *   it is in [ConveyOfferPhase.Progress].
 * - [ConveyOfferPhase.Invite]/`.Progress`/`.Success`/`.Failure`/`.Interrupted` render from the
 *   same pixels via [ConveyStateHost], which this builds on directly.
 *
 * A *destructive* act's required inverse is not a parameter here — it is [ConveyReversal]
 * wrapping this composable, with `onInvoke` calling [ConveyReversalState.destroy]:
 *
 * ```kotlin
 * ConveyReversal(item = message, state = reversalState) {
 *     ConveyOffer(
 *         purpose = "Delete this message",
 *         phase = ConveyOfferPhase.Invite,
 *         onInvoke = { reversalState.destroy(message) },
 *     ) { Text("Delete") }
 * }
 * ```
 *
 * A non-destructive example:
 *
 * ```kotlin
 * val emailGate = remember { ConveyGate("email") { email.isNotBlank() } }
 * var phase by remember { mutableStateOf(ConveyOfferPhase.Invite) }
 *
 * ConveyOffer(
 *     purpose = "Send the invoice to the client",
 *     weight = ConveyWeight.Primary,
 *     outcome = ConveyOutcome.StateChange("invoice sent"),
 *     gate = emailGate,
 *     phase = phase,
 *     onInvoke = {
 *         scope.launch {
 *             phase = ConveyOfferPhase.Progress
 *             phase = if (send()) ConveyOfferPhase.Success else ConveyOfferPhase.Failure
 *         }
 *     },
 * ) { p ->
 *     when (p) {
 *         ConveyOfferPhase.Invite, ConveyOfferPhase.Failure -> Text("Send")
 *         ConveyOfferPhase.Progress -> CircularProgressIndicator(Modifier.conveyYield(ConveyYield.Indeterminate()))
 *         ConveyOfferPhase.Success -> Icon(Icons.Default.Check, null)
 *         ConveyOfferPhase.Interrupted -> Text("Cancelled")
 *     }
 * }
 * ```
 *
 * @param purpose See [ConveyConstruct]. Required — every offered act declares what it is for.
 * @param phase The act's current [ConveyOfferPhase], owned by the caller: [ConveyOffer] renders
 *   it, it does not drive it, since only the caller's own work knows when it settles.
 * @param onInvoke Called when the act is pressed during [ConveyOfferPhase.Invite] or
 *   [ConveyOfferPhase.Failure], with [gate] (if any) satisfied.
 * @param weight See [ConveyConstruct]/[compose.conveyance.ConveyWeight].
 * @param outcome See [ConveyConstruct]/[compose.conveyance.foundation.ConveyOutcome].
 * @param gate If set, blocks [onInvoke] until satisfied — see [ConveyEscort].
 * @param onInterrupt If set, pressing during [ConveyOfferPhase.Progress] calls this instead of
 *   doing nothing. If null, the act cannot be interrupted once in progress.
 */
@Composable
fun ConveyOffer(
    purpose: String,
    phase: ConveyOfferPhase,
    onInvoke: () -> Unit,
    weight: ConveyWeight = ConveyWeight.Secondary,
    outcome: ConveyOutcome = ConveyOutcome.Unspecified,
    gate: ConveyGate? = null,
    onInterrupt: (() -> Unit)? = null,
    escortRegistry: ConveyEscortRegistry = LocalConveyEscortRegistry.current,
    targetShape: Shape = ConveyShape.Medium,
    targetColor: Color = Color.Unspecified,
    targetContentColor: Color = Color.Unspecified,
    targetWidth: Dp? = null,
    targetHeight: Dp? = null,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    modifier: Modifier = Modifier,
    content: @Composable ConveyStateScope.(phase: ConveyOfferPhase) -> Unit,
) {
    ConveyConstruct(purpose = purpose, weight = weight, produces = outcome) {
        val scope = rememberCoroutineScope()
        val shake = remember { Animatable(0f) }

        val enabled = when (phase) {
            ConveyOfferPhase.Invite, ConveyOfferPhase.Failure -> true
            ConveyOfferPhase.Progress -> onInterrupt != null
            ConveyOfferPhase.Success, ConveyOfferPhase.Interrupted -> false
        }

        val onClick: () -> Unit = click@{
            if (phase == ConveyOfferPhase.Progress) {
                onInterrupt?.invoke()
                return@click
            }

            val satisfied = gate?.isSatisfied?.invoke() ?: true
            if (satisfied) {
                onInvoke()
            } else {
                scope.launch {
                    shake.animateTo(-6f, tween(40))
                    shake.animateTo(6f, tween(60))
                    shake.animateTo(0f, tween(60))
                    gate?.let { escortRegistry.escortTo(it.identity) }
                }
            }
        }

        ConveyStateHost(
            state = phase,
            targetShape = targetShape,
            targetColor = targetColor,
            targetContentColor = targetContentColor,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            grammar = grammar,
            modifier = modifier
                .graphicsLayer { translationX = shake.value }
                .clickable(enabled = enabled, onClick = onClick),
        ) { p -> content(p) }
    }
}
