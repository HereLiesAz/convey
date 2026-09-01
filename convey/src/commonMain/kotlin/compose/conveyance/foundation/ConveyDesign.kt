package compose.conveyance.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import compose.conveyance.ConveyKineticSentence
import compose.conveyance.ConveyKineticText
import compose.conveyance.ConveyLife
import compose.conveyance.tokens.ConveyColor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The framework's automatic-composition primitive — Part XI of the Conveyance Manifesto,
 * "The Design Block" (`docs/CONVEYANCE-FRAMEWORK.md`).
 *
 * A [ConveyDesign] is a container of [ConveyDesignLine]s, each carrying a semantic
 * [ConveyDesignLevel] (`Title`/`Header1`/`Header2`/`Header3`/`Body`, analogous to an HTML
 * heading level) and an alignment. [ConveyDesignSolver] adjusts size, weight, condensation, and
 * tracking across the block so its silhouette reads as an intentionally balanced — not
 * necessarily symmetric — composition: a freestanding line takes its nominal, modular-scale
 * size (hierarchy-balance mode, §11.4); a line that inherits a column carved by an earlier
 * line's natural width resizes to fill it (column-fill mode); a line whose inherited column is
 * too narrow to hold it reasonably mirrors the earlier line's whole shape to the opposite edge
 * instead of forcing an unreadable fit (the mirror-fallback rule, §11.6).
 *
 * **Implementation status:** the solver ([ConveyDesignSolver]) is pure, platform-independent
 * math and is exercised directly by `ConveyDesignSolverTest` — it does not depend on this file's
 * Composable wrapper. The wrapper takes the block's available width as an explicit `fullWidthSp`
 * parameter (in the same approximate "advance-width units" the solver's own [ConveyDesignSolver]
 * uses) rather than measuring the actual rendered width of its content via a `TextMeasurer` —
 * real glyph metrics per platform/font are real future work, not yet done here. Condensation is
 * rendered as a horizontal `graphicsLayer` scale rather than a variable font's `wdth` axis,
 * since `ConveyType`'s Azrienoch integration (`tokens/ConveyType.kt`) has not landed on this
 * branch yet; wiring true `wdth`/`wght` axes through `conveyTypeFontFamily` once it does is the
 * natural next step and should replace both approximations without changing this file's public
 * API shape.
 *
 * **Motion (§4.2 of the manifesto):** every [ConveyDesignLine] defaults to
 * [ConveyDesignMotion.None] — static, solved layout only. A line may opt into
 * [ConveyDesignMotion.Kinetic] (per-glyph, via [ConveyKineticText]) or
 * [ConveyDesignMotion.Sentence] (per-word, verb-driven, via [ConveyKineticSentence]); this block
 * never picks a motion for a line on its own. Solved size/weight/condensation/tracking apply
 * identically regardless of motion — motion is layered on top of the solve, never a substitute
 * for it.
 */
enum class ConveyDesignLevel(internal val scaleStep: Int) {
    Title(4),
    Header1(3),
    Header2(2),
    Header3(1),
    Body(0),
}

enum class ConveyDesignAlignment { Left, Right, Center, Justify }

/**
 * A line's motion, chosen from the framework's existing kinetic-typography vocabulary rather
 * than a bespoke gesture (§4.2: "one motion grammar for text, not two"). Offered, never assumed
 * — every line defaults to [None], and static `Body` text with nothing to teach or signal stays
 * that way; picking a value here is a per-line choice a developer makes, not a default this
 * block imposes on their behalf.
 */
enum class ConveyDesignMotion {
    /** Plain, static text. The default for every line, [ConveyDesignLevel.Body] included. */
    None,
    /** Per-glyph motion via [ConveyKineticText] — a Tell-scale gesture, suited to short lines. */
    Kinetic,
    /** Per-word, verb-driven motion via [ConveyKineticSentence] — suited to sentence-length lines. */
    Sentence,
}

/** A horizontal span within a [ConveyDesign] block's full width, in the solver's width units. */
@Immutable
data class ConveyDesignColumn(val start: Float, val end: Float) {
    val width: Float get() = (end - start).coerceAtLeast(0f)
}

@Immutable
data class ConveyDesignLine(
    val text: String,
    val level: ConveyDesignLevel = ConveyDesignLevel.Body,
    val alignment: ConveyDesignAlignment = ConveyDesignAlignment.Left,
    /** Rule 1 of the column-targeting tree (§11.5): an explicit column always wins. */
    val explicitColumn: ConveyDesignColumn? = null,
    /** See [ConveyDesignMotion] — defaults to no motion at all. */
    val motion: ConveyDesignMotion = ConveyDesignMotion.None,
    /** Idle motion [ConveyKineticText] falls back to between Kinetic bursts; ignored otherwise. */
    val idle: ConveyLife = ConveyLife.None,
)

/** The four levers the solver adjusts, in priority order: size, weight, condensation, tracking. */
@Immutable
data class ConveyDesignAxes(
    val fontSizeSp: Float,
    val weight: Float,
    /** Percent; 100 = normal width, below 100 = condensed. */
    val condensation: Float,
    val trackingSp: Float,
)

@Immutable
data class ConveyDesignSolvedLine(
    val line: ConveyDesignLine,
    val axes: ConveyDesignAxes,
    val naturalWidth: Float,
    val column: ConveyDesignColumn,
    /** True when the mirror-fallback rule (§11.6) fired for this line. */
    val mirrored: Boolean,
)

/**
 * Pure, platform-independent solver math for §11.2–§11.6 of the Design Block spec. Kept free of
 * any Composable/UI dependency so it is directly unit-testable.
 */
object ConveyDesignSolver {

    /** A perfect fourth — Bringhurst's modular scale (§11.2), the default hierarchy ratio. */
    const val DEFAULT_RATIO = 1.333f
    const val DEFAULT_BASE_SIZE_SP = 16f
    const val MIN_REASONABLE_SIZE_SP = 9f

    private const val SPACE_ADVANCE = 0.28f
    private const val AVG_ADVANCE = 0.52f

    /** §11.3's `advanceWidth(char, wdth)`, approximated as a fixed per-em average for latin text. */
    private fun advanceUnits(text: String): Float =
        text.sumOf { (if (it == ' ') SPACE_ADVANCE else AVG_ADVANCE).toDouble() }.toFloat()

    /** `base × ratio^n` — one constant produces the whole scale (§11.2). */
    fun nominalSize(level: ConveyDesignLevel, baseSizeSp: Float = DEFAULT_BASE_SIZE_SP, ratio: Float = DEFAULT_RATIO): Float {
        var r = 1f
        repeat(level.scaleStep) { r *= ratio }
        return baseSizeSp * r
    }

    /** Nominal weight per level. Monotonic with [ConveyDesignLevel] ordering, per §11.2. */
    fun nominalWeight(level: ConveyDesignLevel): Float = when (level) {
        ConveyDesignLevel.Title -> 700f
        ConveyDesignLevel.Header1 -> 650f
        ConveyDesignLevel.Header2 -> 600f
        ConveyDesignLevel.Header3 -> 550f
        ConveyDesignLevel.Body -> 400f
    }

    /** §11.3: `Σ advanceWidth(char) × fontSize² × strokeWeightFactor(weight)`. A ratio tool, not a literal ink-coverage measurement. */
    fun inkScore(text: String, fontSizeSp: Float, weight: Float): Float {
        val strokeWeightFactor = weight / 400f
        return advanceUnits(text) * fontSizeSp * fontSizeSp * strokeWeightFactor
    }

    /**
     * A line's rendered width at the given axes. Weight does not appear here: in this model,
     * weight changes a glyph's ink (stroke mass, §11.3) but not its advance width, so the
     * column-fill solve (below) skips straight from size to condensation, exactly as the
     * priority order in §11.4 allows once a lever stops moving the measured quantity.
     */
    fun naturalWidth(text: String, fontSizeSp: Float, condensation: Float = 100f, trackingSp: Float = 0f): Float {
        val base = advanceUnits(text) * fontSizeSp * (condensation / 100f)
        val gaps = (text.length - 1).coerceAtLeast(0)
        return base + trackingSp * gaps
    }

    /**
     * Solves one line toward [targetWidth] (column-fill mode, §11.4), walking size →
     * condensation → tracking (weight is skipped — see [naturalWidth]'s doc). Returns `null`
     * when even the most extreme settings on every lever cannot reasonably reach the target;
     * the caller applies the mirror-fallback rule (§11.6) in that case.
     */
    fun solveToWidth(
        text: String,
        nominal: ConveyDesignAxes,
        targetWidth: Float,
        minSizeSp: Float = MIN_REASONABLE_SIZE_SP,
        maxSizeSp: Float = nominal.fontSizeSp * 1.5f,
        minCondensation: Float = 62.5f,
        maxCondensation: Float = 100f,
        maxTrackingSp: Float = 2f,
    ): ConveyDesignAxes? {
        val weight = nominal.weight

        val size = binarySearchForTarget(minSizeSp, maxSizeSp, targetWidth) { s ->
            naturalWidth(text, s, nominal.condensation, nominal.trackingSp)
        }
        var width = naturalWidth(text, size, nominal.condensation, nominal.trackingSp)
        if (closeEnough(width, targetWidth)) return ConveyDesignAxes(size, weight, nominal.condensation, nominal.trackingSp)

        val condensation = binarySearchForTarget(minCondensation, maxCondensation, targetWidth) { c ->
            naturalWidth(text, size, c, nominal.trackingSp)
        }
        width = naturalWidth(text, size, condensation, nominal.trackingSp)
        if (closeEnough(width, targetWidth)) return ConveyDesignAxes(size, weight, condensation, nominal.trackingSp)

        val remaining = targetWidth - width
        val gaps = (text.length - 1).coerceAtLeast(1)
        val tracking = (nominal.trackingSp + remaining / gaps).coerceIn(-maxTrackingSp, maxTrackingSp)
        width = naturalWidth(text, size, condensation, tracking)

        return if (closeEnough(width, targetWidth, toleranceRatio = 0.12f)) {
            ConveyDesignAxes(size, weight, condensation, tracking)
        } else {
            null
        }

        // Note: this searches size and condensation independently of [targetWidth]'s actual
        // reachable range rather than jointly optimizing; see solveBlock's tooNarrow check for
        // the case where no combination gets close enough.
    }

    /**
     * Bisects [lo, hi] for the input whose [widthOf] is closest to the implicit target already
     * baked into the caller's closure — used to invert `naturalWidth`, which is monotonic in
     * both size and condensation, without a closed-form inverse.
     */
    private fun binarySearchForTarget(lo: Float, hi: Float, target: Float, widthOf: (Float) -> Float): Float {
        var low = lo
        var high = hi
        val ascending = widthOf(hi) >= widthOf(lo)
        repeat(24) {
            val mid = (low + high) / 2f
            val tooNarrow = if (ascending) widthOf(mid) < target else widthOf(mid) > target
            if (tooNarrow) low = mid else high = mid
        }
        return (low + high) / 2f
    }

    private fun closeEnough(width: Float, target: Float, toleranceRatio: Float = 0.04f): Boolean =
        abs(width - target) <= target * toleranceRatio

    private fun carveDefiningColumn(line: ConveyDesignLine, naturalWidth: Float, fullWidth: Float): ConveyDesignColumn =
        when (line.alignment) {
            ConveyDesignAlignment.Justify -> ConveyDesignColumn(0f, fullWidth)
            ConveyDesignAlignment.Left -> ConveyDesignColumn(0f, naturalWidth.coerceAtMost(fullWidth))
            ConveyDesignAlignment.Right -> ConveyDesignColumn(fullWidth - naturalWidth.coerceAtMost(fullWidth), fullWidth)
            ConveyDesignAlignment.Center -> {
                val margin = ((fullWidth - naturalWidth) / 2f).coerceAtLeast(0f)
                ConveyDesignColumn(margin, fullWidth - margin)
            }
        }

    /** The column-targeting decision tree, §11.5, rules 2–4 (rule 1 — explicit override — is handled by the caller). */
    private fun targetColumnFor(definingLine: ConveyDesignLine, definingColumn: ConveyDesignColumn, fullWidth: Float): ConveyDesignColumn? =
        when (definingLine.alignment) {
            ConveyDesignAlignment.Justify -> ConveyDesignColumn(0f, fullWidth)
            ConveyDesignAlignment.Left -> ConveyDesignColumn(definingColumn.end, fullWidth)
            ConveyDesignAlignment.Right -> ConveyDesignColumn(0f, definingColumn.start)
            ConveyDesignAlignment.Center -> {
                val leftSlot = definingColumn.start
                val rightSlot = fullWidth - definingColumn.end
                if (leftSlot <= 0f && rightSlot <= 0f) null else ConveyDesignColumn(0f, fullWidth)
            }
        }

    /**
     * Solves an entire block: the first line is the defining line (hierarchy-balance mode,
     * nominal axes, carves the column grid from its own natural width + alignment); every other
     * line without its own [ConveyDesignLine.explicitColumn] inherits a target column from it
     * (§11.5) and either fills it (column-fill mode) or, if too narrow even at every lever's
     * extreme, mirrors the defining line's whole shape to the opposite edge (§11.6).
     */
    fun solveBlock(
        lines: List<ConveyDesignLine>,
        fullWidth: Float,
        baseSizeSp: Float = DEFAULT_BASE_SIZE_SP,
        ratio: Float = DEFAULT_RATIO,
    ): List<ConveyDesignSolvedLine> {
        if (lines.isEmpty()) return emptyList()

        val nominals = lines.map { line ->
            ConveyDesignAxes(
                fontSizeSp = nominalSize(line.level, baseSizeSp, ratio),
                weight = nominalWeight(line.level),
                condensation = 100f,
                trackingSp = 0f,
            )
        }
        val naturalWidths = lines.mapIndexed { i, line -> naturalWidth(line.text, nominals[i].fontSizeSp) }

        val definingLine = lines[0]
        val definingWidth = naturalWidths[0]
        val definingColumn = carveDefiningColumn(definingLine, definingWidth, fullWidth)
        val mirroredDefiningColumn = ConveyDesignColumn(fullWidth - definingColumn.end, fullWidth - definingColumn.start)

        val solved = ArrayList<ConveyDesignSolvedLine>(lines.size)
        solved.add(ConveyDesignSolvedLine(definingLine, nominals[0], definingWidth, definingColumn, mirrored = false))

        for (i in 1 until lines.size) {
            val line = lines[i]
            val nominal = nominals[i]
            val naturalW = naturalWidths[i]
            val target = line.explicitColumn ?: targetColumnFor(definingLine, definingColumn, fullWidth)

            if (target == null) {
                solved.add(ConveyDesignSolvedLine(line, nominal, naturalW, ConveyDesignColumn(0f, fullWidth), mirrored = false))
                continue
            }

            val obviouslyTooNarrow = target.width < naturalW * (MIN_REASONABLE_SIZE_SP / nominal.fontSizeSp)
            if (obviouslyTooNarrow) {
                solved.add(ConveyDesignSolvedLine(line, nominals[0], definingWidth, mirroredDefiningColumn, mirrored = true))
                continue
            }

            val fit = solveToWidth(line.text, nominal, target.width)
            solved.add(
                if (fit == null) {
                    ConveyDesignSolvedLine(line, nominals[0], definingWidth, mirroredDefiningColumn, mirrored = true)
                } else {
                    ConveyDesignSolvedLine(line, fit, naturalWidth(line.text, fit.fontSizeSp, fit.condensation, fit.trackingSp), target, mirrored = false)
                }
            )
        }

        return solved
    }
}

/**
 * Renders a [ConveyDesign] block. See this file's top-level doc comment for what `fullWidthSp`
 * means today and what it should become once real glyph-metric measurement is wired in.
 */
@Composable
fun ConveyDesign(
    lines: List<ConveyDesignLine>,
    fullWidthSp: Float,
    modifier: Modifier = Modifier,
    color: Color = ConveyColor.OnSurface,
    baseSizeSp: Float = ConveyDesignSolver.DEFAULT_BASE_SIZE_SP,
    ratio: Float = ConveyDesignSolver.DEFAULT_RATIO,
) {
    val solved = remember(lines, fullWidthSp, baseSizeSp, ratio) {
        ConveyDesignSolver.solveBlock(lines, fullWidthSp, baseSizeSp, ratio)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        for (solvedLine in solved) {
            val boxAlignment = when (solvedLine.line.alignment) {
                ConveyDesignAlignment.Left -> Alignment.CenterStart
                ConveyDesignAlignment.Right -> Alignment.CenterEnd
                ConveyDesignAlignment.Center -> Alignment.Center
                ConveyDesignAlignment.Justify -> Alignment.CenterStart
            }
            val style = TextStyle(
                color = color,
                fontSize = solvedLine.axes.fontSizeSp.sp,
                fontWeight = FontWeight(solvedLine.axes.weight.roundToInt().coerceIn(1, 1000)),
                letterSpacing = solvedLine.axes.trackingSp.sp,
            )
            val condensed = Modifier.graphicsLayer(scaleX = solvedLine.axes.condensation / 100f)

            Box(Modifier.fillMaxWidth(), contentAlignment = boxAlignment) {
                when (solvedLine.line.motion) {
                    ConveyDesignMotion.None -> androidx.compose.material3.Text(
                        text = solvedLine.line.text,
                        style = style,
                        modifier = condensed,
                    )
                    ConveyDesignMotion.Kinetic -> ConveyKineticText(
                        text = solvedLine.line.text,
                        idle = solvedLine.line.idle,
                        style = style,
                        modifier = condensed,
                    )
                    ConveyDesignMotion.Sentence -> ConveyKineticSentence(
                        text = solvedLine.line.text,
                        style = style,
                        modifier = condensed,
                    )
                }
            }
        }
    }
}
