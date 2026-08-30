package compose.conveyance

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*

/**
 * Composable interaction layer.
 *
 * The Manifesto says: "Physics/Motion: interaction feedback should demonstrate purpose
 * and train muscle memory." This is what that means in code.
 *
 * Every interaction in Convey is designed to teach:
 *   - Ripple teaches WHERE the touch registered
 *   - Press scale teaches THAT the element received the touch
 *   - Long press progress teaches HOW MUCH LONGER to hold
 *   - Swipe resistance teaches THAT there is content beyond the edge
 *
 * None of these are decorative. Each carries semantic cargo.
 */

// ── Ripple ────────────────────────────────────────────────────────────────────

/**
 * A bounded ripple that marks the exact point of contact.
 *
 * Unlike generic ripple effects, this one scales from the touch point outward,
 * which teaches the user exactly where their input was registered.
 * This is information, not decoration.
 *
 * @param color The ripple color. Should be OnSurface at ~30% alpha.
 * @param bounded Whether the ripple is clipped to the element's bounds.
 *   Bounded = touch is received; Unbounded = touch radiates outward (for icon buttons).
 * @param meaning Grammar meaning for the ripple expansion animation.
 */
fun Modifier.conveyRipple(
    color: Color = Color.White.copy(alpha = 0.28f),
    bounded: Boolean = true,
    grammar: ConveyGrammar = ConveyGrammar.Default,
    meaning: String = "confirm",
): Modifier = composed {
    val ripples = remember { mutableStateListOf<RippleState>() }
    val scope = rememberCoroutineScope()
    val spec = grammar[meaning]

    this
        .drawWithContent {
            drawContent()
            ripples.forEach { ripple ->
                val progress = ripple.expand.value
                drawCircle(
                    color = color.copy(alpha = color.alpha * (1f - progress)),
                    radius = progress * size.minDimension * (if (bounded) 0.9f else 1.4f),
                    center = if (bounded) ripple.center else center,
                )
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    val ripple = RippleState(center = offset, expand = Animatable(0f))
                    ripples.add(ripple)
                    scope.launch {
                        ripple.expand.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
                    }
                    tryAwaitRelease()
                    ripples.remove(ripple)
                }
            )
        }
}

private class RippleState(
    val center: Offset,
    val expand: Animatable<Float, AnimationVector1D>,
)

// ── Press scale ───────────────────────────────────────────────────────────────

/**
 * Physical press feedback via scale.
 *
 * The element shrinks when pressed and recovers with a spring when released.
 * The recovery overshoot (controlled by the grammar's "confirm" spring) teaches
 * that the press was accepted — the spring says "got it."
 *
 * The press-down is always fast (it must feel immediate — latency here breaks trust).
 * The recovery uses the grammar's declared spring, which gives the product its personality.
 *
 * @param scale How small the element becomes at peak press. 0.94f is Convey standard.
 * @param meaning Grammar entry for the recovery animation. "confirm" by default.
 */
fun Modifier.conveyPress(
    scale: Float = 0.94f,
    grammar: ConveyGrammar = ConveyGrammar.Default,
    meaning: String = "confirm",
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    val scaleAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val recoverySpec = grammar[meaning]
    val pressSpec: AnimationSpec<Float> = tween(80, easing = FastOutLinearInEasing)

    this
        .scale(scaleAnim.value)
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    scope.launch { scaleAnim.animateTo(scale, pressSpec) }
                    val released = tryAwaitRelease()
                    scope.launch {
                        scaleAnim.animateTo(1f, recoverySpec)
                    }
                    if (released) onClick?.invoke()
                }
            )
        }
}

// ── Long press ────────────────────────────────────────────────────────────────

/**
 * Long press with progressive disclosure.
 *
 * The Manifesto's construction zone analogy: a well-placed indicator does more
 * than miles of text labels. The progress arc on a long-press button tells the user
 * exactly how much longer to hold — no tooltip, no label, no text.
 *
 * The progress is drawn as an arc around the element's center. It appears only
 * after [initiationDelay] — the first few milliseconds feel like a normal tap,
 * so accidental long-presses don't trigger the affordance.
 *
 * @param durationMs How long the user must hold before [onLongPress] fires.
 * @param initiationDelay How long before the progress arc appears. Prevents flicker on taps.
 * @param progressColor Color of the progress arc.
 * @param onLongPress Called when the user holds for [durationMs] milliseconds.
 */
fun Modifier.conveyLongPress(
    durationMs: Long = 600L,
    initiationDelay: Long = 120L,
    progressColor: Color = Color.White.copy(alpha = 0.7f),
    onLongPress: () -> Unit,
): Modifier = composed {
    var progress by remember { mutableFloatStateOf(0f) }
    var showing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    this
        .drawWithContent {
            drawContent()
            if (showing && progress > 0f) {
                val sweep = 360f * progress
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        .pointerInput(durationMs, onLongPress) {
            detectTapGestures(
                onPress = {
                    progress = 0f
                    showing = false
                    var elapsed = 0L
                    val step = 16L
                    val job = scope.launch {
                        delay(initiationDelay)
                        showing = true
                        while (elapsed < durationMs) {
                            elapsed += step
                            progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                            delay(step)
                        }
                        onLongPress()
                    }
                    val held = tryAwaitRelease()
                    job.cancel()
                    progress = 0f
                    showing = false
                }
            )
        }
}

// ── Swipe resistance ──────────────────────────────────────────────────────────

/**
 * Drag with resistance — the element follows the pointer but pushes back.
 *
 * Resistance teaches the user that the element CAN be dragged but does not want to be
 * moved freely. It communicates: "drag here is meaningful, not free-form."
 *
 * This is used for dismissible items, reorderable lists, and swipe-to-action rows.
 * The resistance factor maps gesture distance to visual offset non-linearly —
 * small gestures produce almost no offset, large gestures produce moderate offset.
 * This is honest: the offset shows effort without implying the action is easy.
 *
 * @param direction Which axis of drag is permitted.
 * @param resistance How much the element resists. 1f = no resistance, 0.3f = very resistant.
 * @param threshold Fraction of [maxDrag] at which [onSwipe] fires.
 * @param maxDrag Maximum drag distance before clamping.
 * @param onSwipe Called when the user drags past [threshold] of [maxDrag].
 */
fun Modifier.conveySwipe(
    direction: SwipeDirection = SwipeDirection.Horizontal,
    resistance: Float = 0.4f,
    threshold: Float = 0.5f,
    maxDrag: Dp = 120.dp,
    grammar: ConveyGrammar = ConveyGrammar.Default,
    onSwipe: (SwipeDirection) -> Unit,
): Modifier = composed {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissSpec = grammar["dismiss"]

    this
        .graphicsLayer {
            if (direction == SwipeDirection.Horizontal || direction == SwipeDirection.Left || direction == SwipeDirection.Right)
                translationX = offset.value
            else
                translationY = offset.value
        }
        .pointerInput(direction, threshold, maxDrag) {
            val maxPx = maxDrag.toPx()
            detectHorizontalDragGestures(
                onDragEnd = {
                    val swipeDir = if (offset.value > 0) SwipeDirection.Right else SwipeDirection.Left
                    if (kotlin.math.abs(offset.value) > maxPx * threshold) {
                        onSwipe(swipeDir)
                    }
                    scope.launch {
                        offset.animateTo(0f, animationSpec = dismissSpec)
                    }
                },
                onHorizontalDrag = { _, delta ->
                    scope.launch {
                        val rawTarget = offset.value + delta
                        val resistedTarget = rawTarget * resistance * (1f - kotlin.math.abs(offset.value) / (maxPx * 2f))
                        offset.snapTo((offset.value + resistedTarget * (1f - resistance)).coerceIn(-maxPx, maxPx))
                    }
                }
            )
        }
}

enum class SwipeDirection { Horizontal, Vertical, Left, Right, Up, Down }

// ── Modifier extension ────────────────────────────────────────────────────────

private fun Modifier.graphicsLayer(block: GraphicsLayerScope.() -> Unit): Modifier =
    this.then(Modifier.graphicsLayer(block))
