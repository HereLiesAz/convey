package compose.conveyance.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyShape
import compose.conveyance.tokens.ConveySize

/**
 * A circular identity representation. Shows [content] if provided (typically an `Image`); falls
 * back to [name]'s initials otherwise — the same "one element, honest about what it has" spirit
 * as the rest of this library, rather than a broken-image icon when a picture hasn't loaded yet.
 *
 * ```kotlin
 * ConveyAvatar(name = "Ada Lovelace") // renders "AL"
 *
 * ConveyAvatar(name = "Ada Lovelace") {
 *     AsyncImage(model = user.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
 * }
 * ```
 *
 * @param size Diameter. Defaults to [compose.conveyance.tokens.ConveySize.Component.IconXLarge].
 * @param backgroundColor Fill shown behind [content], and behind the initials fallback.
 * @param contentColor Color of the initials fallback text.
 * @param name Used for the initials fallback (up to two initials) when [content] is null.
 * @param content Custom content, typically an image. When null, falls back to [name]'s initials.
 */
@Composable
fun ConveyAvatar(
    name: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = ConveySize.Component.IconXLarge,
    backgroundColor: Color = ConveyColor.SecondaryContainer,
    contentColor: Color = ConveyColor.OnSecondaryContainer,
    content: @Composable (BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(ConveyShape.Circle)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else if (name != null) {
            Text(
                text = initialsOf(name),
                color = contentColor,
                style = TextStyle(fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

private fun initialsOf(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
