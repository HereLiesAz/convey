package compose.conveyance.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.conveyPress
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyShape
import compose.conveyance.tokens.ConveySize

/**
 * Bottom (or rail) navigation among a small, fixed set of destinations, with the same sliding-
 * indicator principle as [ConveySegmentedControl]: which destination is current is shown by one
 * persistent pill moving to sit behind the selected icon, not by separately recoloring each
 * destination in place.
 *
 * ```kotlin
 * ConveyNavigationBar(
 *     destinations = Destination.entries,
 *     selected = current,
 *     onSelect = { current = it },
 *     icon = { dest, selected -> Icon(dest.icon, contentDescription = null,
 *         tint = if (selected) ConveyColor.OnSecondaryContainer else ConveyColor.OnSurfaceVariant) },
 *     label = { dest, _ -> Text(dest.label, style = MaterialTheme.typography.labelSmall) },
 * )
 * ```
 */
@Composable
fun <T> ConveyNavigationBar(
    destinations: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    height: Dp = ConveySize.Component.NavigationBar,
    color: Color = ConveyColor.SurfaceContainer,
    icon: @Composable (destination: T, selected: Boolean) -> Unit,
    label: @Composable (destination: T, selected: Boolean) -> Unit,
) {
    require(destinations.isNotEmpty()) { "ConveyNavigationBar needs at least one destination" }
    val morphSpec = grammar["morph"]
    val selectedIndex = destinations.indexOf(selected).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(color),
    ) {
        val destinationWidth = maxWidth / destinations.size
        val pillWidth = destinationWidth * 0.6f
        val indicatorOffset by animateFloatAsState(
            targetValue = destinationWidth.value * selectedIndex,
            animationSpec = morphSpec,
            label = "ConveyNavigationBar.indicator",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset.dp)
                .width(destinationWidth)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = ConveySize.Small)
                    .size(width = pillWidth, height = 32.dp)
                    .clip(ConveyShape.Circle)
                    .background(ConveyColor.SecondaryContainer),
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            destinations.forEach { destination ->
                val isSelected = destination == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .conveyPress(grammar = grammar, onClick = { onSelect(destination) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    icon(destination, isSelected)
                    label(destination, isSelected)
                }
            }
        }
    }
}
