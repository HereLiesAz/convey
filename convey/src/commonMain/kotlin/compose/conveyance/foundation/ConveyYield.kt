package compose.conveyance.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * The framework's replacement for spinners, progress bars, loading skeletons, and "please wait."
 *
 * The engaged element deforms under load, in place, instead of a separate progress object
 * appearing beside it. A separate object severs the link between what a person touched and
 * what is happening — precisely the link they are trying to learn. [conveyYield] keeps that
 * link by having the same pixels the person touched fill and compress as the work proceeds.
 *
 * [ConveyYield.Determinate] deforms proportionally to [ConveyYield.Determinate.progress].
 * [ConveyYield.Indeterminate] deforms rhythmically, looping until the state changes away from
 * it — there is no separate "how long is this" question for the person to ask, because the
 * element visibly keeps working until it stops.
 *
 * ```kotlin
 * var yield by remember { mutableStateOf<ConveyYield>(ConveyYield.Idle) }
 *
 * Button(
 *     onClick = {
 *         scope.launch {
 *             yield = ConveyYield.Indeterminate()
 *             submit()
 *             yield = ConveyYield.Idle
 *         }
 *     },
 *     modifier = Modifier.conveyYield(yield),
 * ) { Text("Submit") }
 * ```
 */
sealed interface ConveyYield {
    /** Not engaged. No deformation. */
    data object Idle : ConveyYield

    /** Engaged, with a known completion fraction. The element deforms proportionally to [progress] (coerced to `0f..1f`). */
    data class Determinate(val progress: Float) : ConveyYield

    /** Engaged, with no known completion fraction. The element deforms rhythmically, looping every [period]ms, until the state changes away from [Indeterminate]. */
    data class Indeterminate(val period: Long = 1100L) : ConveyYield
}

/**
 * Applies [yield]'s deformation directly to this element: a proportional or rhythmic fill drawn
 * behind [Modifier]'s content, plus a slight compression while engaged — the same channel
 * (deformation), the same element, for every kind of load. See [ConveyYield] for the states.
 *
 * @param fillColor The deformation's fill color. Defaults to a low-alpha overlay so it reads
 *   against any content color without callers needing to match their own palette.
 */
fun Modifier.conveyYield(
    yield: ConveyYield,
    fillColor: Color = Color.Unspecified,
): Modifier = this.composed {
    val fraction = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(yield) {
        when (yield) {
            ConveyYield.Idle -> {
                fraction.animateTo(0f, tween(150))
                scale.animateTo(1f, tween(150))
            }
            is ConveyYield.Determinate -> {
                scale.animateTo(0.97f, tween(150))
                fraction.animateTo(yield.progress.coerceIn(0f, 1f), tween(200))
            }
            is ConveyYield.Indeterminate -> {
                scale.animateTo(0.97f, tween(150))
                val halfPeriod = (yield.period / 2).toInt().coerceAtLeast(1)
                while (true) {
                    fraction.animateTo(1f, tween(halfPeriod, easing = FastOutSlowInEasing))
                    fraction.animateTo(0f, tween(halfPeriod, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    val resolvedFill = if (fillColor == Color.Unspecified) Color.Black.copy(alpha = 0.12f) else fillColor

    this
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .drawWithContent {
            drawContent()
            if (fraction.value > 0f) {
                drawRect(
                    color = resolvedFill,
                    size = Size(size.width * fraction.value, size.height),
                )
            }
        }
}
