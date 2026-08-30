package compose.conveyance

import compose.conveyance.internal.ConveyNounData

/**
 * The physical properties [ConveyNounLexicon] resolves a noun to: whether the simulation should
 * treat it as a living, self-propelled actor ([ConveyNounAnimacy]) and whether it should be
 * treated as a single discrete body or a substance ([ConveyNounCountability]). Together these
 * pick the physical-animation strategy in `docs/Procedural Animation of Subject-Verb-Object
 * Typography.md` (§"Noun Ontology and Physical Affordances"): an animate noun gets the gait-wobble
 * approximation of [ConveySvoScene]'s force simulator, and a mass noun gets its soft-body wobble
 * on contact, rather than plain rigid-body sliding.
 */
enum class ConveyNounAnimacy { Animate, Inanimate }

/** Count nouns are rigid bodies. Mass nouns are substances — see [ConveyNounAnimacy]'s doc comment. */
enum class ConveyNounCountability { Count, Mass }

data class ConveyNounProperties(
    val animacy: ConveyNounAnimacy,
    val countability: ConveyNounCountability,
)

/**
 * The WordNet-3.0-backed noun classifier described in `docs/Procedural Animation of
 * Subject-Verb-Object Typography.md` (§"WordNet and Ontological Classification"): real
 * `index.noun`/`data.noun`/`noun.exc` data, not a hand-curated word list, following exactly the
 * architecture of [ConveyVerbLexicon] (same lemmatization-then-sense-resolution shape, same
 * Simplified Lesk disambiguation on disagreement).
 *
 * Two properties are extracted per resolved WordNet sense:
 * 1. **Animacy** — computed offline (see the generation pipeline documented in the "Implementation
 *    status" section of the SVO blueprint doc) via hypernym-chain traversal from every noun
 *    synset up to WordNet's own `person.n.01` (offset 7846) or `animal.n.01` (offset 15388)
 *    synsets. A synset is [ConveyNounAnimacy.Animate] iff that traversal reaches either root.
 *    This is real WordNet hypernym data, just walked once at data-generation time rather than at
 *    Kotlin runtime — the full hypernym pointer graph (an extra ~150,000 edges) isn't worth
 *    shipping in the runtime blob when the only use of it is this one boolean per synset.
 *    Known consequence, mirroring how [ConveyVerbClass]'s own doc comment documents VerbNet's
 *    "yell" judgment call: WordNet's own hierarchy puts some colloquially-animate nouns (e.g.
 *    "robot", "zombie") under `artifact`/`whole` rather than `animal`, so they resolve
 *    [ConveyNounAnimacy.Inanimate] here — a real WordNet judgment, not a bug in this classifier.
 * 2. **Countability** — [ConveyNounCountability.Mass] iff the resolved sense's own WordNet
 *    lexicographer file is `noun.substance` (water, sand, gold, jelly...), else
 *    [ConveyNounCountability.Count]. This is a real, documented approximation: WordNet has no
 *    dedicated mass/count marker field for nouns (unlike its adjective position markers), so
 *    `noun.substance` domain membership is the closest real signal WordNet's own data exposes —
 *    it will not flag less prototypical mass nouns from other domains (e.g. "furniture",
 *    "information") as [ConveyNounCountability.Mass].
 */
object ConveyNounLexicon {

    private data class Synset(
        val domain: Int,
        val animate: Boolean,
        val mass: Boolean,
        val gloss: String,
    )

    private class ParsedData(
        val synsets: Map<Int, Synset>,
        val lemmaOffsets: Map<String, List<Int>>,
        val exceptions: Map<String, String>,
    )

    private val data: ParsedData by lazy(LazyThreadSafetyMode.NONE) { parse(ConveyNounData.blob) }

    private fun parse(blob: String): ParsedData {
        val synsets = HashMap<Int, Synset>()
        val lemmaOffsets = HashMap<String, List<Int>>()
        val exceptions = HashMap<String, String>()

        var section = 0
        for (line in blob.split('\n')) {
            if (line.startsWith("##")) {
                section = when (line) {
                    "##SYN" -> 1
                    "##LEM" -> 2
                    "##EXC" -> 3
                    else -> section
                }
                continue
            }
            if (line.isEmpty()) continue
            when (section) {
                1 -> { // offset \t domainCode \t animate(0/1) \t mass(0/1) \t gloss
                    val a = line.indexOf('\t')
                    val b = line.indexOf('\t', a + 1)
                    val c = line.indexOf('\t', b + 1)
                    val d = line.indexOf('\t', c + 1)
                    val offset = line.substring(0, a).toInt()
                    val domainCode = line.substring(a + 1, b).toInt()
                    val animate = line[c - 1] == '1'
                    val mass = line[d - 1] == '1'
                    val gloss = line.substring(d + 1)
                    synsets[offset] = Synset(domainCode, animate, mass, gloss)
                }
                2 -> { // lemma \t offset,offset,...
                    val tab = line.indexOf('\t')
                    val lemma = line.substring(0, tab)
                    lemmaOffsets[lemma] = line.substring(tab + 1).split(',').map { it.toInt() }
                }
                3 -> { // inflected \t base
                    val tab = line.indexOf('\t')
                    exceptions[line.substring(0, tab)] = line.substring(tab + 1)
                }
            }
        }
        return ParsedData(synsets, lemmaOffsets, exceptions)
    }

    /** WordNet's own noun detachment rules (`morph.c`'s noun `sufx`/`addr` tables), in order. */
    private val DETACHMENT_RULES = listOf(
        "ies" to "y", "ches" to "ch", "shes" to "sh", "xes" to "x", "zes" to "z",
        "ses" to "s", "men" to "man", "s" to "",
    )

    /**
     * Resolves [word] to its WordNet noun base form, exactly mirroring
     * [ConveyVerbLexicon.lemmatize]'s shape: the real `noun.exc` irregular-plural table first
     * (e.g. "mice" -> "mouse", "children" -> "child"), then WordNet's own regular detachment
     * rules checked against the real lemma index, never applied blindly. Null if no base form has
     * a WordNet noun entry at all.
     */
    fun lemmatize(word: String): String? {
        val lower = word.lowercase()
        if (data.lemmaOffsets.containsKey(lower)) return lower
        data.exceptions[lower]?.let { base -> if (data.lemmaOffsets.containsKey(base)) return base }
        for ((suffix, replacement) in DETACHMENT_RULES) {
            if (lower.length > suffix.length + 1 && lower.endsWith(suffix)) {
                val candidate = lower.removeSuffix(suffix) + replacement
                if (data.lemmaOffsets.containsKey(candidate)) return candidate
            }
        }
        return null
    }

    /**
     * Resolves [word] to its [ConveyNounProperties], or null if [word] has no WordNet noun entry
     * at all (callers fall back to a default — see [ConveySvoScene]). Builds one candidate
     * per WordNet sense, in sense-frequency order; if every sense agrees on both animacy and
     * countability, returns that unambiguously. On disagreement, with non-blank [context],
     * disambiguates via the same Simplified Lesk gloss-overlap scoring
     * [ConveyVerbLexicon.classify] uses (excluding the query word's own inflected forms). Without
     * a clear winner, falls back to the noun's primary (most frequent) WordNet sense.
     */
    fun classify(word: String, context: String = ""): ConveyNounProperties? {
        val lemma = lemmatize(word) ?: return null
        val offsets = data.lemmaOffsets.getValue(lemma)
        val offset = resolveOffset(lemma, offsets, context) ?: return null
        val synset = data.synsets[offset] ?: return null
        return ConveyNounProperties(
            animacy = if (synset.animate) ConveyNounAnimacy.Animate else ConveyNounAnimacy.Inanimate,
            countability = if (synset.mass) ConveyNounCountability.Mass else ConveyNounCountability.Count,
        )
    }

    private fun resolveOffset(lemma: String, offsets: List<Int>, context: String): Int? {
        val perOffsetProps = offsets.map { offset -> data.synsets[offset]?.let { it.animate to it.mass } }
        if (perOffsetProps.distinct().size == 1) return offsets[0]
        if (context.isBlank()) return offsets[0]

        val exclude = selfForms(lemma) + lemma
        val contextTokens = tokenize(context, exclude)
        if (contextTokens.isEmpty()) return offsets[0]

        var bestIndex = 0
        var bestScore = 0
        for (i in offsets.indices) {
            val gloss = data.synsets[offsets[i]]?.gloss ?: continue
            val score = tokenize(gloss, exclude).intersect(contextTokens).size
            if (score > bestScore) {
                bestScore = score
                bestIndex = i
            }
        }
        return offsets[bestIndex]
    }

    private fun selfForms(lemma: String): Set<String> {
        val forms = mutableSetOf(lemma)
        for ((suffix, replacement) in DETACHMENT_RULES) {
            if (lemma.endsWith(replacement)) {
                forms += if (replacement.isEmpty()) lemma + suffix else lemma.removeSuffix(replacement) + suffix
            }
        }
        for ((inflected, base) in data.exceptions) {
            if (base == lemma) forms += inflected
        }
        return forms
    }

    private val STOPWORDS = setOf(
        "a", "an", "the", "of", "to", "in", "on", "at", "for", "with", "and", "or", "is", "are",
        "was", "were", "be", "been", "being", "by", "as", "it", "its", "that", "this", "from",
        "into", "than", "then", "so", "not", "no", "do", "does", "did", "has", "have", "had",
    )

    private val WORD_PATTERN = Regex("[A-Za-z]+(?:'[A-Za-z]+)*")

    private fun tokenize(text: String, exclude: Set<String>): Set<String> =
        WORD_PATTERN.findAll(text.lowercase())
            .map { it.value }
            .filter { it.length > 2 && it !in STOPWORDS && it !in exclude }
            .toSet()
}
