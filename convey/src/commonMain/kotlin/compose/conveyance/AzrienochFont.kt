package com.hereliesaz.convey.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.Font

/**
 * Builds an [FontFamily] for the official `azrienoch` variable font with arbitrary
 * OpenType variation axes (standard axes like `wght`, `wdth`, `slnt`, `opsz`, `GRAD`,
 * or custom 4-character tags).
 *
 * Pass your generated Compose Multiplatform resource (e.g., `Res.font.azrienoch`)
 * into [fontResource], or wire a default provider in your theme module.
 */
@Composable
fun azrienochFontFamily(
    fontResource: FontResource,
    vararg settings: FontVariation.Setting,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): FontFamily = FontFamily(
    Font(
        resource = fontResource,
        weight = weight,
        style = style,
        variationSettings = FontVariation.Settings(*settings),
    )
)

/**
 * Convenience overload accepting a list of [FontVariation.Setting] entries.
 */
@Composable
fun azrienochFontFamily(
    fontResource: FontResource,
    settings: List<FontVariation.Setting>,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): FontFamily = azrienochFontFamily(
    fontResource = fontResource,
    settings = settings.toTypedArray(),
    weight = weight,
    style = style,
)

/**
 * Helper to create a custom 4-character OpenType axis setting cleanly.
 * Tags shorter than 4 characters are right-padded with spaces per OpenType spec.
 */
fun axisSetting(tag: String, value: Float): FontVariation.Setting {
    val normalizedTag = tag.padEnd(4, ' ').take(4)
    return FontVariation.Setting(normalizedTag, value)
}
