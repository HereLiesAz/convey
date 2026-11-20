# Convey / Conveyance — Stepped Units & Azrienoch Kinetic Typography

Drop-in Compose Multiplatform files for `hereliesaz/Convey` and `conveyance`.

## Included Files

1. **`SteppedUnits.kt`** (`com.hereliesaz.convey.ui.theme`)
   - Snaps window dimensions into `40dp` adaptive breakpoints with configurable min/max clamping (`SteppedConfig`).
   - Provides `.pw`, `.ph`, and `.psp` percentage extension properties (plus `.pw(min, max)`, `.ph(min, max)`, and `.psp(min, max)` helpers).
2. **`AzrienochFont.kt`** (`com.hereliesaz.convey.ui.theme`)
   - Loads `azrienoch` via Compose Multiplatform `FontResource` with dynamic `FontVariation.Settings` and `axisSetting(tag, value)` support for custom 4-character OpenType axes.
3. **`AzrienochKineticText.kt`** (`com.hereliesaz.convey.ui.components`)
   - Combines `40dp`-stepped percentage sizing with a dual-layer max-bounds reservation box (`maxBoundsAxes` vs `activeAxes`) so variable font animations never rewrap lines mid-motion.

## Quick Setup

1. Place `azrienoch.ttf` (or `.otf`) in `composeApp/src/commonMain/composeResources/font/azrienoch.ttf`.
2. Drop the three `.kt` files into your shared `commonMain` source set (`com/hereliesaz/convey/ui/...`).
3. Use `AzrienochKineticText` with `Res.font.azrienoch`:

```kotlin
AzrienochKineticText(
    text = "Conveyance kinetic typography powered by Azrienoch.",
    fontResource = Res.font.azrienoch,
    activeAxes = listOf(
        FontVariation.weight(animatedWeight.toInt()),
        axisSetting("wdth", animatedWidth),
    ),
    maxBoundsAxes = listOf(
        FontVariation.weight(900),
        axisSetting("wdth", 125f),
    ),
    containerWidth = 90.pw,
    horizontalPadding = 4.pw,
    fontSize = 5.psp(min = 18.sp, max = 42.sp),
)
```
