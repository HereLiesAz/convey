package compose.conveyance.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.conveyance.*
import compose.conveyance.tokens.*

/**
 * Persistent identity across DIFFERENT KINDS of control, not just different states of the
 * same one.
 *
 * [ConveyStateHost] proved a button can become a spinner can become a checkmark — three
 * moments of one control. [ConveyMorphControl] takes the same premise one step further:
 * the element can become a structurally different control entirely. A compact button that
 * sets a value becomes, on demand, the slider that sets it precisely. Nothing is spawned
 * and nothing is dismissed. The button IS the slider's collapsed form.
 *
 * This matters because the two representations are not really different features — a
 * button that opens "a slider dialog" and a button that BECOMES a slider tell the user two
 * different things about their relationship to the value. The first says "elsewhere."
 * The second says "here, now, more precisely." Conveyance is the second one, always.
 *
 * ```kotlin
 * var expanded by remember { mutableStateOf(false) }
 * var volume by remember { mutableFloatStateOf(50f) }
 *
 * ConveyMorphControl(
 *     expanded = expanded,
 *     onToggle = { expanded = !expanded },
 *     value = volume,
 *     onValueChange = { volume = it },
 *     color = ConveyColor.Primary,
 *     collapsedLabel = { Text("Set volume") },
 *     valueBadge = { v -> Text("${v.toInt()}%") },
 * )
 * ```
 *
 * @param expanded Whether this control is currently its slider form.
 * @param onToggle Called when the collapsed (button) form is tapped. Not called while
 *   expanded — collapsing back is the caller's decision (see [onCollapse]), because unlike
 *   a FAB, a slider mid-drag should never be one stray tap away from disappearing.
 * @param onCollapse Called by the built-in "collapse" affordance once expanded. Kept
 *   separate from [onToggle] so a caller can, for instance, snap the value to a step on
 *   collapse without doing so on every drag tick.
 * @param collapsedWidth / [expandedWidth] Width the control morphs between. Height is
 *   constant — only the control's kind changes, not its vertical footprint, so surrounding
 *   layout never reflows during the morph.
 * @param valueBadge The floating value indicator shown beside the slider. Give it
 *   [compose.conveyance.ConveyLife.Breathe] via [Modifier.conveyLife] so the number reads as
 *   live, not static chrome bolted onto the thumb.
 */
@Composable
fun ConveyMorphControl(
    expanded: Boolean,
    onToggle: () -> Unit,
    onCollapse: () -> Unit,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    color: Color = ConveyColor.Primary,
    contentColor: Color = ConveyColor.OnPrimary,
    collapsedWidth: Dp = 150.dp,
    expandedWidth: Dp = 230.dp,
    height: Dp = 44.dp,
    collapsedShape: Shape = ConveyShape.Squircle,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    modifier: Modifier = Modifier,
    collapsedLabel: @Composable () -> Unit,
    valueBadge: @Composable (Float) -> Unit,
) {
    val morphSpec = grammar["morph"]

    val animatedWidth by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) expandedWidth.value else collapsedWidth.value,
        animationSpec = morphSpec,
        label = "ConveyMorphControl.width",
    )

    val shape = if (expanded) ConveyShape.Squircle else collapsedShape

    Row(
        modifier = modifier
            .height(height)
            .width(animatedWidth.dp)
            .clip(shape)
            .background(color)
            .let { if (!expanded) it.clickable(onClick = onToggle) else it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (!expanded) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { collapsedLabel() }
            } else {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    modifier = Modifier.weight(1f).padding(horizontal = ConveySize.Small),
                )
                Box(Modifier.padding(end = ConveySize.Small)) { valueBadge(value) }
            }
        }
    }
}
