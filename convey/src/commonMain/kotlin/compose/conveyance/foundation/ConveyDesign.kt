package compose.conveyance.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.conveyance.ConveyKineticSentence
import compose.conveyance.ConveyKineticText
import compose.conveyance.ConveyLife
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyTypeVariation
import compose.conveyance.tokens.conveyTypeFontFamily
import kotlin.math.abs

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
 * [ConveyDesignPage] promotes the same rules one level, §11.7: multiple `DESIGN` blocks on one
 * page relate to each other the way lines within one block do (a block that doesn't span the
 * full width becomes the measure the next block balances against; a shorter block's height
 * pulls toward the accumulated height of the blocks before it), reusing the exact same
 * column-targeting and mirror-fallback logic rather than a separate mechanism.
 *
 * **Implementation status:** the solver ([ConveyDesignSolver]) is pure, platform-independent
 * math and defaults everywhere to [ConveyDesignSolver.naturalWidth]'s fixed per-character
 * advance-width approximation — unchanged for `ConveyDesignSolverTest`'s own direct calls, which
 * have no [androidx.compose.ui.text.TextMeasurer] to measure against. [ConveyDesign]/
 * [ConveyDesignPage] instead measure against a real [rememberConveyDesignMeasure]: a
 * `TextMeasurer` bound to Azrienoch at `wdth=100`, varying only `fontSize`/`letterSpacing` per
 * call (both cheap — no new [FontFamily][androidx.compose.ui.text.font.FontFamily] instance
 * needed) for the size lever's binary search, with the condensation lever applied as a linear
 * scale correction on top rather than a second real measurement. That's a genuine, honestly
 * scoped limitation, not an oversight: rebuilding a `FontFamily` at a different `wdth` per
 * candidate would need `conveyTypeFontFamily`'s underlying `Font()` resolved fresh each time,
 * which is `@Composable`/async-loaded and cannot run synchronously inside a solver's binary
 * search. See [rememberConveyDesignMeasure]'s own doc for the full accounting. Condensation and
 * weight render through real Azrienoch variable-font axes (`wdth`/`wght`, via
 * [conveyTypeFontFamily]) rather than a `graphicsLayer` approximation, now that `ConveyType`'s
 * Azrienoch integration has landed.
 *
 * **Motion (§4.2 of the manifesto):** every [ConveyDesignLine] defaults to
 * [ConveyDesignMotion.None] — static, solved layout only. A line may opt into
 * [ConveyDesignMotion.Kinetic] (per-glyph, via [ConveyKineticText]) or
 * [ConveyDesignMotion.Sentence] (per-word, verb-driven, via [ConveyKineticSentence]); this block
 * never picks a motion for a line on its own. Solved size/weight/condensation/tracking apply
 * identically regardless of motion — motion is layered on top of the solve, never a substitute
 * for it. A line with [ConveyDesignLine.isAct] set ignores `motion` and always renders through
 * [ConveyActText] instead — the persistent Decoration channel plus a one-time Tell burst, per
 * §4.2 ("text as an Act"), rather than a developer having to pick a motion that happens to also
 * look like a link.
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
    /** See [ConveyDesignMotion] — defaults to no motion at all. Ignored when [isAct] is set. */
    val motion: ConveyDesignMotion = ConveyDesignMotion.None,
    /** Idle motion [ConveyKineticText] falls back to between Kinetic bursts; ignored otherwise. */
    val idle: ConveyLife = ConveyLife.None,
    /** §4.2: this line is itself an Act, not merely descriptive text. Renders via [ConveyActText]. */
    val isAct: Boolean = false,
    /** Required when [isAct] is set; ignored otherwise. */
    val onClick: (() -> Unit)? = null,
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
    /** True when the mirror-fallback rule (§11.6, or its block-level promotion, §11.7) fired for this line. */
    val mirrored: Boolean,
)

/**
 * A pluggable width measurement for §11.4's column-fill solve. Defaults everywhere to
 * [ConveyDesignSolver.naturalWidth] (the fixed per-character advance-width approximation) — see
 * [rememberConveyDesignMeasure] for a real, `TextMeasurer`-based one.
 */
typealias ConveyDesignMeasure = (text: String, fontSizeSp: Float, condensation: Float, trackingSp: Float) -> Float

/**
 * Pure, platform-independent solver math for §11.2–§11.7 of the Design Block spec. Kept free of
 * any Composable/UI dependency so it is directly unit-testable.
 */
object ConveyDesignSolver {

    /** A perfect fourth — Bringhurst's modular scale (§11.2), the default hierarchy ratio. */
    const val DEFAULT_RATIO = 1.333f
    const val DEFAULT_BASE_SIZE_SP = 16f
    const val MIN_REASONABLE_SIZE_SP = 9f

    /** Azrienoch's real published `wdth` floor (`ConveyTypeAxis.Width.min`) — condensation cannot go narrower than the font actually supports. */
    const val MIN_CONDENSATION = 75f

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
        minCondensation: Float = MIN_CONDENSATION,
        maxCondensation: Float = 100f,
        maxTrackingSp: Float = 2f,
        measure: ConveyDesignMeasure = { text2, size2, condensation2, tracking2 -> naturalWidth(text2, size2, condensation2, tracking2) },
    ): ConveyDesignAxes? {
        val weight = nominal.weight

        val size = binarySearchForTarget(minSizeSp, maxSizeSp, targetWidth) { s ->
            measure(text, s, nominal.condensation, nominal.trackingSp)
        }
        var width = measure(text, size, nominal.condensation, nominal.trackingSp)
        if (closeEnough(width, targetWidth)) return ConveyDesignAxes(size, weight, nominal.condensation, nominal.trackingSp)

        val condensation = binarySearchForTarget(minCondensation, maxCondensation, targetWidth) { c ->
            measure(text, size, c, nominal.trackingSp)
        }
        width = measure(text, size, condensation, nominal.trackingSp)
        if (closeEnough(width, targetWidth)) return ConveyDesignAxes(size, weight, condensation, nominal.trackingSp)

        val remaining = targetWidth - width
        val gaps = (text.length - 1).coerceAtLeast(1)
        val tracking = (nominal.trackingSp + remaining / gaps).coerceIn(-maxTrackingSp, maxTrackingSp)
        width = measure(text, size, condensation, tracking)

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

    /** The column-targeting decision tree, §11.5, rules 2–4 (rule 1 — explicit override — is handled by the caller). Reused unchanged at block level by [solvePage] (§11.7). */
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
     * extreme, mirrors the defining line's whole shape to the opposite edge (§11.6). [measure]
     * defaults to [naturalWidth]'s fixed advance-width approximation; pass a real one (see
     * [rememberConveyDesignMeasure]) to size against actual rendered glyph metrics instead —
     * column carving and column-fill both use whichever is given, so the two stay consistent
     * with each other.
     */
    fun solveBlock(
        lines: List<ConveyDesignLine>,
        fullWidth: Float,
        baseSizeSp: Float = DEFAULT_BASE_SIZE_SP,
        ratio: Float = DEFAULT_RATIO,
        measure: ConveyDesignMeasure = { text2, size2, condensation2, tracking2 -> naturalWidth(text2, size2, condensation2, tracking2) },
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
        val naturalWidths = lines.mapIndexed { i, line -> measure(line.text, nominals[i].fontSizeSp, 100f, 0f) }

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

            val fit = solveToWidth(line.text, nominal, target.width, measure = measure)
            solved.add(
                if (fit == null) {
                    ConveyDesignSolvedLine(line, nominals[0], definingWidth, mirroredDefiningColumn, mirrored = true)
                } else {
                    ConveyDesignSolvedLine(line, fit, measure(line.text, fit.fontSizeSp, fit.condensation, fit.trackingSp), target, mirrored = false)
                }
            )
        }

        return solved
    }

    /** Solves [lines] against [column]'s own width, then offsets every result back into [column]'s absolute position — the block-level counterpart of a line filling an inherited column. */
    private fun solveBlockWithinColumn(
        lines: List<ConveyDesignLine>,
        column: ConveyDesignColumn,
        baseSizeSp: Float,
        ratio: Float,
        measure: ConveyDesignMeasure,
    ): List<ConveyDesignSolvedLine> {
        val relative = solveBlock(lines, column.width, baseSizeSp, ratio, measure)
        return relative.map { it.copy(column = ConveyDesignColumn(column.start + it.column.start, column.start + it.column.end)) }
    }

    /** The smallest column spanning every line's own column in [block] — that block's "whole shape," for the mirror-fallback rule promoted to block level. */
    private fun boundingColumn(block: List<ConveyDesignSolvedLine>): ConveyDesignColumn =
        ConveyDesignColumn(block.minOf { it.column.start }, block.maxOf { it.column.end })

    /**
     * §11.7: cross-block (page-level) propagation, promoted one level from §11.5/§11.6 rather
     * than a separate mechanism — blocks share the same full-width coordinate space lines
     * within one block do, so [targetColumnFor] and the mirror-fallback rule apply unchanged,
     * one level up. Each block after the first relates to the *accumulated* shape of every
     * block before it (running max right edge, running total height), not just the immediately
     * preceding block alone — the working model for "the balancing is spread out" across three
     * or more blocks, while column-targeting itself anchors off the nearest (most recent)
     * block, since that is what §11.5's tree was already built to read.
     */
    fun solvePage(
        blocks: List<List<ConveyDesignLine>>,
        fullWidth: Float,
        baseSizeSp: Float = DEFAULT_BASE_SIZE_SP,
        ratio: Float = DEFAULT_RATIO,
        measure: ConveyDesignMeasure = { text2, size2, condensation2, tracking2 -> naturalWidth(text2, size2, condensation2, tracking2) },
    ): List<List<ConveyDesignSolvedLine>> {
        if (blocks.isEmpty()) return emptyList()

        val solvedBlocks = ArrayList<List<ConveyDesignSolvedLine>>(blocks.size)
        var referenceBlock = solveBlock(blocks[0], fullWidth, baseSizeSp, ratio, measure)
        solvedBlocks.add(referenceBlock)
        var accumulatedRightEdge = referenceBlock.maxOf { it.column.end }
        var accumulatedHeight = referenceBlock.sumOf { it.axes.fontSizeSp.toDouble() }.toFloat()

        for (i in 1 until blocks.size) {
            val blockLines = blocks[i]
            val spansFull = accumulatedRightEdge >= fullWidth * 0.999f

            var solved = if (spansFull) {
                solveBlock(blockLines, fullWidth, baseSizeSp, ratio, measure)
            } else {
                val anchorLine = referenceBlock.first().line
                val anchorColumn = referenceBlock.first().column
                val target = targetColumnFor(anchorLine, anchorColumn, fullWidth)
                val tooNarrow = target == null || target.width < fullWidth * 0.15f
                if (tooNarrow) {
                    val mirroredColumn = boundingColumn(referenceBlock).let { ConveyDesignColumn(fullWidth - it.end, fullWidth - it.start) }
                    solveBlockWithinColumn(blockLines, mirroredColumn, baseSizeSp, ratio, measure).map { it.copy(mirrored = true) }
                } else {
                    solveBlockWithinColumn(blockLines, target!!, baseSizeSp, ratio, measure)
                }
            }

            // Height-balancing: a block with fewer lines than its reference scales its lines'
            // sizes so its own total height approaches (never forced exactly to) the accumulated
            // height so far -- the same hierarchy-pull idea used within a block, one level up.
            if (blockLines.size < referenceBlock.size) {
                val ownHeight = solved.sumOf { it.axes.fontSizeSp.toDouble() }.toFloat()
                val targetHeight = accumulatedHeight / solvedBlocks.size
                if (ownHeight > 0f) {
                    val scale = (targetHeight / ownHeight).coerceIn(0.6f, 1.8f)
                    solved = solved.map { it.copy(axes = it.axes.copy(fontSizeSp = it.axes.fontSizeSp * scale)) }
                }
            }

            solvedBlocks.add(solved)
            accumulatedRightEdge = maxOf(accumulatedRightEdge, solved.maxOf { it.column.end })
            accumulatedHeight += solved.sumOf { it.axes.fontSizeSp.toDouble() }.toFloat()
            referenceBlock = solved
        }

        return solvedBlocks
    }
}

/**
 * A real, [androidx.compose.ui.text.TextMeasurer]-backed [ConveyDesignMeasure]: binds one
 * [androidx.compose.ui.text.font.FontFamily] instance at `wdth=100` (Azrienoch's neutral width)
 * and varies only `fontSize`/`letterSpacing` per call -- both cheap, since neither needs the
 * `FontFamily` rebuilt -- for the size lever's binary search, which the solve order (§11.4)
 * tries first and is what most lines actually converge on. Condensation is applied afterward as
 * a linear scale correction on the real-measured width rather than a second, per-candidate real
 * measurement.
 *
 * That split is a genuine, honestly scoped limitation, not an oversight: rebuilding a
 * `FontFamily` at a different `wdth` per candidate would need [conveyTypeFontFamily]'s
 * underlying `Font()` resolved fresh each time, which is confirmed (via bytecode inspection of
 * the actual `org.jetbrains.compose.resources.Font(...)` signature this project's pinned Compose
 * Multiplatform version compiles against) to take a `Composer` parameter and resolve its font
 * bytes through an internal async cache -- it cannot run synchronously inside a solver's binary
 * search. Size becomes genuinely real measurement; condensation stays an approximation, now a
 * documented one layered on real data rather than synthetic from the first character.
 *
 * Returns width in dp-equivalent units (`pixels / density`) to stay in the same rough scale
 * `fullWidthSp`'s callers already pass — itself an approximation once `fontScale != 1`, since dp
 * and sp diverge exactly there; stated here rather than silently assumed away.
 */
@Composable
fun rememberConveyDesignMeasure(): ConveyDesignMeasure {
    val textMeasurer = rememberTextMeasurer()
    val baseFamily = conveyTypeFontFamily(ConveyTypeVariation(width = 100f))
    val density = LocalDensity.current
    return remember(textMeasurer, baseFamily, density) {
        { text: String, fontSizeSp: Float, condensation: Float, trackingSp: Float ->
            if (text.isEmpty()) {
                0f
            } else {
                val result = textMeasurer.measure(
                    text = text,
                    style = TextStyle(fontSize = fontSizeSp.sp, fontFamily = baseFamily, letterSpacing = trackingSp.sp),
                )
                val widthDpEquivalent = result.size.width.toFloat() / density.density
                widthDpEquivalent * (condensation / 100f)
            }
        }
    }
}

/** Builds the real Azrienoch [TextStyle] for one solved line's axes -- `wght`/`wdth` via [conveyTypeFontFamily], never a `graphicsLayer` approximation. */
@Composable
private fun conveyDesignTextStyle(axes: ConveyDesignAxes, color: Color): TextStyle = TextStyle(
    color = color,
    fontSize = axes.fontSizeSp.sp,
    fontFamily = conveyTypeFontFamily(ConveyTypeVariation(weight = axes.weight, width = axes.condensation)),
    letterSpacing = axes.trackingSp.sp,
)

@Composable
private fun ConveyDesignSolvedLineRow(solvedLine: ConveyDesignSolvedLine, color: Color) {
    val boxAlignment = when (solvedLine.line.alignment) {
        ConveyDesignAlignment.Left -> Alignment.CenterStart
        ConveyDesignAlignment.Right -> Alignment.CenterEnd
        ConveyDesignAlignment.Center -> Alignment.Center
        ConveyDesignAlignment.Justify -> Alignment.CenterStart
    }
    val style = conveyDesignTextStyle(solvedLine.axes, color)

    Box(Modifier.fillMaxWidth(), contentAlignment = boxAlignment) {
        when {
            solvedLine.line.isAct -> ConveyActText(
                text = solvedLine.line.text,
                onClick = solvedLine.line.onClick ?: {},
                style = style,
            )
            solvedLine.line.motion == ConveyDesignMotion.Kinetic -> ConveyKineticText(
                text = solvedLine.line.text,
                idle = solvedLine.line.idle,
                style = style,
            )
            solvedLine.line.motion == ConveyDesignMotion.Sentence -> ConveyKineticSentence(
                text = solvedLine.line.text,
                style = style,
            )
            else -> androidx.compose.material3.Text(text = solvedLine.line.text, style = style)
        }
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
    measure: ConveyDesignMeasure = rememberConveyDesignMeasure(),
) {
    val solved = remember(lines, fullWidthSp, baseSizeSp, ratio, measure) {
        ConveyDesignSolver.solveBlock(lines, fullWidthSp, baseSizeSp, ratio, measure)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        for (solvedLine in solved) {
            ConveyDesignSolvedLineRow(solvedLine, color)
        }
    }
}

/**
 * Renders multiple [ConveyDesign] blocks as one page/screen, solved together per §11.7 — see
 * [ConveyDesignSolver.solvePage].
 */
@Composable
fun ConveyDesignPage(
    blocks: List<List<ConveyDesignLine>>,
    fullWidthSp: Float,
    modifier: Modifier = Modifier,
    color: Color = ConveyColor.OnSurface,
    baseSizeSp: Float = ConveyDesignSolver.DEFAULT_BASE_SIZE_SP,
    ratio: Float = ConveyDesignSolver.DEFAULT_RATIO,
    blockSpacing: Dp = 24.dp,
    measure: ConveyDesignMeasure = rememberConveyDesignMeasure(),
) {
    val solvedBlocks = remember(blocks, fullWidthSp, baseSizeSp, ratio, measure) {
        ConveyDesignSolver.solvePage(blocks, fullWidthSp, baseSizeSp, ratio, measure)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(blockSpacing)) {
        for (solvedBlock in solvedBlocks) {
            Column(Modifier.fillMaxWidth()) {
                for (solvedLine in solvedBlock) {
                    ConveyDesignSolvedLineRow(solvedLine, color)
                }
            }
        }
    }
}
