package compose.conveyance.foundation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import compose.conveyance.ConveyKineticText
import compose.conveyance.ConveyPracticeRegistry
import compose.conveyance.LocalConveyPracticeRegistry
import kotlinx.coroutines.delay

/**
 * The Decoration channel — Part IV §4.2 of the Conveyance Manifesto ("Text as an Act"): any
 * span of text that **is** an Act, rather than merely describing one, carries a persistent
 * visual marker distinguishing it from the static text around it. Plain text never borrows
 * Decoration for emphasis — the moment it did, the signal would stop meaning "you can act on
 * this."
 *
 * [ConveyActText] is that persistent marker (a literal `TextDecoration.Underline`, the same
 * convention the manifesto's own web port renders via its `decoration` channel) *plus* the
 * taught half: an unpracticed instance performs one Tell-scale burst through the existing
 * kinetic-typography engine ([ConveyKineticText]) shortly after first appearing, exactly once —
 * §4.2 is explicit that this is one signal in two registers, not two separate mechanisms, and
 * that the taught half draws on the vocabulary already in the framework rather than a bespoke
 * gesture. [ConveyPracticeRegistry] is what remembers "already taught": once [key] has recorded
 * a real operation (a click), the burst never fires again for it.
 *
 * ```kotlin
 * ConveyActText(text = "terms of service", onClick = { openTerms() })
 * ```
 */
@Composable
fun ConveyActText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    key: Any = text,
    registry: ConveyPracticeRegistry? = null,
) {
    val resolvedRegistry = registry ?: LocalConveyPracticeRegistry.current
    var burstTrigger by remember(key) { mutableIntStateOf(0) }
    val alreadyTaught = resolvedRegistry.operationCount(key) > 0

    LaunchedEffect(key, alreadyTaught) {
        if (!alreadyTaught) {
            delay(TELL_DELAY_MS)
            burstTrigger++
        }
    }

    ConveyKineticText(
        text = text,
        triggerKey = burstTrigger,
        burstMeaning = "confirm",
        style = style.merge(TextStyle(textDecoration = TextDecoration.Underline)),
        modifier = modifier,
        onClick = {
            resolvedRegistry.recordOperation(key)
            onClick()
        },
    )
}

/**
 * Static Decoration only, no Tell — for a text Act rendered inside a context (like
 * [compose.conveyance.foundation.ConveyDesign]) that already drives its own motion. Never used
 * for plain, non-interactive text; that would be exactly the borrowed-emphasis failure §4.2
 * warns against.
 */
@Composable
fun ConveyDecoratedText(text: String, modifier: Modifier = Modifier, style: TextStyle = TextStyle.Default) {
    Text(text = text, modifier = modifier, style = style.merge(TextStyle(textDecoration = TextDecoration.Underline)))
}

private const val TELL_DELAY_MS = 400L
