package compose.conveyance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*
import kotlin.math.*

/**
 * A composable that maintains persistent visual identity across state changes.
 *
 * The core difference from AnimatedContent: there is no "old" content and "new" content.
 * There is ONE element that IS different things at different times. The transformation between
 * states is not decorative — it IS the communication. The user sees the same thing becoming
 * something else, which tells them those things are related.
 *
 * A FAB expanding into a sheet is not a FAB disappearing and a sheet appearing.
 * It is ONE element demonstrating its full range. The morph shows the relationship.
 *
 * ```kotlin
 * var expanded by remember { mutableStateOf(false) }
 *
 * ConveyMorph(
 *     state = if (expanded) MorphState.Expanded else MorphState.Collapsed,
 *     color = ConveyColor.Primary,
 *     shape = if (expanded) ConveyShape.ExtraLarge else ConveyShape.Circle,
 *     modifier = Modifier
 *         .size(if (expanded) 320.dp else 56.dp)
 *         .clickable { expanded = !expanded }
 * ) {
 *     if (expanded) ExpandedContent() else CollapsedContent()
 * }
 * ```
 *
 * The meaning passed to the grammar MUST be "morph" or a declared alias. The system
 * refuses to animate a morph with a "navigate" spec — they mean different things.
 *
 * @param state Any value. When it changes, the morph triggers.
 * @param shape The current target shape. Interpolated from the previous shape.
 * @param color The current target container color. Interpolated from the previous color.
 * @param contentColor The current target content color.
 * @param meaning Which grammar entry drives this morph. Defaults to "morph".
 */
@Composable
fun ConveyMorph(
    state: Any,
    shape: Shape,
    color: Color,
    contentColor: Color = Color.Unspecified,
    meaning: String = "morph",
    modifier: Modifier = Modifier,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    content: @Composable BoxScope.() -> Unit,
) {
    val spec = grammar[meaning]

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = spec.toColorSpec(),
        label = "ConveyMorph.color[$meaning]",
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (contentColor == Color.Unspecified) color else contentColor,
        animationSpec = spec.toColorSpec(),
        label = "ConveyMorph.contentColor[$meaning]",
    )

    val morphShape = rememberAnimatedMorphShape(shape, spec)

    Box(
        modifier = modifier
            .drawBehind { drawRect(animatedColor) }
            .clip(morphShape),
        content = {
            CompositionLocalProvider(LocalContentColor provides animatedContentColor) {
                content()
            }
        },
    )
}

/**
 * A shape that interpolates between two [Shape] instances via a progress float.
 *
 * This is the low-level primitive. [ConveyMorph] uses this internally.
 * Use directly when you need explicit control over morph progress — for instance,
 * when the morph is driven by a gesture rather than a state toggle.
 *
 * @param progress 0f = fully fromShape, 1f = fully toShape
 */
@Composable
fun rememberAnimatedMorphShape(
    targetShape: Shape,
    spec: AnimationSpec<Float> = LocalConveyGrammar.current["morph"],
): Shape {
    var fromShape by remember { mutableStateOf(targetShape) }
    var toShape by remember { mutableStateOf(targetShape) }

    val progress = remember { Animatable(1f) }

    LaunchedEffect(targetShape) {
        fromShape = InterpolatedShape(fromShape, toShape, progress.value)
        toShape = targetShape
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = spec)
    }

    return remember(fromShape, toShape, progress.value) {
        InterpolatedShape(fromShape, toShape, progress.value)
    }
}

/**
 * Morphs between two shapes by interpolating their outlines point by point.
 *
 * This is intentionally simple: both shapes are sampled at [sampleCount] points,
 * and corresponding points are linearly interpolated. This works well for convex shapes
 * and gives correct results for all [ConveyShape] tokens. For complex concave paths,
 * use the Compose Graphics Path Morphing API directly.
 */
internal class InterpolatedShape(
    private val from: Shape,
    private val to: Shape,
    private val progress: Float,
    private val sampleCount: Int = 64,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (progress <= 0f) return from.createOutline(size, layoutDirection, density)
        if (progress >= 1f) return to.createOutline(size, layoutDirection, density)

        val fromPath = Path().also { p ->
            (from.createOutline(size, layoutDirection, density) as? Outline.Generic)
                ?.path?.let { p.addPath(it) }
                ?: run {
                    val outline = from.createOutline(size, layoutDirection, density)
                    when (outline) {
                        is Outline.Rectangle -> p.addRect(outline.rect)
                        is Outline.Rounded -> p.addRoundRect(outline.roundRect)
                        is Outline.Generic -> p.addPath(outline.path)
                    }
                }
        }

        val toPath = Path().also { p ->
            val outline = to.createOutline(size, layoutDirection, density)
            when (outline) {
                is Outline.Rectangle -> p.addRect(outline.rect)
                is Outline.Rounded -> p.addRoundRect(outline.roundRect)
                is Outline.Generic -> p.addPath(outline.path)
            }
        }

        return Outline.Generic(lerpPaths(fromPath, toPath, progress, sampleCount))
    }
}

/**
 * Interpolates between two paths by resampling each at [sampleCount] evenly-spaced points.
 */
internal fun lerpPaths(from: Path, to: Path, t: Float, sampleCount: Int): Path {
    val fromMeasure = PathMeasure().apply { setPath(from, false) }
    val toMeasure = PathMeasure().apply { setPath(to, false) }

    val result = Path()
    var first = true

    for (i in 0..sampleCount) {
        val fraction = i.toFloat() / sampleCount
        val fromPos = fromMeasure.getPosition(fromMeasure.length * fraction)
        val toPos = toMeasure.getPosition(toMeasure.length * fraction)

        val x = lerp(fromPos.x, toPos.x, t)
        val y = lerp(fromPos.y, toPos.y, t)

        if (first) { result.moveTo(x, y); first = false }
        else result.lineTo(x, y)
    }

    result.close()
    return result
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

// ── Type extensions ──────────────────────────────────────────────────────────

private fun AnimationSpec<Float>.toColorSpec(): AnimationSpec<Color> = when (this) {
    is SpringSpec<Float> -> spring(dampingRatio = dampingRatio, stiffness = stiffness)
    is TweenSpec<Float> -> tween(durationMillis = durationMillis, easing = easing)
    is SnapSpec<Float> -> snap()
    else -> spring()
}

// ── Scope ────────────────────────────────────────────────────────────────────

typealias BoxScope = androidx.compose.foundation.layout.BoxScope
