package compose.conveyance.foundation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.conveyPress
import compose.conveyance.conveyRipple
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyShape
import compose.conveyance.tokens.ConveySize

/**
 * A compact, selectable tag. Selected/unselected is a morph (background and content color), the
 * same "the element IS its state" principle as [ConveyStateHost] rather than a swapped
 * background drawable.
 *
 * Removal is a plain [onRemove] callback, deliberately not wired to [ConveyReversal] itself --
 * a single chip doesn't know whether its removal should be reversible; compose it inside a
 * [ConveyReversal] yourself if it should be:
 *
 * ```kotlin
 * ConveyReversal(item = tag, state = reversalState) {
 *     ConveyChip(label = { Text(tag.name) }, onRemove = { reversalState.destroy(tag) })
 * }
 * ```
 *
 * @param selected Morphs the chip's colors between selected and unselected.
 * @param leading Optional content before [label] — typically a small icon or a [ConveyAvatar].
 * @param onClick If set, the chip body is pressable (ripple + press-scale).
 * @param onRemove If set, shows a trailing dismiss control that calls this directly.
 */
@Composable
fun ConveyChip(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val morphSpec = grammar["morph"]

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) ConveyColor.SecondaryContainer else ConveyColor.SurfaceContainerHigh,
        animationSpec = morphSpec.toColorSpec(),
        label = "ConveyChip.background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) ConveyColor.OnSecondaryContainer else ConveyColor.OnSurfaceVariant,
        animationSpec = morphSpec.toColorSpec(),
        label = "ConveyChip.content",
    )

    val interactionModifier = if (onClick != null) {
        Modifier
            .conveyRipple(grammar = grammar)
            .conveyPress(grammar = grammar, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .clip(ConveyShape.Circle)
            .background(backgroundColor)
            .then(interactionModifier)
            .padding(horizontal = ConveySize.Medium, vertical = ConveySize.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ConveySize.XSmall),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            leading?.invoke()
            label()
            if (onRemove != null) {
                Box(
                    modifier = Modifier
                        .size(ConveySize.Component.IconSmall)
                        .clip(ConveyShape.Circle)
                        .conveyPress(scale = 0.85f, grammar = grammar, onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "×", color = contentColor)
                }
            }
        }
    }
}

private fun AnimationSpec<Float>.toColorSpec(): AnimationSpec<Color> = when (this) {
    is SpringSpec<Float> -> spring(dampingRatio = dampingRatio, stiffness = stiffness)
    is TweenSpec<Float> -> tween(durationMillis = durationMillis, easing = easing)
    is SnapSpec<Float> -> snap()
    else -> spring()
}
