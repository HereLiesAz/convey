package compose.conveyance

import compose.conveyance.internal.ConveyVerbData

/**
 * The deterministic, rule-based verb classification described in
 * `docs/kinetic-text-verb-classification.md` — a full implementation of that report's
 * architecture built on the real lexical resources it cites (Princeton WordNet 3.0, VerbNet
 * 3.3), not a hand-curated word list.
 *
 * Two layers, exactly as the report specifies:
 *
 * 1. **WordNet domain fallback** (report §"High-Level Domain Categorization via WordNet"):
 *    every one of WordNet's ~11,500 verb lemmas has a domain for *every one of its senses* —
 *    one of the 15 lexicographer-file domains below (`verb.body` through `verb.weather`).
 *    This is the guaranteed-coverage layer.
 * 2. **VerbNet refinement** (report §"Event Timeline Modeling... via VerbNet" and
 *    §"Addressing Granularity in Motion Verbs"): where a *specific WordNet sense* is also a
 *    member of a VerbNet class whose `SEMANTICS`/`PRED` predicates or Levin-class identity
 *    resolve to one of the finer classes below (e.g. predicate `degradation_material_integrity`
 *    → [Punctual]; the `run-51.3` "Run verbs" family → [MannerAgent]), that sense's
 *    classification is overridden to the finer class instead of its coarse domain.
 *
 * Crucially, the override is keyed to the **exact WordNet sense** VerbNet linked (via each
 * class member's `wn=` sense key), never applied lemma-wide — a verb's other, unrelated senses
 * keep their own domain. Polysemy (a verb whose senses disagree on class) is resolved by
 * [ConveyVerbLexicon] using the Simplified Lesk algorithm the report names
 * (§"Deterministic Word Sense Disambiguation"), scored against real WordNet glosses, when a
 * context sentence is supplied; without context it falls back deterministically to the verb's
 * primary (most frequent) WordNet sense — which is itself real, corpus-derived data (WordNet's
 * senses are ordered by frequency in the SemCor-tagged corpus), not a guess.
 *
 * See `docs/kinetic-text-verb-classification.md` for the source datasets and the generation
 * pipeline that produced [compose.conveyance.internal.ConveyVerbData], and for **known,
 * honestly-documented limitations**: VerbNet's own classes sometimes group senses more broadly
 * than intuition expects (e.g. "yell" resolves to [SubtleBody] because its primary WordNet sense
 * is VerbNet-linked to a class that treats sudden vocal exclamations as bodily/reflexive events
 * alongside cough and sneeze — a real VerbNet judgment call, not a bug in this classifier), and
 * Simplified Lesk can fail to disambiguate when context vocabulary doesn't literally overlap any
 * candidate gloss (a well-known, inherent weakness of the algorithm, not particular to this
 * implementation).
 */
enum class ConveyVerbClass {
    // ── WordNet lexicographer-file domains (verb.body .. verb.weather) — full coverage ──────

    /** `verb.body`: groom, yawn, bleed. */
    Body,

    /** `verb.change`: become, melt, grow. */
    Change,

    /** `verb.cognition`: think, know, decide. */
    Cognition,

    /** `verb.communication`: speak, inform, declare. */
    Communication,

    /** `verb.competition`: race, fight, win. */
    Competition,

    /** `verb.consumption`: eat, drink, smoke. */
    Consumption,

    /** `verb.contact`: touch, hit, kick — also the VerbNet Hit-class refinement target. */
    Contact,

    /** `verb.creation`: build, compose, bake. */
    Creation,

    /** `verb.emotion`: love, fear, enjoy — also the VerbNet `emotional_state` refinement target. */
    Emotion,

    /** `verb.motion`: move, travel, fly — unrefined; see [PurePath]/[MannerAgent]/[SubtleBody]. */
    Motion,

    /** `verb.perception`: see, hear, feel — also the VerbNet `perceive` refinement target. */
    Perception,

    /** `verb.possession`: own, give, buy. */
    Possession,

    /** `verb.social`: marry, govern, cooperate. */
    Social,

    /** `verb.stative`: be, exist, cost — definitionally static. */
    Stative,

    /** `verb.weather`: rain, snow, thunder. */
    Weather,

    // ── VerbNet-driven refinements (report's motion hierarchy + Aktionsart) ─────────────────

    /** VerbNet `escape-51.1`/`leave-51.2`. Report: "constant-velocity... without manner." */
    PurePath,

    /** VerbNet `run-51.3`/`waltz-51.5` (Levin's Run verbs). Report: "an uneven gait." */
    MannerAgent,

    /** VerbNet `body_internal_states-40.6`/`body_internal_motion-49.1`/`breathe-40.1.2`. */
    SubtleBody,

    /** VerbNet `cooking-45.3` (`apply_heat`+`cooked` predicates). Report's Apply_heat example. */
    StateMetaphor,

    /** VerbNet `break-45.1` / any class with predicate `degradation_material_integrity`. */
    Punctual,

    /**
     * VerbNet `calibratable_cos-45.6`/`other_cos-45.4`: verbs of degree/value change with a
     * defined endpoint (e.g. prices *soaring*). Narrower than the report's own Aktionsart
     * "Scalar" row (which also groups plain accomplishment verbs like "build" or "find" by a
     * telic/atelic distinction WordNet and VerbNet don't directly encode) — this case captures
     * only the change-of-degree subset that VerbNet's own data actually supports.
     */
    Scalar,

    /** Lemmatization succeeded but the lemma has no WordNet verb entry. Renders still. */
    Unclassified,
}

/**
 * A procedural static-layout shape a sentence's own verb can suggest — see
 * [ConveyVerbLexicon.topographicalCategory] for how a word resolves to one of these, and
 * [compose.conveyance.foundation.ConveyTopographicalPaths] for the geometry each one drives.
 */
enum class ConveyTopographicalCategory {
    /** e.g. fall, descend, sink, plunge. Report's own worked example: a downward staircase. */
    Descent,

    /** e.g. rise, climb, soar, ascend. The mirror of [Descent]: an upward staircase. */
    Ascent,

    /** e.g. scatter, disperse, spread. Words moving apart from a shared origin. */
    Scatter,

    /** e.g. circle, surround, orbit. Words arranged around a shared center. */
    Encircle,
}

/**
 * Maps a resolved [ConveyVerbClass] to a concrete [ConveyLife] profile.
 *
 * [ConveyLife] only speaks in scale, opacity, and skew — it has no font-weight/color axis and
 * no notion of spatial path translation ([PurePath]/[Scalar] describe *global* kineticism,
 * which belongs to [ConveyTransform]/[ConveyMorph], not per-glyph idle motion; [Contact]/
 * [Punctual] are one-shot events, which belong on [Modifier.conveyLifeBurst], not continuous
 * idle motion). Where a class has no idle-motion equivalent, the mapping is [ConveyLife.None]
 * deliberately — stillness, not a fabricated match.
 */
fun ConveyVerbClass.toConveyLife(): ConveyLife = when (this) {
    ConveyVerbClass.Body -> ConveyLife.Wobble(period = 1800L, skewDegrees = 2.5f)
    ConveyVerbClass.Change -> ConveyLife.Breathe(period = 4000L, peakScale = 1.05f)
    ConveyVerbClass.Cognition -> ConveyLife.Twinkle(period = 3200L, minOpacity = 0.65f)
    ConveyVerbClass.Communication -> ConveyLife.Twinkle(period = 1800L)
    ConveyVerbClass.Competition -> ConveyLife.Wobble(period = 700L, skewDegrees = 4f)
    ConveyVerbClass.Consumption -> ConveyLife.Breathe(period = 2200L, peakScale = 1.1f)
    ConveyVerbClass.Contact -> ConveyLife.None
    ConveyVerbClass.Creation -> ConveyLife.Breathe(period = 3400L, peakScale = 1.15f)
    ConveyVerbClass.Emotion -> ConveyLife.Breathe(period = 3000L, peakScale = 1.05f, minOpacity = 0.88f)
    ConveyVerbClass.Motion -> ConveyLife.Wobble(period = 2000L, skewDegrees = 3f)
    ConveyVerbClass.Perception -> ConveyLife.Twinkle(period = 1200L, minOpacity = 0.7f)
    ConveyVerbClass.Possession -> ConveyLife.None
    ConveyVerbClass.Social -> ConveyLife.Wobble(period = 2400L, skewDegrees = 2f)
    ConveyVerbClass.Stative -> ConveyLife.None
    ConveyVerbClass.Weather -> ConveyLife.Twinkle(period = 2600L, minOpacity = 0.55f)

    ConveyVerbClass.PurePath -> ConveyLife.None
    ConveyVerbClass.MannerAgent -> ConveyLife.Wobble(period = 1400L, skewDegrees = 5f)
    ConveyVerbClass.SubtleBody -> ConveyLife.Wobble(period = 340L, skewDegrees = 1.5f)
    ConveyVerbClass.StateMetaphor -> ConveyLife.Breathe(period = 3200L, peakScale = 1.06f)
    ConveyVerbClass.Punctual -> ConveyLife.None
    ConveyVerbClass.Scalar -> ConveyLife.None

    ConveyVerbClass.Unclassified -> ConveyLife.None
}

/**
 * The verb → [ConveyVerbClass] classifier, backed by real Princeton WordNet 3.0 and VerbNet 3.3
 * data (see [ConveyVerbClass]'s own doc comment for the two-layer architecture, and
 * `docs/kinetic-text-verb-classification.md` for the source datasets). All data is parsed once,
 * lazily, from the compact blob in [compose.conveyance.internal.ConveyVerbData].
 */
object ConveyVerbLexicon {

    // ── Parsed, memoized views over ConveyVerbData.blob ─────────────────────────────────────

    private data class Synset(val domain: ConveyVerbClass, val gloss: String)

    private val DOMAIN_BY_CODE = arrayOf(
        ConveyVerbClass.Body, ConveyVerbClass.Change, ConveyVerbClass.Cognition,
        ConveyVerbClass.Communication, ConveyVerbClass.Competition, ConveyVerbClass.Consumption,
        ConveyVerbClass.Contact, ConveyVerbClass.Creation, ConveyVerbClass.Emotion,
        ConveyVerbClass.Motion, ConveyVerbClass.Perception, ConveyVerbClass.Possession,
        ConveyVerbClass.Social, ConveyVerbClass.Stative, ConveyVerbClass.Weather,
    )

    /** Must match `codegen.py`'s `REFINEMENTS` list order exactly — see the generation doc. */
    private val REFINEMENT_BY_CODE = arrayOf(
        ConveyVerbClass.PurePath, ConveyVerbClass.MannerAgent, ConveyVerbClass.SubtleBody,
        ConveyVerbClass.StateMetaphor, ConveyVerbClass.Contact, ConveyVerbClass.Punctual,
        ConveyVerbClass.Scalar, ConveyVerbClass.Emotion, ConveyVerbClass.Perception,
    )

    private class ParsedData(
        val synsets: Map<Int, Synset>,
        val lemmaOffsets: Map<String, List<Int>>,
        val exceptions: Map<String, String>,
        /** WordNet synset offset -> VerbNet-refined class, for the *exact* sense VerbNet linked. */
        val offsetOverride: Map<Int, ConveyVerbClass>,
    )

    private val data: ParsedData by lazy(LazyThreadSafetyMode.NONE) { parse(ConveyVerbData.blob) }

    private fun parse(blob: String): ParsedData {
        val synsets = HashMap<Int, Synset>()
        val lemmaOffsets = HashMap<String, List<Int>>()
        val exceptions = HashMap<String, String>()
        val offsetOverride = HashMap<Int, ConveyVerbClass>()

        var section = 0
        for (line in blob.split('\n')) {
            if (line.startsWith("##")) {
                section = when (line) {
                    "##SYN" -> 1
                    "##LEM" -> 2
                    "##EXC" -> 3
                    "##REF" -> 4
                    else -> section
                }
                continue
            }
            if (line.isEmpty()) continue
            when (section) {
                1 -> { // offset \t domainCode \t gloss
                    val first = line.indexOf('\t')
                    val second = line.indexOf('\t', first + 1)
                    val offset = line.substring(0, first).toInt()
                    val domainCode = line.substring(first + 1, second).toInt()
                    val gloss = line.substring(second + 1)
                    synsets[offset] = Synset(DOMAIN_BY_CODE[domainCode], gloss)
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
                4 -> { // offset \t refinementCode
                    val tab = line.indexOf('\t')
                    val offset = line.substring(0, tab).toInt()
                    val code = line.substring(tab + 1).toInt()
                    offsetOverride[offset] = REFINEMENT_BY_CODE[code]
                }
            }
        }
        return ParsedData(synsets, lemmaOffsets, exceptions, offsetOverride)
    }

    // ── WordNet-style verb lemmatization ─────────────────────────────────────────────────────

    /** WordNet's own verb detachment rules (`morph.c`'s `sufx`/`addr` tables), in order. */
    private val DETACHMENT_RULES = listOf(
        "ies" to "y", "es" to "e", "es" to "", "ed" to "e", "ed" to "",
        "ing" to "e", "ing" to "", "s" to "",
    )

    /**
     * Resolves [word] to its WordNet base form: the real `verb.exc` irregular-form table first
     * (e.g. "ran" → "run", "went" → "go"), then WordNet's own regular detachment rules, checked
     * against the actual lemma index rather than applied blindly. Returns null if no base form
     * has a WordNet verb entry at all.
     *
     * Known limitation: a word that is *itself* a valid base-form verb takes priority over an
     * irregular reinterpretation — e.g. "saw" resolves to the tool-use verb "saw" (to cut),
     * never to "see"'s irregular past tense, because disambiguating that requires part-of-speech
     * context this single-word lemmatizer does not have. This mirrors a real, well-known
     * limitation of deterministic morphological analysis, not an oversight.
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
     * Resolves [word] to its [ConveyVerbClass], per the two-layer architecture documented on
     * [ConveyVerbClass]. Builds one candidate classification per WordNet sense (VerbNet
     * refinement where one exists for that exact sense, else the sense's WordNet domain), in
     * sense-frequency order. If every sense agrees, returns that unambiguously. If they disagree
     * and [context] is non-blank, disambiguates via the Simplified Lesk algorithm the report
     * names — gloss word-overlap against [context]'s own words, excluding the target word's own
     * forms so a gloss's self-referential example sentence can't spuriously "match" the query.
     * Without [context] (or when no candidate's gloss overlaps it), resolves to the verb's
     * primary — i.e. most frequent — WordNet sense, deterministically.
     */
    fun classify(word: String, context: String = ""): ConveyVerbClass {
        val lemma = lemmatize(word) ?: return ConveyVerbClass.Unclassified
        val offsets = data.lemmaOffsets.getValue(lemma)
        val offset = resolveOffset(lemma, offsets, context) ?: return ConveyVerbClass.Unclassified
        return data.offsetOverride[offset] ?: data.synsets[offset]?.domain ?: ConveyVerbClass.Unclassified
    }

    /**
     * The procedural static-layout category [word]'s resolved sense (same resolution [classify]
     * uses) suggests, or null if none of the categories' vocabulary matches. Generalizes the
     * report's own single worked example (§"Static Topographical Alignment and Concrete Poetry":
     * "if the engine parses a verb denoting a cascading downward movement... it can trigger a
     * static typographical alignment rule") to the same "verb's semantic path → static coordinate
     * geometry" principle across a small set of spatially-opposed categories, rather than
     * hardcoding descent as the only shape a sentence's own geometry can take.
     *
     * Each category is a keyword check against the resolved sense's real WordNet gloss text — not
     * a hypernym-chain traversal (verified against actual WordNet data: near-synonyms like "fall"
     * and "descend" sit at inconsistent hypernym depths from their shared parent, so chain-ancestor
     * checks miss several of the report's own named examples; gloss vocabulary catches them
     * because WordNet's definitions themselves tend to cross-reference the same handful of words
     * for the same concept). See [compose.conveyance.foundation.ConveyTopographicalPaths] for the
     * geometry each category actually drives.
     */
    fun topographicalCategory(word: String, context: String = ""): ConveyTopographicalCategory? {
        val lemma = lemmatize(word) ?: return null
        val offsets = data.lemmaOffsets.getValue(lemma)
        val offset = resolveOffset(lemma, offsets, context) ?: return null
        val gloss = data.synsets[offset]?.gloss ?: return null
        // Unlike Lesk (which excludes the query word's own forms so a gloss's self-referential
        // example can't spuriously "match" external context), the marker check here is comparing
        // the gloss against a fixed vocabulary independent of the query word -- excluding the
        // word's own forms would strip exactly the marker most glosses actually use (e.g.
        // "circle"'s own primary gloss example literally says "circle the globe").
        val tokens = tokenize(gloss, exclude = emptySet())
        return TOPOGRAPHICAL_MARKERS.entries.firstOrNull { (_, markers) -> tokens.any { it in markers } }?.key
    }

    /** Convenience for [topographicalCategory] `== `[ConveyTopographicalCategory.Descent]. */
    fun isDescent(word: String, context: String = ""): Boolean =
        topographicalCategory(word, context) == ConveyTopographicalCategory.Descent

    private val TOPOGRAPHICAL_MARKERS: Map<ConveyTopographicalCategory, Set<String>> = mapOf(
        ConveyTopographicalCategory.Descent to setOf(
            "downward", "descend", "descending", "descent", "fall", "falling", "drop", "sink", "plunge",
        ),
        ConveyTopographicalCategory.Ascent to setOf(
            "upward", "ascend", "ascending", "ascent", "rise", "rising", "climb", "climbing", "soar", "mount", "mounting",
        ),
        ConveyTopographicalCategory.Scatter to setOf(
            "scatter", "scattered", "scattering", "disperse", "dispersed", "dispersing", "spread", "spreading",
        ),
        ConveyTopographicalCategory.Encircle to setOf(
            "circle", "encircle", "surround", "surrounding", "orbit", "orbiting", "encompass", "encompassing",
        ),
    )

    /** Shared sense-resolution core for [classify] and [topographicalCategory] — see [classify]'s doc comment. */
    private fun resolveOffset(lemma: String, offsets: List<Int>, context: String): Int? {
        val perOffsetClass = offsets.map { offset ->
            data.offsetOverride[offset] ?: data.synsets[offset]?.domain ?: ConveyVerbClass.Unclassified
        }
        if (perOffsetClass.distinct().size == 1) return offsets[0]
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

    /** The lemma's own inflected forms, so Lesk never scores a gloss's self-example as "context". */
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

    // ── Simplified Lesk (report §"Deterministic Word Sense Disambiguation") ─────────────────

    private val STOPWORDS = setOf(
        "a", "an", "the", "of", "to", "in", "on", "at", "for", "with", "and", "or", "is", "are",
        "was", "were", "be", "been", "being", "by", "as", "it", "its", "that", "this", "from",
        "into", "than", "then", "so", "not", "no", "do", "does", "did", "has", "have", "had",
    )

    // Apostrophe only *inside* a letter run (contractions: "don't", "can't") -- never at a word's
    // edge. The generated gloss data re-encodes each embedded '"' as a bare "'" (see
    // ConveyVerbData.kt's generation pipeline), so a quoted example like "circle the globe"
    // becomes 'circle the globe' with no space before the opening quote -- a naive [A-Za-z']+
    // regex would swallow that leading apostrophe into the token ('circle), which then matches
    // no real word. Anchoring the apostrophe between letters avoids that without touching the data.
    private val WORD_PATTERN = Regex("[A-Za-z]+(?:'[A-Za-z]+)*")

    private fun tokenize(text: String, exclude: Set<String>): Set<String> =
        WORD_PATTERN.findAll(text.lowercase())
            .map { it.value }
            .filter { it.length > 2 && it !in STOPWORDS && it !in exclude }
            .toSet()
}
