package compose.conveyance.foundation

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.conveyance.*

/**
 * A single element that is multiple things over time.
 *
 * This is the central primitive of Resourceful Minimalism: "One element should transition
 * through multiple states rather than using three separate components."
 *
 * [ConveyStateHost] is not AnimatedContent. AnimatedContent swaps content.
 * [ConveyStateHost] is one persistent entity that BECOMES different things.
 * The user never sees a new element appear — they see the same element transform.
 * That transformation communicates the relationship between states.
 *
 * States are declared upfront as an enum. The host manages transitions between them.
 * Each transition is a morph: shape changes, size changes, color changes, content changes —
 * all driven by the grammar's "morph" meaning unless explicitly overridden.
 *
 * The canonical example: a submit button.
 *
 * ```kotlin
 * enum class SubmitState { Idle, Loading, Success, Error }
 *
 * ConveyStateHost(
 *     state = submitState,
 *     grammar = grammar,
 * ) { state, scope ->
 *     when (state) {
 *         SubmitState.Idle -> scope.IdleButton(onClick = { submit() })
 *         SubmitState.Loading -> scope.Spinner()
 *         SubmitState.Success -> scope.Checkmark()
 *         SubmitState.Error -> scope.ErrorPulse()
 *     }
 * }
 * ```
 *
 * The button does not disappear when loading starts. THE BUTTON BECOMES THE SPINNER.
 * The spinner does not disappear when success happens. THE SPINNER BECOMES THE CHECKMARK.
 * The user's eye follows a single object through its journey. That is Conveyance.
 *
 * @param state The current state. Changes trigger morphs.
 * @param targetShape Shape to morph toward. Can change with state.
 * @param targetColor Background color to morph toward.
 * @param targetWidth Width to morph toward. Null means wrap content.
 * @param targetHeight Height to morph toward. Null means wrap content.
 * @param grammar Motion vocabulary. Uses "morph" meaning for shape/color/size transitions.
 * @param content Content for the current state, provided with a [ConveyStateScope].
 */
@Composable
fun <S : Any> ConveyStateHost(
    state: S,
    targetShape: Shape = ConveyShape.Medium,
    targetColor: Color = Color.Unspecified,
    targetContentColor: Color = Color.Unspecified,
    targetWidth: Dp? = null,
    targetHeight: Dp? = null,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    modifier: Modifier = Modifier,
    content: @Composable ConveyStateScope.(state: S) -> Unit,
) {
    val morphSpec = grammar["morph"]

    val animatedColor by animateColorAsState(
        targetValue = if (targetColor == Color.Unspecified) Color.Transparent else targetColor,
        animationSpec = morphSpec.toColorSpec(),
        label = "ConveyStateHost.color",
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (targetContentColor == Color.Unspecified) Color.White else targetContentColor,
        animationSpec = morphSpec.toColorSpec(),
        label = "ConveyStateHost.contentColor",
    )

    val morphShape = rememberAnimatedMorphShape(targetShape, morphSpec)

    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth?.value ?: -1f,
        animationSpec = if (targetWidth != null) morphSpec else snap(),
        label = "ConveyStateHost.width",
    )

    val animatedHeight by animateFloatAsState(
        targetValue = targetHeight?.value ?: -1f,
        animationSpec = if (targetHeight != null) morphSpec else snap(),
        label = "ConveyStateHost.height",
    )

    val scope = remember(grammar) { ConveyStateScope(grammar, animatedContentColor) }

    val sizeModifier = modifier.let { m ->
        when {
            targetWidth != null && targetHeight != null ->
                m.then(Modifier.size(animatedWidth.dp, animatedHeight.dp))
            targetWidth != null ->
                m.then(Modifier.width(animatedWidth.dp))
            targetHeight != null ->
                m.then(Modifier.height(animatedHeight.dp))
            else -> m
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = sizeModifier
            .drawBehind { drawRect(animatedColor) }
            .clip(morphShape),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides animatedContentColor) {
            scope.content(state)
        }
    }
}

/**
 * Scope available inside [ConveyStateHost]'s content lambda.
 *
 * Provides access to the current animated content color and the grammar,
 * so content can use grammar-driven animations that stay in sync with the host.
 */
@Stable
class ConveyStateScope(
    val grammar: ConveyGrammar,
    val contentColor: Color,
) {
    /** Animate a float using a grammar-declared meaning. */
    @Composable
    fun animateFloat(target: Float, meaning: String = "morph"): State<Float> =
        animateFloatAsState(
            targetValue = target,
            animationSpec = grammar[meaning],
            label = "ConveyStateScope.float[$meaning]",
        )
}

// ── Private utilities ────────────────────────────────────────────────────────

@Composable
private fun Modifier.width(dp: Float): Modifier =
    this.then(Modifier.then(object : LayoutModifier {
        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            val width = dp.dp.roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    }))

@Composable
private fun Modifier.height(dp: Float): Modifier =
    this.then(Modifier.then(object : LayoutModifier {
        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            val height = dp.dp.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
            val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
            return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    }))

private fun AnimationSpec<Float>.toColorSpec(): AnimationSpec<Color> = when (this) {
    is SpringSpec<Float> -> spring(dampingRatio = dampingRatio, stiffness = stiffness)
    is TweenSpec<Float> -> tween(durationMillis = durationMillis, easing = easing)
    is SnapSpec<Float> -> snap()
    else -> spring()
}

// ── Standard multi-state composables built on ConveyStateHost ────────────────

private enum class SubmitButtonState { Idle, Loading, Success, Error }

/**
 * A button that morphs through idle → loading → success → idle.
 *
 * This is the canonical demonstration of [ConveyStateHost]: one element that IS
 * a button, then IS a spinner, then IS a checkmark. It never spawns a sibling.
 * The transformation shows the user what their action produced.
 *
 * The user does not see "button disappeared, spinner appeared."
 * The user sees their action acknowledged and fulfilled by the thing they touched.
 * That is Conveyance.
 */
@Composable
fun ConveySubmitButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isSuccess: Boolean,
    isError: Boolean = false,
    idleColor: Color,
    idleShape: Shape = ConveyShape.Large,
    collapsedShape: Shape = ConveyShape.Circle,
    collapsedSize: Dp = 52.dp,
    idleWidth: Dp = 160.dp,
    idleHeight: Dp = 52.dp,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    idleContent: @Composable () -> Unit,
    loadingContent: @Composable () -> Unit,
    successContent: @Composable () -> Unit,
    errorContent: @Composable () -> Unit = loadingContent,
    modifier: Modifier = Modifier,
) {
    val state = when {
        isError -> SubmitButtonState.Error
        isSuccess -> SubmitButtonState.Success
        isLoading -> SubmitButtonState.Loading
        else -> SubmitButtonState.Idle
    }

    val collapsed = state != SubmitButtonState.Idle
    val isInteractable = state == SubmitButtonState.Idle

    ConveyStateHost(
        state = state,
        targetShape = if (collapsed) collapsedShape else idleShape,
        targetColor = when (state) {
            SubmitButtonState.Error -> Color(0xFFFF4D6A)
            else -> idleColor
        },
        targetWidth = if (collapsed) collapsedSize else idleWidth,
        targetHeight = if (collapsed) collapsedSize else idleHeight,
        grammar = grammar,
        modifier = modifier
            .clickableIf(isInteractable, onClick = onClick),
    ) { s ->
        when (s) {
            SubmitButtonState.Idle -> idleContent()
            SubmitButtonState.Loading -> loadingContent()
            SubmitButtonState.Success -> successContent()
            SubmitButtonState.Error -> errorContent()
        }
    }
}

private fun Modifier.clickableIf(condition: Boolean, onClick: () -> Unit): Modifier =
    if (condition) this.then(Modifier.clickable(onClick = onClick)) else this
