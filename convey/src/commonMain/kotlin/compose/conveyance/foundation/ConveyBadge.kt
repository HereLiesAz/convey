package compose.conveyance.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import compose.conveyance.ConveyGrammar
import compose.conveyance.LocalConveyGrammar
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyShape

/**
 * A small status indicator anchored to [anchor]'s top-end corner: a bare dot when [count] is
 * null, a count pill otherwise. Appearing/disappearing scales in and out (the "morph" meaning);
 * a genuine count change gets a brief confirm bounce, so a new notification reads as an event,
 * not a silent number swap.
 *
 * ```kotlin
 * ConveyBadge(count = unreadCount) {
 *     Icon(Icons.Default.Notifications, contentDescription = null)
 * }
 * ```
 *
 * @param count Null shows a bare dot (present/absent only). Zero or less hides the badge
 *   entirely. A positive count renders as text, capped at [maxCount] (shown as `"$maxCount+"`).
 * @param color Badge fill.
 * @param contentColor Count text color.
 * @param anchor The element this badge is attached to.
 */
@Composable
fun ConveyBadge(
    modifier: Modifier = Modifier,
    count: Int? = null,
    color: Color = ConveyColor.Error,
    contentColor: Color = ConveyColor.OnError,
    maxCount: Int = 99,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    anchor: @Composable () -> Unit,
) {
    val visible = count == null || count > 0
    val scale = remember { Animatable(if (visible) 1f else 0f) }

    LaunchedEffect(visible) {
        scale.animateTo(if (visible) 1f else 0f, grammar["morph"])
    }
    LaunchedEffect(count) {
        if (visible && count != null) {
            scale.animateTo(1.25f, grammar["confirm"])
            scale.animateTo(1f, grammar["confirm"])
        }
    }

    Box(modifier = modifier) {
        anchor()

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        ) {
            if (scale.value > 0.01f) {
                if (count == null) {
                    Box(Modifier.size(8.dp).clip(ConveyShape.Circle).background(color))
                } else {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                            .clip(ConveyShape.Circle)
                            .background(color)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (count > maxCount) "$maxCount+" else count.toString(),
                            color = contentColor,
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }
        }
    }
}
