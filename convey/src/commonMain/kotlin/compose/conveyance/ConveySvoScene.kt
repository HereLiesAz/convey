package compose.conveyance

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import compose.conveyance.foundation.ConveyForceDynamics
import compose.conveyance.foundation.ConveyGaitOscillator
import compose.conveyance.foundation.ConveyRigidBody
import compose.conveyance.foundation.ConveySpringMassBody
import compose.conveyance.foundation.Vec2
import kotlinx.coroutines.isActive

/**
 * A subject/verb/object triple extracted from a sentence by [parseSvoHeuristic] — see that
 * function's own doc comment for exactly how, and for why it is explicitly *not* a real syntactic
 * parser.
 */
data class ConveySvoParts(val subject: String, val verb: String, val obj: String)

/**
 * Splits [sentence] into a subject/verb/object triple using a straightforward heuristic chunker,
 * **not a real syntactic parser** — building one is out of scope for this library (see the
 * "Implementation status" section of `docs/Procedural Animation of Subject-Verb-Object
 * Typography.md`). The rule: the first word (scanning from index 1, so a lone leading word never
 * mistakenly claims to be its own verb) that [ConveyVerbLexicon] can actually classify — i.e. has
 * a real WordNet verb entry — is the verb; everything before it is the subject noun phrase,
 * everything after it is the object noun phrase. Since English noun phrases are head-final in
 * their common form ("the fast cheetah"), each phrase's *last* word is taken as its head noun —
 * this deliberately ignores modifiers and articles rather than attempting to parse them.
 *
 * Returns null when [sentence] has fewer than 3 real words, or when no plausible object phrase
 * remains after a fallback verb-position guess (second word) — callers should fall back to
 * ordinary kinetic text (see [ConveySvoScene]) rather than force a shape that doesn't fit.
 */
fun parseSvoHeuristic(sentence: String): ConveySvoParts? {
    val words = sentence.split(Regex("\\s+"))
        .map { it.trim { c -> !c.isLetter() } }
        .filter { it.isNotEmpty() }
    if (words.size < 3) return null

    var verbIndex = -1
    for (i in 1 until words.size - 1) {
        if (ConveyVerbLexicon.classify(words[i], sentence) != ConveyVerbClass.Unclassified) {
            verbIndex = i
            break
        }
    }
    if (verbIndex == -1) verbIndex = 1
    if (verbIndex >= words.size - 1) return null

    val subject = words.subList(0, verbIndex).lastOrNull() ?: return null
    val obj = words.subList(verbIndex + 1, words.size).lastOrNull() ?: return null
    return ConveySvoParts(subject = subject, verb = words[verbIndex], obj = obj)
}

/**
 * The top-level orchestrating composable synthesizing every real piece built for
 * `docs/Procedural Animation of Subject-Verb-Object Typography.md`: [parseSvoHeuristic] splits
 * the sentence, [ConveyNounLexicon] classifies the subject and object's animacy and countability,
 * [ConveyVerbLexicon]/[ConveyVerbClass.toEventTimeline] classify the verb onto a physical event
 * timeline, and [compose.conveyance.foundation.ConveyForceDynamics]'s pure-Kotlin simulator drives
 * the subject word toward the object word accordingly, entirely via [Animatable]s read in the
 * draw phase — the same reactive pattern [Modifier.conveyRipple] already uses, never a
 * non-reactive `var`:
 *
 * - A verb whose [ConveyVerbEventTimeline.approaches] is true translates the subject toward the
 *   object over time via [ConveyRigidBody] under [ConveyForceDynamics.attraction].
 * - [ConveyVerbEventTimeline.contactAtEnd] stops the subject on actual collision and fires a
 *   [ConveySpringMassBody] impulse on the object, read back as a squash/stretch scale — real
 *   variable-font axis interpolation per the report's own §"Variable Fonts and Parametric Axes"
 *   isn't reliably available across every Compose Multiplatform target, so this is scale-transform
 *   squash-and-stretch instead, deliberately, everywhere.
 * - An [ConveyNounAnimacy.Animate] subject additionally receives [ConveyGaitOscillator]'s bob/tilt
 *   approximation, keyed to its own translation speed, standing in for true multi-bone IK (see
 *   [ConveySpringMassBody]'s own doc comment, and the SVO doc's "Implementation status", for why
 *   real IK solving is out of scope).
 * - A [ConveyNounCountability.Mass] object keeps wobbling after contact (the spring's own natural
 *   decay) rather than settling instantly — the "soft-body wobble" a count-noun object doesn't get.
 *
 * Word-as-image morphing (diffusion-guided glyph warping) is explicitly **not** attempted here —
 * see the SVO doc's "Implementation status" for why. The subject and object render as plain,
 * unmorphed kinetic text; only their *position, scale, and rotation* are driven by the simulation.
 *
 * A sentence [parseSvoHeuristic] can't confidently split (fewer than 3 words, or no object
 * remains after a verb guess) falls back to [ConveyKineticSentence] — ordinary per-word kinetic
 * text — rather than forcing a scene that has nothing real to simulate.
 *
 * @param sceneWidth The simulation's horizontal extent — also the subject's target travel distance.
 * @param sceneHeight The simulation's vertical extent, reserved for the verb's own label and gait bob.
 */
@Composable
fun ConveySvoScene(
    sentence: String,
    style: TextStyle = TextStyle.Default,
    sceneWidth: Dp = 320.dp,
    sceneHeight: Dp = 96.dp,
    modifier: Modifier = Modifier,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
) {
    val parts = remember(sentence) { parseSvoHeuristic(sentence) }
    if (parts == null) {
        ConveyKineticSentence(text = sentence, grammar = grammar, style = style, modifier = modifier)
        return
    }

    val subjectProps = remember(parts, sentence) { ConveyNounLexicon.classify(parts.subject, sentence) }
    val objectProps = remember(parts, sentence) { ConveyNounLexicon.classify(parts.obj, sentence) }
    val verbClass = remember(parts, sentence) { ConveyVerbLexicon.classify(parts.verb, sentence) }
    val timeline = remember(verbClass) { verbClass.toEventTimeline() }

    var subjectSize by remember { mutableStateOf(IntSize.Zero) }
    var objectSize by remember { mutableStateOf(IntSize.Zero) }

    val subjectX = remember { Animatable(0f) }
    val subjectY = remember { Animatable(0f) }
    val objectSquash = remember { Animatable(0f) }
    val gaitBob = remember { Animatable(0f) }
    val gaitTilt = remember { Animatable(0f) }

    LaunchedEffect(parts, sentence, sceneWidth) {
        val separationPx = sceneWidth.value * 2.5f
        val rigid = ConveyRigidBody(initialPosition = Vec2.Zero, mass = 1f, damping = 0.92f)
        val gait = ConveyGaitOscillator()
        // Mass nouns keep visibly oscillating after contact (underdamped); count nouns settle in
        // one squash with no bounce (near-critically damped) -- the "soft-body wobble a count-noun
        // object doesn't get" this composable's own doc comment promises.
        val spring = ConveySpringMassBody(
            dampingRatio = if (objectProps?.countability == ConveyNounCountability.Mass) 0.12f else 0.9f,
        )
        var hasContacted = false
        var lastFrameNanos = -1L

        while (isActive) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos < 0L) {
                lastFrameNanos = frameNanos
                continue
            }
            val dt = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrameNanos = frameNanos

            val objectPos = Vec2(separationPx, 0f)
            val combinedRadius = ((subjectSize.width + objectSize.width) / 2f).coerceAtLeast(48f)

            if (timeline.approaches && !hasContacted) {
                val force = ConveyForceDynamics.attraction(rigid.position, objectPos, strength = 1400f)
                rigid.applyForce(force, dt)
                val distanceToObject = (objectPos - rigid.position).length
                if (timeline.contactAtEnd && ConveyForceDynamics.hasCollided(rigid.position, objectPos, combinedRadius)) {
                    hasContacted = true
                    val impactSpeed = rigid.velocity.length
                    rigid.stop()
                    spring.impulse(impactSpeed * 0.012f)
                } else if (timeline.continuousNoContact && distanceToObject <= combinedRadius * 1.15f) {
                    rigid.stop()
                }
            }

            spring.step(dt)
            gait.step(dt, rigid.velocity.length)

            subjectX.snapTo(rigid.position.x)
            subjectY.snapTo(rigid.position.y)
            objectSquash.snapTo(spring.displacement)
            gaitBob.snapTo(gait.bobPx(rigid.velocity.length))
            gaitTilt.snapTo(gait.tiltDegrees(rigid.velocity.length))
        }
    }

    Box(modifier = modifier.width(sceneWidth).height(sceneHeight)) {
        Text(
            text = parts.obj,
            style = style,
            modifier = Modifier
                .onSizeChanged { objectSize = it }
                .graphicsLayer {
                    translationX = sceneWidth.toPx() * 2.5f
                    val squash = objectSquash.value
                    scaleX = (1f + squash * 0.35f).coerceIn(0.6f, 1.6f)
                    scaleY = (1f - squash * 0.5f).coerceIn(0.4f, 1.6f)
                },
        )
        Text(
            text = parts.subject,
            style = style,
            modifier = Modifier
                .onSizeChanged { subjectSize = it }
                .graphicsLayer {
                    translationX = subjectX.value
                    translationY = subjectY.value - if (subjectProps?.animacy == ConveyNounAnimacy.Animate) gaitBob.value else 0f
                    rotationZ = if (subjectProps?.animacy == ConveyNounAnimacy.Animate) gaitTilt.value else 0f
                },
        )
        ConveyKineticText(
            text = parts.verb,
            idle = verbClass.toConveyLife(),
            grammar = grammar,
            style = style,
        )
    }
}
