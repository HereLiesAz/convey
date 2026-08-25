package compose.conveyance

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConveyMorphTest {

    private val density = Density(1f)
    private val size = Size(100f, 100f)

    @Test
    fun interpolatedShapeAtZeroProgressIsExactlyTheFromShape() {
        val from = RoundedCornerShape(0.dp)
        val to = RoundedCornerShape(50)
        val shape = InterpolatedShape(from, to, progress = 0f)

        val expected = from.createOutline(size, LayoutDirection.Ltr, density)
        val actual = shape.createOutline(size, LayoutDirection.Ltr, density)

        assertEquals((expected as Outline.Rectangle).rect, (actual as Outline.Rectangle).rect)
    }

    @Test
    fun interpolatedShapeAtFullProgressIsExactlyTheToShape() {
        val from = RoundedCornerShape(0.dp)
        val to = RoundedCornerShape(50)
        val shape = InterpolatedShape(from, to, progress = 1f)

        val expected = to.createOutline(size, LayoutDirection.Ltr, density)
        val actual = shape.createOutline(size, LayoutDirection.Ltr, density)

        assertEquals((expected as Outline.Rounded).roundRect, (actual as Outline.Rounded).roundRect)
    }

    @Test
    fun interpolatedShapeMidwayProducesAGenericOutlineBetweenTheTwoBounds() {
        val from = RoundedCornerShape(0.dp)
        val to = RoundedCornerShape(50)
        val shape = InterpolatedShape(from, to, progress = 0.5f)

        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Generic
        val bounds = outline.path.getBounds()

        // The interpolated outline should stay within the sampled square's footprint.
        assertTrue(bounds.left >= 0f && bounds.top >= 0f)
        assertTrue(bounds.right <= size.width && bounds.bottom <= size.height)
    }

    @Test
    fun lerpPathsAtZeroFollowsTheFromPath() {
        val from = Path().apply { addRect(Rect(0f, 0f, 10f, 10f)) }
        val to = Path().apply { addRect(Rect(90f, 90f, 100f, 100f)) }

        val result = lerpPaths(from, to, t = 0f, sampleCount = 32)
        val bounds = result.getBounds()

        assertTrue(bounds.left < 20f && bounds.top < 20f)
    }

    @Test
    fun lerpPathsAtOneFollowsTheToPath() {
        val from = Path().apply { addRect(Rect(0f, 0f, 10f, 10f)) }
        val to = Path().apply { addRect(Rect(90f, 90f, 100f, 100f)) }

        val result = lerpPaths(from, to, t = 1f, sampleCount = 32)
        val bounds = result.getBounds()

        assertTrue(bounds.right > 80f && bounds.bottom > 80f)
    }

    @Test
    fun lerpPathsMidwaySitsBetweenBothEndpoints() {
        val from = Path().apply { addRect(Rect(0f, 0f, 10f, 10f)) }
        val to = Path().apply { addRect(Rect(90f, 90f, 100f, 100f)) }

        val result = lerpPaths(from, to, t = 0.5f, sampleCount = 32)
        val bounds = result.getBounds()

        assertTrue(bounds.left in 20f..80f)
        assertTrue(bounds.top in 20f..80f)
    }
}
