package compose.conveyance

/**
 * The deterministic, rule-based verb classification described in
 * `docs/kinetic-text-verb-classification.md` — implemented as the smallest real slice of that
 * report rather than a full port of it.
 *
 * That report's engine has three layers: classify the verb (Levin/VerbNet), disambiguate its
 * sense in context (Simplified Lesk against WordNet glosses), then map the resolved class to
 * animation parameters. This library ships none of Levin's ~3,100-verb taxonomy, VerbNet's
 * predicate logic, or WordNet's gloss corpus — none of that data is available here. What this
 * file implements instead is the *architecture* the report argues for, at a scale a hand-curated
 * lexicon can actually cover honestly: a fixed set of kinetic classes drawn directly from the
 * report's own worked examples (§"Addressing Granularity in Motion Verbs", §"Analogous Motion
 * and Force Dynamics", its Aktionsart table), a lexicon of the verbs the report itself names,
 * and no word-sense disambiguation — every entry is one verb to one class, always. A verb the
 * lexicon doesn't recognize renders as [ConveyVerbClass.Unclassified] (⇒ [ConveyLife.None]),
 * never a guess.
 *
 * Extending coverage is a lexicon edit ([ConveyVerbLexicon]), never a new case here — adding a
 * [ConveyVerbClass] is a taxonomy decision and should be rare.
 */
enum class ConveyVerbClass {
    // ── Motion hierarchy (report §"Addressing Granularity in Motion Verbs") ──────────────────

    /** e.g. go, come. Report: "constant-velocity linear translation without manner modifiers." */
    PurePath,

    /** e.g. limp, stagger, walk. Report: "asymmetric, arrhythmic... an uneven gait." */
    MannerAgent,

    /** e.g. shiver, tremble. Report: "high-frequency, low-amplitude positional vibrations." */
    SubtleBody,

    /** e.g. soar, bake, boil. Report: "ease-out upward trajectory... dynamic lightening." */
    StateMetaphor,

    // ── Force / contact (report §Levin's "Hit" class) ────────────────────────────────────────

    /** e.g. hit, kick, beat, destroy. Report: "sudden impact scaling or typographic vibration." */
    Contact,

    /** e.g. shatter, snap, arrive. Report: "sudden, high-velocity shifts, zero interpolation." */
    Punctual,

    // ── Aktionsart (report's Levin table) ─────────────────────────────────────────────────────

    /** e.g. read, kill, find, build. Report: "ease-out... resolve into a static, final state." */
    Scalar,

    // ── Force dynamics / affect (report §"Analogous Motion and Force Dynamics") ──────────────

    /** Anger, excitement. Report: "small, rapid, high-frequency vibrations... trembling." */
    HighArousal,

    /** Disappointment, sadness. Report: "slow, shrinking motions... slumping shoulders." */
    LowArousal,

    /** Peaceful states. Report: "slow, rhythmic expansions and contractions... breathing." */
    Calm,

    /** Shouting, roaring. Report: "massive scale expansions... shake effects... reverberation." */
    Volume,

    // ── WordNet-domain fallback (report §"High-Level Domain Categorization via WordNet") ─────

    /** e.g. speak, inform, declare. Report: "originates... mimicking an acoustic wave." */
    Communication,

    /** e.g. love, trust, see. Report: "font weight interpolations or color-space transitions." */
    Emotion,

    /** No lexicon entry. Renders still — [ConveyLife.None] — rather than assume a class. */
    Unclassified,
}

/**
 * Maps a resolved [ConveyVerbClass] to a concrete [ConveyLife] profile.
 *
 * [ConveyLife] only speaks in scale, opacity, and skew — it has no font-weight or color axis
 * yet, and no notion of spatial path translation ([PurePath], [Scalar] describe *global*
 * kineticism, which belongs to [ConveyTransform]/[ConveyMorph], not per-glyph idle motion).
 * Where the report's own description names a mechanism [ConveyLife] can't yet produce, the
 * mapping below picks the closest honest fit and says so — it does not fabricate a match.
 */
fun ConveyVerbClass.toConveyLife(): ConveyLife = when (this) {
    // Global motion — no per-glyph idle profile fits; the caller should drive position via
    // ConveyTransform.slideIn instead. Rendered still here, not guessed at.
    ConveyVerbClass.PurePath -> ConveyLife.None
    ConveyVerbClass.Scalar -> ConveyLife.None

    ConveyVerbClass.MannerAgent -> ConveyLife.Wobble(period = 1400L, skewDegrees = 5f)
    ConveyVerbClass.SubtleBody -> ConveyLife.Wobble(period = 340L, skewDegrees = 1.5f)
    // Weight/height interpolation isn't implemented yet; a slow upward-reading breathe is the
    // closest available approximation of "lightening, ease-out drift."
    ConveyVerbClass.StateMetaphor -> ConveyLife.Breathe(period = 3200L, peakScale = 1.06f)

    // Contact/Punctual are one-shot events, not idle chrome — they belong on the trigger path
    // (see ConveyLife.Burst / Modifier.conveyLifeBurst), not the continuous one. No idle motion.
    ConveyVerbClass.Contact -> ConveyLife.None
    ConveyVerbClass.Punctual -> ConveyLife.None

    ConveyVerbClass.HighArousal -> ConveyLife.Wobble(period = 500L, skewDegrees = 6f)
    ConveyVerbClass.LowArousal -> ConveyLife.Breathe(period = 3600L, peakScale = 1.03f, minOpacity = 0.7f)
    ConveyVerbClass.Calm -> ConveyLife.Breathe(period = 2600L, peakScale = 1.08f)
    ConveyVerbClass.Volume -> ConveyLife.Breathe(period = 900L, peakScale = 1.25f)

    // "Distant, still transmitting" is Twinkle's own stated meaning — this is the one class
    // where the report's mechanism and ConveyLife's existing vocabulary line up exactly.
    ConveyVerbClass.Communication -> ConveyLife.Twinkle(period = 1800L)
    // No color-space axis exists yet; Breathe stands in for "internal state, quietly present."
    ConveyVerbClass.Emotion -> ConveyLife.Breathe(period = 3000L, peakScale = 1.05f, minOpacity = 0.88f)

    ConveyVerbClass.Unclassified -> ConveyLife.None
}

/**
 * A hand-curated, deterministic verb → [ConveyVerbClass] lexicon.
 *
 * Every entry here is drawn from a specific example named in
 * `docs/kinetic-text-verb-classification.md` — this is not a heuristic or a synonym expansion,
 * it is a literal transcription of that report's worked cases. There is no word-sense
 * disambiguation: a verb classified here always resolves to the same class regardless of the
 * sentence around it (the report's Simplified Lesk stage is not implemented). Extend this map
 * to extend coverage; do not loosen [classify]'s matching to compensate for a missing entry.
 */
object ConveyVerbLexicon {

    private val classified: Map<String, ConveyVerbClass> = run {
        val map = mutableMapOf<String, ConveyVerbClass>()
        fun put(vararg words: String, verbClass: ConveyVerbClass) {
            words.forEach { map[it] = verbClass }
        }

        put("go", "come", verbClass = ConveyVerbClass.PurePath)
        put("limp", "stagger", "walk", "stroll", verbClass = ConveyVerbClass.MannerAgent)
        put("shiver", "tremble", verbClass = ConveyVerbClass.SubtleBody)
        put(
            "soar", "bake", "boil", "fry", "roast", "simmer",
            verbClass = ConveyVerbClass.StateMetaphor,
        )

        put("hit", "kick", "beat", "destroy", verbClass = ConveyVerbClass.Contact)
        put("shatter", "snap", "arrive", verbClass = ConveyVerbClass.Punctual)

        put("read", "kill", "find", "build", verbClass = ConveyVerbClass.Scalar)

        put("shout", "yell", "roar", verbClass = ConveyVerbClass.Volume)
        put("speak", "inform", "declare", "say", "tell", verbClass = ConveyVerbClass.Communication)
        put("love", "trust", "see", verbClass = ConveyVerbClass.Emotion)

        map
    }

    /**
     * Resolves [word] to its lexicon class, applying only a minimal, deterministic suffix strip
     * (-ing/-ed/-s) so common inflections of a listed verb still resolve. This is not a real
     * morphological analyzer: irregular forms (ran, spoke, built) are not normalized and must be
     * listed as their own entries if they need to classify. Ambiguity is never resolved by
     * guessing — an unrecognized word always returns [ConveyVerbClass.Unclassified].
     */
    fun classify(word: String): ConveyVerbClass {
        val lower = word.lowercase().filter { it.isLetter() }
        if (lower.isEmpty()) return ConveyVerbClass.Unclassified
        return classified[lower] ?: classified[lemmatize(lower)] ?: ConveyVerbClass.Unclassified
    }

    private fun lemmatize(word: String): String = when {
        word.length > 4 && word.endsWith("ing") -> word.removeSuffix("ing")
        word.length > 3 && word.endsWith("ed") -> word.removeSuffix("ed")
        word.length > 3 && word.endsWith("s") && !word.endsWith("ss") -> word.removeSuffix("s")
        else -> word
    }
}
