package com.hereliesaz.convey.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hereliesaz.convey.ui.theme.azrienochFontFamily
import com.hereliesaz.convey.ui.theme.psp
import com.hereliesaz.convey.ui.theme.pw
import org.jetbrains.compose.resources.FontResource

/**
 * Renders animated `azrienoch` variable-font text inside a 40dp-stepped percentage
 * container without line-rewrapping jitter.
 *
 * How it works:
 * 1. The outer container width and padding snap to 40dp breakpoints via `.pw`.
 * 2. An invisible ghost `Text` layer renders using [maxBoundsAxes] (set to the widest/heaviest
 *    extremes of your animation) to lock line breaks and bounding box dimensions.
 * 3. The visible `Text` layer animates [activeAxes] smoothly inside the reserved space.
 */
@Composable
fun AzrienochKineticText(
    text: String,
    fontResource: FontResource,
    activeAxes: List<FontVariation.Setting>,
    maxBoundsAxes: List<FontVariation.Setting>,
    modifier: Modifier = Modifier,
    containerWidth: Dp = 88.pw,
    horizontalPadding: Dp = 3.pw,
    fontSize: TextUnit = 4.5.psp(min = 16.sp, max = 38.sp),
    lineHeightMultiplier: Float = 1.3f,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    alignment: Alignment = Alignment.CenterStart,
    maxLines: Int = Int.MAX_VALUE,
) {
    val activeFamily = azrienochFontFamily(
        fontResource = fontResource,
        settings = activeAxes,
    )
    val maxBoundsFamily = azrienochFontFamily(
        fontResource = fontResource,
        settings = maxBoundsAxes,
    )

    val baseStyle = TextStyle(
        fontSize = fontSize,
        lineHeight = fontSize * lineHeightMultiplier,
        textAlign = textAlign,
        color = color,
    )

    Box(
        modifier = modifier
            .width(containerWidth)
            .padding(horizontal = horizontalPadding),
        contentAlignment = alignment,
    ) {
        // Ghost layer: locks line breaks & vertical bounds to the largest glyph expansion
        Text(
            text = text,
            style = baseStyle.copy(fontFamily = maxBoundsFamily),
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0f),
        )

        // Live animated layer: smoothly animates variable font axes without rewrapping
        Text(
            text = text,
            style = baseStyle.copy(fontFamily = activeFamily),
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
