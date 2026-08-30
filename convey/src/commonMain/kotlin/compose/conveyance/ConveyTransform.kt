package compose.conveyance

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Transform composables — the physical language of interaction.
 *
 * These are not animations for their own sake. Each transform communicates something:
 *
 * Scale on press → "I received your input"
 * Lift on hover  → "I can be interacted with"
 * Rotate hint    → "I have an orientation or direction"
 * Fade in        → "I am new here — notice me, then I'll be quiet"
 *
 * The Manifesto says: "Shape/Morphing: use varied forms to direct focus without literal arrows."
 * Transform is how shape becomes dynamic. Static shape is vocabulary. Transform is syntax.
 *
 * All transforms use the ambient [ConveyGrammar] by default. Override [grammar] only when
 * a specific UI element needs isolated motion behavior — for example, an element that
 * uses "delight" motion independent of the surface's default timing.
 */

/**
 * DSL entry point for composing multiple transforms.
 *
 * ```kotlin
 * Button(
 *     modifier = Modifier.conveyTransform {
 *         scaleOnPress()
 *         liftOnHover(elevation = 8.dp)
 *     }
 * )
 * ```
 */
fun Modifier.conveyTransform(
    grammar: ConveyGrammar = ConveyGrammar.Default,
    block: @Composable ConveyTransformScope.() -> Modifier,
): Modifier = composed {
    val scope = ConveyTransformScope(grammar)
    this.then(scope.block())
}

@Stable
class ConveyTransformScope(private val grammar: ConveyGrammar) {

    /**
     * Scale down on press, recover with spring on release.
     *
     * The recovery overshoot from the grammar's "confirm" spring tells the user
     * their touch was received. The spring does not just restore scale — it confirms.
     *
     * @param pressedScale Scale at peak press. Default 0.94f.
     * @param recoveryMeaning Which grammar entry drives the recovery spring.
     */
    @Composable
    fun Modifier.scaleOnPress(
        pressedScale: Float = 0.94f,
        recoveryMeaning: String = "confirm",
    ): Modifier = composed {
        val scale = remember { Animatable(1f) }
        val scope = rememberCoroutineScope()
        val recovery = grammar[recoveryMeaning]
        val immediate: AnimationSpec<Float> = tween(80, easing = FastOutLinearInEasing)

        this
            .scale(scale.value)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { scale.animateTo(pressedScale, immediate) }
                        tryAwaitRelease()
                        scale.animateTo(1f, recovery)
                    }
                )
            }
    }

    /**
     * Translate upward and increase shadow on pointer hover.
     *
     * Physical lift communicates that the element is above the surface and can be engaged.
     * This is the digital equivalent of a card rising from a table.
     *
     * @param elevation How many dp to translate upward at peak hover.
     * @param scaleUp How much to scale up at peak hover.
     * @param meaning Grammar entry for the lift animation.
     */
    @Composable
    fun Modifier.liftOnHover(
        elevation: Dp = 8.dp,
        scaleUp: Float = 1.03f,
        meaning: String = "reveal",
    ): Modifier = composed {
        val translateY = remember { Animatable(0f) }
        val scale = remember { Animatable(1f) }
        val scope = rememberCoroutineScope()
        val spec = grammar[meaning]

        this
            .graphicsLayer {
                this.translationY = translateY.value
                this.scaleX = scale.value
                this.scaleY = scale.value
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val entered = event.changes.any { it.pressed }
                        val hovered = event.changes.any { !it.isOutOfBounds(size, extendedTouchPadding) }
                        if (hovered && !entered) {
                            scope.launch {
                                translateY.animateTo(-elevation.value, spec)
                                scale.animateTo(scaleUp, spec)
                            }
                        } else {
                            scope.launch {
                                translateY.animateTo(0f, spec)
                                scale.animateTo(1f, spec)
                            }
                        }
                    }
                }
            }
    }

    /**
     * Rotate by [degrees] when the pointer enters.
     *
     * Useful for icon buttons, directional indicators, and decorative accents that
     * should respond to proximity. The rotation implies directionality or liveliness.
     *
     * @param degrees Rotation at peak hover. Positive = clockwise.
     * @param meaning Grammar entry for the rotation animation.
     */
    @Composable
    fun Modifier.rotateOnHover(
        degrees: Float = 8f,
        meaning: String = "reveal",
    ): Modifier = composed {
        val rotation = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val spec = grammar[meaning]

        this
            .graphicsLayer { rotationZ = rotation.value }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val hovered = event.changes.any { !it.isOutOfBounds(size, extendedTouchPadding) }
                        scope.launch {
                            rotation.animateTo(if (hovered) degrees else 0f, spec)
                        }
                    }
                }
            }
    }

    /**
     * Scale and fade in from [initialScale] to 1f on first composition.
     *
     * New elements should announce their presence, then become quiet.
     * This animates once — on entry. It does not repeat. It does not pulse.
     * It introduces, then trusts that the user noticed.
     *
     * @param initialScale Starting scale. 0.85f gives a "grow in" feel.
     * @param initialAlpha Starting opacity.
     * @param meaning Grammar entry for the reveal animation.
     */
    @Composable
    fun Modifier.scaleIn(
        initialScale: Float = 0.85f,
        initialAlpha: Float = 0f,
        meaning: String = "reveal",
    ): Modifier = composed {
        val scale = remember { Animatable(initialScale) }
        val alpha = remember { Animatable(initialAlpha) }
        val spec = grammar[meaning]

        LaunchedEffect(Unit) {
            kotlinx.coroutines.coroutineScope {
                launch { scale.animateTo(1f, spec) }
                launch { alpha.animateTo(1f, spec) }
            }
        }

        this.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        }
    }

    /**
     * Translate in from [offsetDp] on the specified axis.
     *
     * Content that slides in carries spatial information: it came from somewhere.
     * "Navigate" transitions use this — the new screen slides in from the direction
     * of the navigation hierarchy.
     *
     * @param offsetDp Starting translation. Positive = from right (horizontal) or bottom (vertical).
     * @param horizontal If true, translates along X. If false, along Y.
     * @param meaning Grammar entry. Should be "navigate" for navigation transitions.
     */
    @Composable
    fun Modifier.slideIn(
        offsetDp: Float = 32f,
        horizontal: Boolean = false,
        meaning: String = "navigate",
    ): Modifier = composed {
        val offset = remember { Animatable(offsetDp) }
        val spec = grammar[meaning]

        LaunchedEffect(Unit) { offset.animateTo(0f, spec) }

        if (horizontal) this.graphicsLayer { translationX = offset.value }
        else this.graphicsLayer { translationY = offset.value }
    }
}

// ── Utility extensions ────────────────────────────────────────────────────────

private fun Modifier.graphicsLayer(block: GraphicsLayerScope.() -> Unit): Modifier =
    this.then(Modifier.graphicsLayer(block))
