package compose.conveyance.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import compose.conveyance.ConveyGrammar
import compose.conveyance.ConveyWeight
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyExpressiveShape
import compose.conveyance.tokens.ConveyPolygonShape

/**
 * A [ConveyOffer] whose clip shape is a [ConveyExpressiveShape] polygon that changes with
 * [phase] -- [restShapeName] at [ConveyOfferPhase.Invite]/[ConveyOfferPhase.Failure]/
 * [ConveyOfferPhase.Interrupted], [busyShapeName] while [ConveyOfferPhase.Progress], and
 * [resolvedShapeName] once [ConveyOfferPhase.Success] -- the [ConveyExpressiveShape] counterpart
 * to `conveyance-expressive`'s own `MorphControl` template (`expressive.control.morph`),
 * re-mapped from that library's `Act`/`ActState` vocabulary onto this one's own
 * [ConveyOfferPhase] (`Progress`/`Success` here where the original used `Yielding`/`Settled`).
 *
 * The morph itself is [ConveyOffer]'s own -- passing a different [targetShape] on each phase
 * change is exactly what its underlying [ConveyStateHost] already animates between via
 * [rememberAnimatedMorphShape]'s point-sampled path interpolation, the same mechanism every
 * other [ConveyStateHost]-based composable in this library uses. An earlier version of this
 * composable instead drove a continuous, self-computed `androidx.graphics.shapes.Morph`
 * progress every frame (for a "pulses while indefinite work is in flight" effect) and fed the
 * per-frame result in as [targetShape] -- this fought [ConveyStateHost]'s own morph layer, which
 * re-interpolates *from the previous frame's shape* on every single value change, and produced a
 * visibly wrong, never-settling outline (confirmed against a real screenshot, not assumed).
 * [ConveyYield] already exists in this library for a workload that needs to show live progress;
 * this composable's own busy shape is a static waypoint, not a second progress indicator.
 *
 * [targetColor]/[targetContentColor] default to [weight]'s own [ConveyColor.containerFor]/
 * [ConveyColor.contentFor] rather than [ConveyOffer]'s own `Color.Unspecified` default -- a
 * morphing outline with no fill has nothing for the morph to visibly read against.
 */
@Composable
fun ConveyExpressiveOffer(
    purpose: String,
    phase: ConveyOfferPhase,
    onInvoke: () -> Unit,
    restShapeName: String = "circle",
    busyShapeName: String = "cookie9Sided",
    resolvedShapeName: String = "heart",
    weight: ConveyWeight = ConveyWeight.Secondary,
    targetColor: Color = ConveyColor.containerFor(weight),
    targetContentColor: Color = ConveyColor.contentFor(weight),
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    modifier: Modifier = Modifier,
    content: @Composable ConveyStateScope.(phase: ConveyOfferPhase) -> Unit,
) {
    val rest: Shape = remember(restShapeName) { ConveyPolygonShape(ConveyExpressiveShape.byName(restShapeName)) }
    val busy: Shape = remember(busyShapeName) { ConveyPolygonShape(ConveyExpressiveShape.byName(busyShapeName)) }
    val resolved: Shape = remember(resolvedShapeName) { ConveyPolygonShape(ConveyExpressiveShape.byName(resolvedShapeName)) }

    val shape = when (phase) {
        ConveyOfferPhase.Progress -> busy
        ConveyOfferPhase.Success -> resolved
        ConveyOfferPhase.Invite, ConveyOfferPhase.Failure, ConveyOfferPhase.Interrupted -> rest
    }

    ConveyOffer(
        purpose = purpose,
        phase = phase,
        onInvoke = onInvoke,
        weight = weight,
        targetShape = shape,
        targetColor = targetColor,
        targetContentColor = targetContentColor,
        grammar = grammar,
        modifier = modifier,
        content = content,
    )
}
