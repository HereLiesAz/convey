package compose.conveyance.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import compose.conveyance.ConveyGrammar
import compose.conveyance.ConveyWeight
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.conveyPress
import compose.conveyance.conveyRipple
import compose.conveyance.conveyWeight
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveySize

/**
 * A single row: the most common visual object in any product. One element carrying a leading
 * slot (icon/avatar), a title, an optional subtitle, and a trailing slot (icon/action/value) --
 * weight-aware and grammar-driven, the same way every other Convey element is, rather than a
 * bare `Row` a product has to re-assemble by hand on every screen.
 *
 * ```kotlin
 * ConveyListItem(
 *     title = { Text("Invoice #4021") },
 *     subtitle = { Text("Due in 3 days") },
 *     leading = { ConveyAvatar(name = "Acme Co") },
 *     trailing = { Text("$412.00") },
 *     onClick = { openInvoice() },
 * )
 * ```
 *
 * @param title The item's primary label. Rendered at [compose.conveyance.tokens.ConveyColor.OnSurface].
 * @param subtitle Secondary detail beneath [title], if any. Rendered at
 *   [compose.conveyance.tokens.ConveyColor.OnSurfaceVariant].
 * @param leading Content before the text column -- typically a [ConveyAvatar] or an icon.
 * @param trailing Content after the text column -- typically a value, an icon, or a small control.
 * @param weight This row's position in the visual hierarchy. See [compose.conveyance.ConveyWeight].
 * @param minHeight The row's minimum height. Defaults to [ConveySize.Component.ListItem]; use
 *   [ConveySize.Component.ListItemSmall]/`Large`/`XLarge` for denser or roomier lists.
 * @param onClick If set, the row is pressable: ripple + press-scale feedback via
 *   [compose.conveyance.conveyRipple]/[compose.conveyance.conveyPress]. If null, the row is
 *   inert -- caller is still responsible for [compose.conveyance.conveyInert] if that's
 *   deliberate, per the audit story in [compose.conveyance.ConveyAffordance].
 */
@Composable
fun ConveyListItem(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    weight: ConveyWeight = ConveyWeight.Secondary,
    minHeight: Dp = ConveySize.Component.ListItem,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    onClick: (() -> Unit)? = null,
) {
    val interactionModifier = if (onClick != null) {
        Modifier
            .conveyRipple(grammar = grammar)
            .conveyPress(grammar = grammar, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .conveyWeight(weight)
            .then(interactionModifier)
            .padding(horizontal = ConveySize.Medium, vertical = ConveySize.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ConveySize.Medium),
    ) {
        leading?.invoke()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ConveySize.XSmall),
        ) {
            CompositionLocalProvider(LocalContentColor provides ConveyColor.OnSurface) { title() }
            subtitle?.let {
                CompositionLocalProvider(LocalContentColor provides ConveyColor.OnSurfaceVariant) { it() }
            }
        }

        trailing?.invoke()
    }
}
