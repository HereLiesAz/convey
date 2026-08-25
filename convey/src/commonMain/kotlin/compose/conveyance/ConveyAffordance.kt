package compose.conveyance

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Self-revealing interactivity.
 *
 * An affordance is a property of an object that reveals how it can be used.
 * A door handle affords pulling. A button affords pressing. In UI, we cannot rely on
 * physical form — we must signal affordance through behavior.
 *
 * The Manifesto says: "Design should teach users what to do simply by being used."
 * [ConveyAffordance] makes elements teach their own interactivity, once, without help text,
 * without onboarding overlays, without tooltips that appear on hover.
 *
 * The element moves. The movement demonstrates what happens when you interact.
 * Then it stops, because the user has learned. It does not repeat the lesson.
 *
 * This is the difference between compassionate design and patronizing design:
 * compassionate design teaches once and trusts. Patronizing design never stops explaining.
 *
 * Affordance types:
 *
 * [ConveyAffordance.None] — No self-revelation. Use for elements whose affordance is
 *   structurally obvious (e.g., a text field, a slider with visible thumb).
 *
 * [ConveyAffordance.PressHint] — On first composition, the element performs a subtle
 *   scale-down and recovery, demonstrating pressability. Plays once.
 *
 * [ConveyAffordance.SwipeHint] — On first composition, the element translates slightly
 *   in the swipe direction and recovers. Plays once.
 *
 * [ConveyAffordance.DragHint] — A gentle float that implies the element can be grabbed.
 *   Plays until the user first interacts, then never again.
 *
 * [ConveyAffordance.ExpandHint] — The element breathes slightly larger, implying it can
 *   be expanded. Plays once.
 */
sealed interface ConveyAffordance {
    data object None : ConveyAffordance
    data class PressHint(
        val scale: Float = 0.92f,
        val delay: Long = 400L,
        val meaning: String = "confirm",
    ) : ConveyAffordance
    data class SwipeHint(
        val directionDp: Float = 20f,
        val horizontal: Boolean = true,
        val delay: Long = 600L,
    ) : ConveyAffordance
    data class DragHint(
        val amplitudeDp: Float = 4f,
    ) : ConveyAffordance
    data class ExpandHint(
        val scale: Float = 1.06f,
        val delay: Long = 500L,
    ) : ConveyAffordance
}

/**
 * Applies a [ConveyAffordance] to a composable.
 *
 * The [key] parameter controls when the affordance replays. When [key] changes,
 * the element treats itself as new and may perform the hint again. This is useful
 * for persistent elements that take on new roles (e.g., a button that becomes
 * the primary action on a new step of a flow — it should demonstrate itself again).
 *
 * ```kotlin
 * FloatingActionButton(
 *     modifier = Modifier.conveyAffordance(
 *         ConveyAffordance.PressHint(delay = 800L)
 *     ),
 *     onClick = { openCompose() }
 * )
 * ```
 */
@Stable
fun Modifier.conveyAffordance(
    affordance: ConveyAffordance,
    key: Any = Unit,
    grammar: ConveyGrammar = ConveyGrammar.Default,
): Modifier = when (affordance) {
    ConveyAffordance.None -> this
    is ConveyAffordance.PressHint -> this.composed {
        val scale = remember { Animatable(1f) }
        val spec = grammar[affordance.meaning]

        LaunchedEffect(key) {
            kotlinx.coroutines.delay(affordance.delay)
            scale.animateTo(affordance.scale, animationSpec = spec)
            scale.animateTo(1f, animationSpec = spec)
        }

        scale(scale.value)
    }
    is ConveyAffordance.ExpandHint -> this.composed {
        val scale = remember { Animatable(1f) }

        LaunchedEffect(key) {
            kotlinx.coroutines.delay(affordance.delay)
            scale.animateTo(affordance.scale, animationSpec = spring(stiffness = 180f, dampingRatio = 0.5f))
            scale.animateTo(1f, animationSpec = spring(stiffness = 280f, dampingRatio = 0.7f))
        }

        scale(scale.value)
    }
    is ConveyAffordance.SwipeHint -> this.composed {
        val offset = remember { Animatable(0f) }

        LaunchedEffect(key) {
            kotlinx.coroutines.delay(affordance.delay)
            val spec = spring<Float>(stiffness = 200f, dampingRatio = 0.6f)
            offset.animateTo(affordance.directionDp, animationSpec = spec)
            offset.animateTo(0f, animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f))
        }

        if (affordance.horizontal) graphicsLayer { translationX = offset.value }
        else graphicsLayer { translationY = offset.value }
    }
    is ConveyAffordance.DragHint -> this.composed {
        var interacted by remember { mutableStateOf(false) }
        val offset = remember { Animatable(0f) }

        LaunchedEffect(key, interacted) {
            if (interacted) { offset.animateTo(0f); return@LaunchedEffect }
            while (!interacted) {
                offset.animateTo(
                    affordance.amplitudeDp,
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
                offset.animateTo(
                    -affordance.amplitudeDp,
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
            }
        }

        this
            .graphicsLayer { translationY = offset.value }
            .pointerInput(key) {
                detectTapGestures { interacted = true }
            }
    }
}

/**
 * Marks an element as having no interactive affordance — it is Ghost by nature.
 * This is not an empty modifier. This is an explicit declaration: "I know this element
 * does nothing, and that is intentional."
 *
 * In audit mode, elements without either [conveyAffordance] or [conveyInert] are flagged
 * as "affordance unknown" — the system cannot tell if you forgot or intended passivity.
 */
fun Modifier.conveyInert(reason: String = ""): Modifier = this
