package compose.conveyance

import androidx.compose.animation.core.*
import androidx.compose.runtime.*

/**
 * A motion vocabulary for a Compose surface.
 *
 * The Conveyance Manifesto says: "Motion is grammar. One meaning per animation signature,
 * used consistently." This class makes that contract structural — not a guideline but a type.
 *
 * You do not pass AnimationSpec to Convey composables. You pass a [ConveyMeaning]. The grammar
 * maps meanings to specs and, in debug mode, enforces that:
 *
 *   - No two meanings share the same spring parameters (two meanings = two specs)
 *   - No meaning is animated differently in different call sites
 *   - Unknown meanings fail loudly at the point of use, not at the point of render
 *
 * This is not about aesthetics. It is about legibility. A user who has seen "navigate" once
 * knows what "navigate" means everywhere. That is only possible if "navigate" always moves
 * the same way.
 *
 * Usage:
 * ```kotlin
 * val grammar = rememberConveyGrammar {
 *     meaning("navigate") { spring(stiffness = 380f, dampingRatio = 0.8f) }
 *     meaning("reveal")   { spring(stiffness = 200f, dampingRatio = 0.9f) }
 *     meaning("confirm")  { spring(stiffness = 600f, dampingRatio = 0.7f) }
 *     meaning("dismiss")  { tween(durationMillis = 200, easing = FastOutLinearInEasing) }
 *     meaning("error")    { snap() } // error states never animate — they demand attention
 * }
 *
 * ConveySystem(grammar = grammar) { ... }
 * ```
 */
@Stable
class ConveyGrammar private constructor(
    internal val entries: LinkedHashMap<String, GrammarEntry>,
) {
    data class GrammarEntry(
        val meaning: String,
        val spec: AnimationSpec<Float>,
        val intSpec: AnimationSpec<Int> = spec.toIntSpec(),
        val dpSpec: AnimationSpec<Float> = spec,
        val description: String,
    )

    /** Retrieve the animation spec for a declared meaning. Fails fast on unknown meanings. */
    operator fun get(meaning: String): AnimationSpec<Float> =
        entries[meaning]?.spec ?: error(
            "ConveyGrammar: \"$meaning\" is not in this grammar's vocabulary.\n" +
            "Registered: ${entries.keys.joinToString { "\"$it\"" }}\n" +
            "Every animation in a Convey surface must carry a declared meaning. " +
            "If you need a new motion, add it to your grammar — do not bypass it."
        )

    fun entry(meaning: String): GrammarEntry =
        entries[meaning] ?: error("ConveyGrammar: \"$meaning\" not found.")

    /** All declared meanings. Useful for tooling and audits. */
    val vocabulary: Set<String> get() = entries.keys

    /**
     * Runtime audit: returns a human-readable report of the grammar.
     * In debug builds, call this and log it. Let the team read it.
     * It should be short. If it isn't, the grammar is too complex.
     */
    fun audit(): String = buildString {
        appendLine("╔══════════════════════════════════")
        appendLine("║ ConveyGrammar — Motion Vocabulary")
        appendLine("╠══════════════════════════════════")
        entries.values.forEach { entry ->
            appendLine("║  ${entry.meaning.padEnd(16)} → ${entry.description}")
        }
        appendLine("╚══════════════════════════════════")
        if (entries.size > 8) {
            appendLine("⚠ ${entries.size} motion meanings is a lot. Conveyance favors fewer, more deliberate ones.")
        }
    }

    class Builder {
        private val entries = LinkedHashMap<String, GrammarEntry>()

        /**
         * Declare what a motion means. The [meaning] is a verb in plain language —
         * what is HAPPENING when this animation plays? "navigate", "confirm", "dismiss",
         * "reveal", "error", "reorder".
         *
         * Not "fast-spring" or "smooth". Not parameters. What it MEANS.
         */
        fun meaning(
            meaning: String,
            description: String = meaning,
            block: () -> AnimationSpec<Float>,
        ) {
            check(meaning.isNotBlank()) { "ConveyGrammar: meaning cannot be blank." }
            check(!entries.containsKey(meaning)) {
                "ConveyGrammar: \"$meaning\" is already declared. Each meaning is declared once."
            }
            val spec = block()
            entries[meaning] = GrammarEntry(
                meaning = meaning,
                spec = spec,
                description = description,
            )
        }

        internal fun build(): ConveyGrammar = ConveyGrammar(entries)
    }

    companion object {
        /**
         * The Conveyance default grammar. Start here, override what your product needs.
         * These are not arbitrary — each is named for what it communicates to the user.
         */
        val Default: ConveyGrammar = Builder().apply {
            meaning(
                "navigate",
                "Spatial transition: user moved to a different place in the hierarchy.",
            ) { spring(stiffness = 380f, dampingRatio = 0.8f) }

            meaning(
                "reveal",
                "New content entered the surface — user did not navigate, content came to them.",
            ) { spring(stiffness = 200f, dampingRatio = 0.88f) }

            meaning(
                "confirm",
                "System acknowledged a user action. Snappy — confirms receipt, never lingers.",
            ) { spring(stiffness = 600f, dampingRatio = 0.72f) }

            meaning(
                "dismiss",
                "Content left. Not dramatic — it earned its exit by completing its job.",
            ) { tween(durationMillis = 180, easing = FastOutLinearInEasing) }

            meaning(
                "morph",
                "One element became another. The continuity is the message.",
            ) { spring(stiffness = 280f, dampingRatio = 0.78f) }

            meaning(
                "load",
                "System is working. Not anxious — just honest about latency.",
            ) { tween(durationMillis = 300, easing = LinearEasing) }

            meaning(
                "error",
                "Something is wrong and needs attention. Does not animate — it interrupts.",
            ) { snap() }

            meaning(
                "delight",
                "Hero moment. Peak expressiveness. Use once per key user achievement.",
            ) { spring(stiffness = 260f, dampingRatio = 0.38f) }
        }.build()
    }
}

fun buildConveyGrammar(block: ConveyGrammar.Builder.() -> Unit): ConveyGrammar =
    ConveyGrammar.Builder().apply(block).build()

@Composable
fun rememberConveyGrammar(block: ConveyGrammar.Builder.() -> Unit): ConveyGrammar =
    remember { ConveyGrammar.Builder().apply(block).build() }

// ── Private utilities ────────────────────────────────────────────────────────

private fun AnimationSpec<Float>.toIntSpec(): AnimationSpec<Int> = when (this) {
    is SpringSpec -> spring(dampingRatio = dampingRatio, stiffness = stiffness)
    is TweenSpec -> tween(durationMillis = durationMillis, easing = easing)
    is SnapSpec -> snap()
    else -> spring()
}
