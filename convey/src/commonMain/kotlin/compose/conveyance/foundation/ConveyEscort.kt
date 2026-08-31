package compose.conveyance.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar
import kotlinx.coroutines.launch

/**
 * The framework's replacement for disabled controls, validation error summaries, and
 * "please complete all required fields."
 *
 * A [ConveyGate] is a condition standing between a person and an act, and — the part a plain
 * `enabled: Boolean` cannot express — it knows where it lives. Pressing a blocked
 * [ConveyEscorted] does not do nothing, and does not show a message. The element resists at
 * the point of contact (a small Refuse shake) and then carries the person to the gate: the
 * element registered at that gate's location is brought into view and focused.
 *
 * Two conventional constructs collapse into this one mechanism: the disabled control and the
 * "jump to first error" affordance. And the emotional register changes with it. A greyed-out
 * button says *you failed to read the rules*. An escort says *come on, it's this way* — same
 * information, opposite treatment of the person's dignity.
 *
 * ```kotlin
 * val emailGate = remember { ConveyGate("email") { email.isNotBlank() } }
 *
 * OutlinedTextField(
 *     value = email,
 *     onValueChange = { email = it },
 *     modifier = Modifier.conveyGateLocation(emailGate),
 * )
 *
 * ConveyEscorted(gate = emailGate, onClick = { submit() }) { satisfied ->
 *     Text(if (satisfied) "Submit" else "Submit")
 * }
 * ```
 */
@Immutable
class ConveyGate(
    val identity: String,
    val isSatisfied: () -> Boolean,
)

/**
 * Tracks where each [ConveyGate] identity physically lives on a surface, so a blocked
 * [ConveyEscorted] can travel there. Elements register their location via
 * [Modifier.conveyGateLocation]; nothing needs to be wired up manually beyond that.
 */
@Stable
class ConveyEscortRegistry {
    private val locations = mutableStateMapOf<String, suspend () -> Unit>()

    internal fun register(identity: String, travel: suspend () -> Unit) {
        locations[identity] = travel
    }

    internal fun unregister(identity: String) {
        locations.remove(identity)
    }

    /** Brings the element at [identity]'s registered location into view and focuses it. Does nothing if no element has registered that identity. */
    suspend fun escortTo(identity: String) {
        locations[identity]?.invoke()
    }
}

val LocalConveyEscortRegistry = staticCompositionLocalOf { ConveyEscortRegistry() }

/**
 * Marks this composable as the physical location of [gate]. A blocked [ConveyEscorted]
 * elsewhere on the surface that requires this gate will bring this element into view and
 * focus it when pressed.
 */
fun Modifier.conveyGateLocation(
    gate: ConveyGate,
    registry: ConveyEscortRegistry? = null,
): Modifier = this.composed {
    val resolvedRegistry = registry ?: LocalConveyEscortRegistry.current
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    DisposableEffect(gate.identity, resolvedRegistry) {
        resolvedRegistry.register(gate.identity) {
            bringIntoViewRequester.bringIntoView()
            focusRequester.requestFocus()
        }
        onDispose { resolvedRegistry.unregister(gate.identity) }
    }

    Modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .focusRequester(focusRequester)
        .focusable()
}

/**
 * Wraps an act's control with [gate]. While [ConveyGate.isSatisfied] is false, pressing
 * performs the Refuse signature — a brief resistant shake — and escorts the surface to the
 * gate's registered location, rather than doing nothing or rendering a disabled appearance.
 * While satisfied, pressing invokes [onClick] normally.
 *
 * [content] receives whether the gate is currently satisfied; most content should look
 * identical either way — an escorted control stays visually alive, never greyed out, because
 * a genuinely dead control and a gated one are different things (see [compose.conveyance.ConveyWeight.Ghost]
 * for the former).
 */
@Composable
fun ConveyEscorted(
    gate: ConveyGate,
    onClick: () -> Unit,
    registry: ConveyEscortRegistry = LocalConveyEscortRegistry.current,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    modifier: Modifier = Modifier,
    content: @Composable (satisfied: Boolean) -> Unit,
) {
    val satisfied = gate.isSatisfied()
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .graphicsLayer { translationX = shake.value }
            .clickable {
                if (satisfied) {
                    onClick()
                } else {
                    scope.launch {
                        shake.animateTo(-6f, tween(40))
                        shake.animateTo(6f, tween(60))
                        shake.animateTo(0f, tween(60))
                        registry.escortTo(gate.identity)
                    }
                }
            },
    ) {
        content(satisfied)
    }
}
