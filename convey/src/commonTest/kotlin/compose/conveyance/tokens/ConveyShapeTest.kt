package compose.conveyance.tokens

import kotlin.test.Test
import kotlin.test.assertEquals

class ConveyShapeTest {

    @Test
    fun escalateMovesOneStepTowardCircle() {
        assertEquals(ConveyShape.Small, ConveyShape.escalate(ConveyShape.XSmall))
        assertEquals(ConveyShape.Circle, ConveyShape.escalate(ConveyShape.Squircle))
    }

    @Test
    fun escalatingCircleStaysCircle() {
        assertEquals(ConveyShape.Circle, ConveyShape.escalate(ConveyShape.Circle))
    }

    @Test
    fun escalatingAShapeOutsideTheScaleReturnsCircle() {
        assertEquals(ConveyShape.Circle, ConveyShape.escalate(ConveyShape.Cut))
    }

    @Test
    fun deescalateMovesOneStepTowardNone() {
        assertEquals(ConveyShape.Small, ConveyShape.deescalate(ConveyShape.Medium))
        assertEquals(ConveyShape.None, ConveyShape.deescalate(ConveyShape.XSmall))
    }

    @Test
    fun deescalatingNoneStaysNone() {
        assertEquals(ConveyShape.None, ConveyShape.deescalate(ConveyShape.None))
    }

    @Test
    fun deescalatingAShapeOutsideTheScaleReturnsNone() {
        assertEquals(ConveyShape.None, ConveyShape.deescalate(ConveyShape.CutSmall))
    }

    @Test
    fun scaleIsOrderedFromNoneToCircle() {
        assertEquals(ConveyShape.None, ConveyShape.scale.first())
        assertEquals(ConveyShape.Circle, ConveyShape.scale.last())
    }
}
