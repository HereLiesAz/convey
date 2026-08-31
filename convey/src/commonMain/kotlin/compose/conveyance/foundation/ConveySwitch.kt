package compose.conveyance.foundation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.conveyance.ConveyAffordance
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.conveyAffordance
import compose.conveyance.conveyPress
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyShape

/**
 * A toggle. The thumb is one persistent element that slides between positions and morphs color —
 * it is never replaced with a different drawable for on vs. off, the same "one element is one
 * thing across all its states" principle as [ConveyStateHost].
 *
 * ```kotlin
 * var enabled by remember { mutableStateOf(false) }
 * ConveySwitch(checked = enabled, onCheckedChange = { enabled = it })
 * ```
 *
 * @param affordance Self-teaching hint shown on first appearance — see [ConveyAffordance]. Pass
 *   [ConveyAffordance.None] to disable it (e.g. for a switch whose affordance is already obvious
 *   from context).
 */
@Composable
fun ConveySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 52.dp,
    trackHeight: Dp = 32.dp,
    thumbSize: Dp = 24.dp,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    affordance: ConveyAffordance = ConveyAffordance.PressHint(scale = 0.9f),
) {
    val morphSpec = grammar["morph"]
    val trackPadding = (trackHeight - thumbSize) / 2
    val travel = (trackWidth - thumbSize - trackPadding * 2).value

    val trackColor by animateColorAsState(
        targetValue = if (checked) ConveyColor.Primary else ConveyColor.SurfaceContainerHighest,
        animationSpec = morphSpec.toColorSpec(),
        label = "ConveySwitch.track",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) ConveyColor.OnPrimary else ConveyColor.OnSurfaceVariant,
        animationSpec = morphSpec.toColorSpec(),
        label = "ConveySwitch.thumb",
    )
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) travel else 0f,
        animationSpec = morphSpec,
        label = "ConveySwitch.thumbOffset",
    )

    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .clip(ConveyShape.Circle)
            .background(trackColor)
            .conveyAffordance(affordance, key = checked, grammar = grammar)
            .conveyPress(grammar = grammar, onClick = { onCheckedChange(!checked) })
            .padding(trackPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(thumbSize)
                .clip(ConveyShape.Circle)
                .background(thumbColor),
        )
    }
}

private fun AnimationSpec<Float>.toColorSpec(): AnimationSpec<Color> = when (this) {
    is SpringSpec<Float> -> spring(dampingRatio = dampingRatio, stiffness = stiffness)
    is TweenSpec<Float> -> tween(durationMillis = durationMillis, easing = easing)
    is SnapSpec<Float> -> snap()
    else -> spring()
}
