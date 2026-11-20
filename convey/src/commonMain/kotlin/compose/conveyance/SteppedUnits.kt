package com.hereliesaz.convey.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/**
 * Global configuration for 40dp stepped breakpoints and virtual viewport clamping.
 * Adjust bounds at startup if you want wider desktop expansion or tighter mobile clamping.
 */
object SteppedConfig {
    const val STEP_DP = 40f

    var minWidthDp: Float = 320f
    var maxWidthDp: Float = 840f
    var minHeightDp: Float = 480f
    var maxHeightDp: Float = 1000f
}

@Composable
fun steppedWindowWidth(): Dp {
    val rawWidthDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp().value
    }
    val clamped = rawWidthDp.coerceIn(SteppedConfig.minWidthDp, SteppedConfig.maxWidthDp)
    val snapped = floor(clamped / SteppedConfig.STEP_DP) * SteppedConfig.STEP_DP
    return snapped.dp
}

@Composable
fun steppedWindowHeight(): Dp {
    val rawHeightDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp().value
    }
    val clamped = rawHeightDp.coerceIn(SteppedConfig.minHeightDp, SteppedConfig.maxHeightDp)
    val snapped = floor(clamped / SteppedConfig.STEP_DP) * SteppedConfig.STEP_DP
    return snapped.dp
}

/** Percentage of the 40dp-stepped, clamped window width (e.g., 50.pw = 50% width). */
val Number.pw: Dp
    @Composable get() = steppedWindowWidth() * (this.toFloat() / 100f)

/** Percentage of the 40dp-stepped, clamped window height (e.g., 10.ph = 10% height). */
val Number.ph: Dp
    @Composable get() = steppedWindowHeight() * (this.toFloat() / 100f)

/** Text unit scaled by percentage of the 40dp-stepped width (e.g., 4.psp = 4% of width). */
val Number.psp: TextUnit
    @Composable get() = (steppedWindowWidth().value * (this.toFloat() / 100f)).sp

/** Percentage width clamped between [min] and [max] Dp bounds. */
@Composable
fun Number.pw(min: Dp = 0.dp, max: Dp = Dp.Infinity): Dp =
    this.pw.coerceIn(min, max)

/** Percentage height clamped between [min] and [max] Dp bounds. */
@Composable
fun Number.ph(min: Dp = 0.dp, max: Dp = Dp.Infinity): Dp =
    this.ph.coerceIn(min, max)

/** Percentage font size clamped between [min] and [max] TextUnit bounds. */
@Composable
fun Number.psp(min: TextUnit = 12.sp, max: TextUnit = 48.sp): TextUnit =
    this.psp.value.coerceIn(min.value, max.value).sp
