package compose.conveyance.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.conveyPress
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyShape
import compose.conveyance.tokens.ConveySize

/**
 * Selection among a small, fixed set of options via one persistent, sliding indicator, instead
 * of separately highlighting/unhighlighting each option in place. The indicator IS the selection
 * -- the same "one element across states" principle as [ConveyStateHost], applied to "which of
 * these" rather than "what state is this."
 *
 * Segments are equal-width; for options whose natural widths differ a lot, consider
 * [ConveyChip]s with [ConveyChip]'s own selected state instead -- this component's sliding
 * indicator assumes uniform segments to avoid the per-item measurement machinery a variable-width
 * indicator would need.
 *
 * ```kotlin
 * var period by remember { mutableStateOf(Period.Week) }
 * ConveySegmentedControl(
 *     options = Period.entries,
 *     selected = period,
 *     onSelect = { period = it },
 * ) { option, selected ->
 *     Text(option.label, color = if (selected) ConveyColor.OnPrimary else ConveyColor.OnSurfaceVariant)
 * }
 * ```
 */
@Composable
fun <T> ConveySegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    label: @Composable (option: T, selected: Boolean) -> Unit,
) {
    require(options.isNotEmpty()) { "ConveySegmentedControl needs at least one option" }
    val morphSpec = grammar["morph"]
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .clip(ConveyShape.Circle)
            .background(ConveyColor.SurfaceContainerHigh)
            .padding(ConveySize.XSmall),
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateFloatAsState(
            targetValue = segmentWidth.value * selectedIndex,
            animationSpec = morphSpec,
            label = "ConveySegmentedControl.indicator",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset.dp)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(ConveyShape.Circle)
                .background(ConveyColor.Primary),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .conveyPress(grammar = grammar, onClick = { onSelect(option) }),
                    contentAlignment = Alignment.Center,
                ) {
                    label(option, option == selected)
                }
            }
        }
    }
}
