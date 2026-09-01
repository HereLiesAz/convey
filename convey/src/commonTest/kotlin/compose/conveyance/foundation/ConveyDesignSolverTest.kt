package compose.conveyance.foundation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConveyDesignSolverTest {

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, got $actual (tolerance $tolerance)")
    }

    @Test
    fun nominalSizeIsMonotonicAcrossLevels() {
        val sizes = ConveyDesignLevel.entries.associateWith { ConveyDesignSolver.nominalSize(it) }
        assertTrue(sizes[ConveyDesignLevel.Body]!! < sizes[ConveyDesignLevel.Header3]!!)
        assertTrue(sizes[ConveyDesignLevel.Header3]!! < sizes[ConveyDesignLevel.Header2]!!)
        assertTrue(sizes[ConveyDesignLevel.Header2]!! < sizes[ConveyDesignLevel.Header1]!!)
        assertTrue(sizes[ConveyDesignLevel.Header1]!! < sizes[ConveyDesignLevel.Title]!!)
    }

    @Test
    fun nominalSizeFallsOutOfOneModularScaleRatio() {
        val base = 16f
        val ratio = 1.333f
        assertEquals(base, ConveyDesignSolver.nominalSize(ConveyDesignLevel.Body, base, ratio))
        assertClose(base * ratio, ConveyDesignSolver.nominalSize(ConveyDesignLevel.Header3, base, ratio))
        assertClose(base * ratio * ratio * ratio * ratio, ConveyDesignSolver.nominalSize(ConveyDesignLevel.Title, base, ratio), tolerance = 0.05f)
    }

    @Test
    fun nominalWeightIsMonotonicAcrossLevels() {
        assertTrue(ConveyDesignSolver.nominalWeight(ConveyDesignLevel.Body) < ConveyDesignSolver.nominalWeight(ConveyDesignLevel.Header3))
        assertTrue(ConveyDesignSolver.nominalWeight(ConveyDesignLevel.Header1) < ConveyDesignSolver.nominalWeight(ConveyDesignLevel.Title))
    }

    @Test
    fun inkScoreScalesWithSquareOfFontSize() {
        val small = ConveyDesignSolver.inkScore("hello", 10f, 400f)
        val big = ConveyDesignSolver.inkScore("hello", 20f, 400f)
        // fontSize doubled -> inkScore should roughly quadruple.
        assertClose(4f, big / small, tolerance = 0.01f)
    }

    @Test
    fun inkScoreScalesWithStrokeWeightFactor() {
        val regular = ConveyDesignSolver.inkScore("hello", 16f, 400f)
        val bold = ConveyDesignSolver.inkScore("hello", 16f, 800f)
        assertClose(2f, bold / regular, tolerance = 0.01f)
    }

    @Test
    fun naturalWidthGrowsLinearlyWithFontSize() {
        val narrow = ConveyDesignSolver.naturalWidth("hello world", 10f)
        val wide = ConveyDesignSolver.naturalWidth("hello world", 20f)
        assertClose(2f, wide / narrow, tolerance = 0.01f)
    }

    @Test
    fun naturalWidthShrinksWithCondensation() {
        val normal = ConveyDesignSolver.naturalWidth("hello world", 16f, condensation = 100f)
        val condensed = ConveyDesignSolver.naturalWidth("hello world", 16f, condensation = 75f)
        assertTrue(condensed < normal)
    }

    @Test
    fun solveToWidthReachesACloseWidthWithinLeverBounds() {
        val nominal = ConveyDesignAxes(fontSizeSp = 16f, weight = 400f, condensation = 100f, trackingSp = 0f)
        val target = 180f
        val fit = ConveyDesignSolver.solveToWidth("a modest headline", nominal, target)
        assertTrue(fit != null)
        val actual = ConveyDesignSolver.naturalWidth("a modest headline", fit!!.fontSizeSp, fit.condensation, fit.trackingSp)
        assertTrue(abs(actual - target) <= target * 0.12f)
    }

    @Test
    fun solveBlockRendersASingleFreestandingLineAtNominalAxes() {
        val lines = listOf(ConveyDesignLine("Only line", ConveyDesignLevel.Title, ConveyDesignAlignment.Left))
        val solved = ConveyDesignSolver.solveBlock(lines, fullWidth = 400f)

        assertEquals(1, solved.size)
        assertEquals(ConveyDesignSolver.nominalSize(ConveyDesignLevel.Title), solved[0].axes.fontSizeSp)
        assertFalse(solved[0].mirrored)
    }

    @Test
    fun secondLineInheritsTheLeftoverColumnFromALeftAlignedDefiningLine() {
        // A short, left-aligned defining line leaves most of a wide block empty; the second
        // line should target that leftover column, not the full width.
        val lines = listOf(
            ConveyDesignLine("Co", ConveyDesignLevel.Header1, ConveyDesignAlignment.Left),
            ConveyDesignLine("A modest location name", ConveyDesignLevel.Body, ConveyDesignAlignment.Left),
        )
        val fullWidth = 500f
        val solved = ConveyDesignSolver.solveBlock(lines, fullWidth)

        val definingColumn = solved[0].column
        val second = solved[1]
        if (!second.mirrored) {
            assertClose(definingColumn.end, second.column.start)
            assertClose(fullWidth, second.column.end)
        }
    }

    @Test
    fun tooNarrowLeftoverColumnTriggersMirrorFallback() {
        // A wide, nearly-full-width defining line leaves almost no leftover column; a second
        // line with real content cannot fit it even at minimum size, so it must mirror instead.
        val lines = listOf(
            ConveyDesignLine(
                "A rather long title that spans most of the available width already",
                ConveyDesignLevel.Title,
                ConveyDesignAlignment.Left,
            ),
            ConveyDesignLine("Another full sentence of real content", ConveyDesignLevel.Body, ConveyDesignAlignment.Left),
        )
        val fullWidth = 320f
        val solved = ConveyDesignSolver.solveBlock(lines, fullWidth)

        val second = solved[1]
        assertTrue(second.mirrored)
        val definingColumn = solved[0].column
        assertClose(fullWidth - definingColumn.end, second.column.start)
        assertClose(fullWidth - definingColumn.start, second.column.end)
    }

    @Test
    fun justifiedDefiningLineMakesTheNextLineFillFullWidthToo() {
        val lines = listOf(
            ConveyDesignLine("A full-width justified headline here", ConveyDesignLevel.Title, ConveyDesignAlignment.Justify),
            ConveyDesignLine("Subtitle", ConveyDesignLevel.Body, ConveyDesignAlignment.Left),
        )
        val fullWidth = 400f
        val solved = ConveyDesignSolver.solveBlock(lines, fullWidth)

        assertEquals(0f, solved[0].column.start)
        assertEquals(fullWidth, solved[0].column.end)
        if (!solved[1].mirrored) {
            assertClose(0f, solved[1].column.start)
            assertClose(fullWidth, solved[1].column.end)
        }
    }

    @Test
    fun explicitColumnOverridesInheritance() {
        val explicit = ConveyDesignColumn(50f, 150f)
        val lines = listOf(
            ConveyDesignLine("Tag", ConveyDesignLevel.Header1, ConveyDesignAlignment.Left),
            ConveyDesignLine("Explicit", ConveyDesignLevel.Body, ConveyDesignAlignment.Left, explicitColumn = explicit),
        )
        val solved = ConveyDesignSolver.solveBlock(lines, fullWidth = 400f)

        if (!solved[1].mirrored) {
            assertEquals(explicit, solved[1].column)
        }
    }

    @Test
    fun solvePageMakesTheSecondBlockTreatTheFullScreenAsItsMeasureWhenTheFirstBlockDoesNotSpanIt() {
        // Block 1 is a short, left-aligned tagline that leaves most of the screen empty. Per
        // §11.7, block 2 should target that leftover the same way a second LINE would.
        val fullWidth = 500f
        val block1 = listOf(ConveyDesignLine("Co", ConveyDesignLevel.Header1, ConveyDesignAlignment.Left))
        val block2 = listOf(ConveyDesignLine("A modest location name", ConveyDesignLevel.Body, ConveyDesignAlignment.Left))

        val solved = ConveyDesignSolver.solvePage(listOf(block1, block2), fullWidth)
        assertEquals(2, solved.size)

        val block1RightEdge = solved[0].maxOf { it.column.end }
        val secondBlockFirstLine = solved[1].first()
        if (!secondBlockFirstLine.mirrored) {
            assertClose(secondBlockFirstLine.column.start, block1RightEdge)
        }
    }

    @Test
    fun solvePageMirrorsTheWholePriorBlockWhenTheLeftoverIsTooNarrow() {
        // Chosen so block 1's own natural width leaves a real but small (<15% of fullWidth)
        // leftover -- large enough that block 1 doesn't already span the full width outright
        // (which would take a different, non-mirroring path), small enough to trigger the
        // too-narrow mirror-fallback rather than the ordinary column-fill path.
        val fullWidth = 320f
        val block1 = listOf(ConveyDesignLine("A short line", ConveyDesignLevel.Title, ConveyDesignAlignment.Left))
        val block2 = listOf(ConveyDesignLine("Another full sentence of real content", ConveyDesignLevel.Body, ConveyDesignAlignment.Left))

        val solved = ConveyDesignSolver.solvePage(listOf(block1, block2), fullWidth)

        assertTrue(solved[1].first().mirrored)
    }

    @Test
    fun solvePageBalancesAShorterBlocksHeightTowardThePriorBlocksHeight() {
        val fullWidth = 400f
        val threeLineBlock = listOf(
            ConveyDesignLine("Tagline here", ConveyDesignLevel.Header2, ConveyDesignAlignment.Justify),
            ConveyDesignLine("Company Name", ConveyDesignLevel.Title, ConveyDesignAlignment.Justify),
            ConveyDesignLine("Location", ConveyDesignLevel.Body, ConveyDesignAlignment.Justify),
        )
        val twoLineBlock = listOf(
            ConveyDesignLine("Short", ConveyDesignLevel.Body, ConveyDesignAlignment.Justify),
            ConveyDesignLine("Two", ConveyDesignLevel.Body, ConveyDesignAlignment.Justify),
        )

        val solved = ConveyDesignSolver.solvePage(listOf(threeLineBlock, twoLineBlock), fullWidth)
        val threeLineHeight = solved[0].sumOf { it.axes.fontSizeSp.toDouble() }.toFloat()
        val twoLineHeightUnbalanced = twoLineBlock.sumOf { ConveyDesignSolver.nominalSize(it.level).toDouble() }.toFloat()
        val twoLineHeightBalanced = solved[1].sumOf { it.axes.fontSizeSp.toDouble() }.toFloat()

        // The balanced height should move closer to the three-line block's height than the
        // unbalanced nominal total was.
        assertTrue(abs(twoLineHeightBalanced - threeLineHeight) < abs(twoLineHeightUnbalanced - threeLineHeight))
    }

    @Test
    fun solvePageOnASingleBlockMatchesSolveBlock() {
        val fullWidth = 400f
        val lines = listOf(ConveyDesignLine("Only block", ConveyDesignLevel.Title, ConveyDesignAlignment.Left))
        val page = ConveyDesignSolver.solvePage(listOf(lines), fullWidth)
        val block = ConveyDesignSolver.solveBlock(lines, fullWidth)

        assertEquals(1, page.size)
        assertEquals(block, page[0])
    }
}
