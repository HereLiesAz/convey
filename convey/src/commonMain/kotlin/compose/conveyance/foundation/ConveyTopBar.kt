package compose.conveyance.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import compose.conveyance.ConveyWeight
import compose.conveyance.conveyWeight
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveySize

/**
 * Structural chrome for a screen's top edge: a leading slot (typically back/menu), a
 * weight-declared title, and a trailing action row. Deliberately thin — this is layout and
 * hierarchy, not a new interaction pattern; the leading/action controls inside it should be
 * ordinary pressable content (an icon button wired to [compose.conveyance.conveyPress], say),
 * not something this composable invents its own version of.
 *
 * ```kotlin
 * ConveyTopBar(
 *     title = { Text("Inbox") },
 *     leading = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.conveyPress(onClick = ::openDrawer)) },
 *     actions = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.conveyPress(onClick = ::search)) },
 * )
 * ```
 *
 * @param titleWeight The title's position in the visual hierarchy. Defaults to
 *   [compose.conveyance.ConveyWeight.Primary] — a screen's title is rarely its Hero.
 */
@Composable
fun ConveyTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    height: Dp = ConveySize.Component.TopAppBar,
    color: Color = ConveyColor.Surface,
    contentColor: Color = ConveyColor.OnSurface,
    titleWeight: ConveyWeight = ConveyWeight.Primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(color)
            .padding(horizontal = ConveySize.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            leading?.invoke()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = ConveySize.Small)
                    .conveyWeight(titleWeight),
            ) {
                title()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ConveySize.XSmall),
                content = actions,
            )
        }
    }
}
