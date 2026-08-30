package compose.conveyance.foundation

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import compose.conveyance.*
import compose.conveyance.tokens.*

/**
 * The morphing FAB — the canonical Conveyance hero element.
 *
 * The Manifesto's central example: "A FAB expanding into a menu doesn't just look alive —
 * it shows you what just happened and where to go next."
 *
 * [ConveyFab] is not a FAB that spawns a bottom sheet. It IS the bottom sheet.
 * When expanded, the FAB's own bounds grow to contain the actions.
 * The user sees one element that contains different amounts of information.
 * The expansion teaches: "everything you saw is still here, and there is more."
 *
 * This is the difference between Conveyance and conventional modal overlays:
 * a modal replaces the surface. The ConveyFab extends it.
 * The user never loses track of where they were. The element teaches its own range.
 *
 * Implementation:
 * - Collapsed: Circle, [collapsedSize], shows [collapsedIcon]
 * - Expanded: [expandedShape], [expandedWidth] × [expandedHeight], shows [actions]
 * - Morphs use the "morph" grammar meaning between these two states
 * - Icon rotates to signal state (+ → × or custom)
 *
 * ```kotlin
 * var fabExpanded by remember { mutableStateOf(false) }
 *
 * ConveyFab(
 *     expanded = fabExpanded,
 *     onToggle = { fabExpanded = !fabExpanded },
 *     color = ConveyColor.Primary,
 *     contentColor = ConveyColor.OnPrimary,
 *     actions = listOf(
 *         ConveyFabAction("New note",    Icons.Default.Edit,     { createNote() }),
 *         ConveyFabAction("New folder",  Icons.Default.Folder,   { createFolder() }),
 *         ConveyFabAction("Import file", Icons.Default.Upload,   { importFile() }),
 *     ),
 * )
 * ```
 */
@Composable
fun ConveyFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    color: Color = ConveyColor.Primary,
    contentColor: Color = ConveyColor.OnPrimary,
    collapsedSize: Dp = ConveySize.Component.Fab,
    expandedWidth: Dp = 240.dp,
    expandedHeight: Dp = Dp.Unspecified,
    collapsedShape: Shape = ConveyShape.Circle,
    expandedShape: Shape = ConveyShape.ExtraLarge,
    grammar: ConveyGrammar = LocalConveyGrammar.current,
    modifier: Modifier = Modifier,
    collapsedIcon: @Composable () -> Unit,
    actions: List<ConveyFabAction> = emptyList(),
) {
    val morphSpec = grammar["morph"]

    val animatedWidth by animateFloatAsState(
        targetValue = if (expanded) expandedWidth.value else collapsedSize.value,
        animationSpec = morphSpec,
        label = "ConveyFab.width",
    )

    val animatedHeight by animateFloatAsState(
        targetValue = collapsedSize.value,
        animationSpec = morphSpec,
        label = "ConveyFab.height",
    )

    val morphShape = rememberAnimatedMorphShape(
        targetShape = if (expanded) expandedShape else collapsedShape,
        spec = morphSpec,
    )

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f),
        label = "ConveyFab.color",
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = morphSpec,
        label = "ConveyFab.iconRotation",
    )

    val actionsAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (expanded)
            tween(durationMillis = 200, delayMillis = 150, easing = LinearOutSlowInEasing)
        else
            tween(durationMillis = 100, easing = FastOutLinearInEasing),
        label = "ConveyFab.actionsAlpha",
    )

    Box(
        modifier = modifier
            .size(width = animatedWidth.dp, height = animatedHeight.dp)
            .clip(morphShape)
            .drawBehind { drawRect(animatedColor) }
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(actionsAlpha),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                actions.forEachIndexed { index, action ->
                    FabActionRow(
                        action = action,
                        contentColor = contentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action.onClick(); onToggle() }
                            .padding(horizontal = ConveySize.Medium, vertical = ConveySize.Small),
                    )
                    if (index < actions.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ConveySize.Hairline)
                                .alpha(0.15f)
                                .drawBehind { drawRect(contentColor) },
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.graphicsLayer { rotationZ = iconRotation },
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    collapsedIcon()
                }
            }
        }
    }
}

@Composable
private fun FabActionRow(
    action: ConveyFabAction,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ConveySize.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            action.icon?.let { icon ->
                Box(
                    modifier = Modifier.size(ConveySize.Component.IconMedium),
                    contentAlignment = Alignment.Center,
                ) { icon() }
            }
        }
    }
}

/**
 * A single action inside an expanded [ConveyFab].
 *
 * @param label What this action does. A verb phrase. Short.
 * @param icon Optional composable icon. Use [androidx.compose.material.icons].
 * @param onClick Called when the user selects this action. The FAB will close automatically.
 */
data class ConveyFabAction(
    val label: String,
    val icon: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
)

// ── Utility ───────────────────────────────────────────────────────────────────

private fun Modifier.graphicsLayer(block: GraphicsLayerScope.() -> Unit): Modifier =
    this.then(Modifier.graphicsLayer(block))
