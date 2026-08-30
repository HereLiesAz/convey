package compose.conveyance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Text is a composable, not a label.
 *
 * Every other element in this library carries the grammar — shape, color, and hierarchy
 * are never just "styling," they are signals. Type has been the one exception: most
 * Compose surfaces treat [Text] as inert content dropped into a layout. [ConveyKineticText]
 * closes that gap. A headline is not less alive than a FAB; it has simply never been asked
 * to demonstrate it.
 *
 * Each glyph gets its own [ConveyLife] via [phasePerGlyph] — a word breathing in unison is
 * one puppet; a word whose letters breathe out of phase is a colony. That distinction is
 * not decorative, it is the same "resourceful minimalism" argument the Manifesto makes for
 * every other element: don't animate the container when animating the parts says more.
 *
 * On [onGlyphTrigger] (typically a tap), each glyph plays its [ConveyLife.Burst] meaning
 * staggered by [staggerMs] — a wave passing left to right, not a simultaneous flash.
 * That stagger IS the message for a burst: "this word noticed you, one letter at a time."
 *
 * ```kotlin
 * var struck by remember { mutableStateOf(0) }
 *
 * ConveyKineticText(
 *     text = "KINETIC",
 *     idle = ConveyLife.Wobble(period = 4500L),
 *     staggerMs = 90L,
 *     triggerKey = struck,
 *     onClick = { struck++ },
 *     style = MaterialTheme.typography.displayMedium,
 * )
 * ```
 *
 * @param text The word or phrase. Each character becomes its own [conveyLife] scope.
 * @param idle The continuous per-glyph motion. [ConveyLife.None] for text that should sit
 *   still until struck — stillness is itself a valid, deliberate signal (see LIBRARY.md:
 *   not every element must move to prove it's alive; some communicate by refusing to fidget).
 * @param staggerMs Delay between each glyph's phase and, on trigger, its burst. 0 = unison.
 * @param triggerKey Any value; changing it plays [burstMeaning] across all glyphs, staggered.
 * @param burstMeaning Grammar entry for the triggered burst. Defaults to "delight" — reserve
 *   it, per the grammar's own rule, for moments that deserve peak expressiveness.
 * @param onClick Optional; when present the text becomes interactive chrome, not prose.
 */
@Composable
fun ConveyKineticText(
    text: String,
    idle: ConveyLife = ConveyLife.None,
    staggerMs: Long = 90L,
    triggerKey: Any = 0,
    burstMeaning: String = "delight",
    peakBurstScale: Float = 1.4f,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    style: TextStyle = TextStyle.Default,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(modifier = modifier.let { if (onClick != null) it.clickable(onClick = onClick) else it }) {
        text.forEachIndexed { index, glyph ->
            val phase = index * staggerMs
            Text(
                text = glyph.toString(),
                style = style,
                modifier = Modifier
                    .conveyLife(idle, phaseOffset = phase)
                    .conveyLifeBurst(
                        trigger = triggerKeyFor(triggerKey, index),
                        peakScale = peakBurstScale,
                        grammar = grammar,
                        meaning = burstMeaning,
                    ),
            )
        }
    }
}

/**
 * Derives a per-glyph trigger value from a shared [triggerKey] so [Modifier.conveyLifeBurst]
 * fires once per real trigger change per glyph, without every glyph needing its own state.
 * The glyph [index] is folded in only to keep keys distinct across positions in [LaunchedEffect]
 * caches — it does not delay the burst; [staggerMs] does that via [conveyLife]'s own timing.
 */
private fun triggerKeyFor(triggerKey: Any, index: Int): Any =
    if (triggerKey == 0 || triggerKey == false) triggerKey else "$triggerKey#$index"
