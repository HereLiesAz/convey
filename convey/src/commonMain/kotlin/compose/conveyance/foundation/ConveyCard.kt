package compose.conveyance.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import compose.conveyance.ConveyGrammar
import compose.conveyance.ConveyWeight
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.conveyPress
import compose.conveyance.conveyRipple
import compose.conveyance.conveyWeight
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyShape
import compose.conveyance.tokens.ConveySize

/**
 * A weight-aware container surface.
 *
 * Per the channel table (Part IV of the framework spec), elevation is not a taste choice here —
 * it carries a specific meaning: "things that float can be dismissed; things that are flush
 * cannot." [elevation] should say something true about this card, not just look nice: a card the
 * person can swipe away or that sits above a modal-free flow floats; a card that's a fixed,
 * permanent part of the layout (a settings section, a form group) sits flush at
 * [compose.conveyance.tokens.ConveySize.Elevation.None].
 *
 * ```kotlin
 * ConveyCard(
 *     elevation = ConveySize.Elevation.Small, // reversible: this card can be dismissed
 *     onClick = { openDetails() },
 * ) {
 *     ConveyListItem(title = { Text("Recent activity") })
 * }
 * ```
 *
 * @param weight This card's position in the visual hierarchy. See [ConveyWeight].
 * @param elevation Shadow depth — see the doc above for what it actually communicates. Use
 *   [compose.conveyance.tokens.ConveySize.Elevation]'s tokens, not an arbitrary value.
 * @param shape Corner shape. See [compose.conveyance.tokens.ConveyShape].
 * @param color Background fill.
 * @param minHeight Minimum height. Defaults to [compose.conveyance.tokens.ConveySize.Component.CardMinHeight].
 * @param onClick If set, the card is pressable: ripple + press-scale feedback, same as
 *   [ConveyListItem]. If null, the card is a static container.
 * @param content The card's content, laid out in a [Box] with [ConveySize.Medium] padding.
 */
@Composable
fun ConveyCard(
    modifier: Modifier = Modifier,
    weight: ConveyWeight = ConveyWeight.Secondary,
    elevation: Dp = ConveySize.Elevation.XSmall,
    shape: Shape = ConveyShape.Medium,
    color: Color = ConveyColor.SurfaceContainer,
    minHeight: Dp = ConveySize.Component.CardMinHeight,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionModifier = if (onClick != null) {
        Modifier
            .conveyRipple(grammar = grammar)
            .conveyPress(grammar = grammar, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .conveyWeight(weight)
            .shadow(elevation = elevation, shape = shape)
            .clip(shape)
            .background(color)
            .then(interactionModifier)
            .padding(ConveySize.Medium),
    ) {
        content()
    }
}
