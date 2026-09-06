package compose.conveyance.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * M3's real fifteen-step type scale — five roles (Display/Headline/Title/Body/Label) times
 * three sizes (Large/Medium/Small), values as specified by Material Design 3, not invented
 * here — ported directly from [conveyance-expressive](https://github.com/HereLiesAz/conveyance-expressive)'s
 * own `ExpressiveType.kt`, one of the wider Conveyance ecosystem's real, spec-grounded style
 * systems (see `ConveyColor`'s own doc comment for the rest of that parity). [family] defaults
 * to [FontFamily.Default]; a caller wanting this library's own Azrienoch typeface passes
 * `conveyTypeFontFamily(...)` instead — this scale doesn't choose a typeface for you any more
 * than `ConveyType.kt` chooses one for [compose.conveyance.tokens.conveyTypeFontFamily]'s own
 * callers.
 */
data class ConveyExpressiveType(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

fun conveyExpressiveType(family: FontFamily = FontFamily.Default): ConveyExpressiveType = ConveyExpressiveType(
    displayLarge = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)

/**
 * Looks up a step by name. Accepts M3's own role names lowerCamelCased (`"titleMedium"`), and
 * the same h2g2-style aliases `conveyance-expressive`'s own `step()` accepts
 * (`hero`/`section`/`lead`/`body`/`eyebrow`/`micro`), so a value authored against either
 * composable-set's scale vocabulary resolves sensibly here too. Falls back to [ConveyExpressiveType.bodyMedium]
 * for an unrecognized name, matching every other template in this vocabulary's own fallback.
 */
fun ConveyExpressiveType.step(name: String): TextStyle = when (name) {
    "displayLarge" -> displayLarge
    "displayMedium", "hero" -> displayMedium
    "displaySmall" -> displaySmall
    "headlineLarge", "section" -> headlineLarge
    "headlineMedium" -> headlineMedium
    "headlineSmall" -> headlineSmall
    "titleLarge", "lead" -> titleLarge
    "titleMedium" -> titleMedium
    "titleSmall", "capsule" -> titleSmall
    "bodyLarge" -> bodyLarge
    "bodyMedium", "body" -> bodyMedium
    "bodySmall" -> bodySmall
    "labelLarge", "eyebrow" -> labelLarge
    "labelMedium", "endCap" -> labelMedium
    "labelSmall", "micro" -> labelSmall
    else -> bodyMedium
}
