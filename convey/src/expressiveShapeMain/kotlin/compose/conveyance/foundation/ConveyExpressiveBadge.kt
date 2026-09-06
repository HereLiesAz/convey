package compose.conveyance.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import compose.conveyance.ConveyWeight
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyExpressiveShape
import compose.conveyance.tokens.ConveyExpressiveType
import compose.conveyance.tokens.ConveyPolygonShape
import compose.conveyance.tokens.conveyExpressiveType
import compose.conveyance.tokens.step

/**
 * A static [ConveyExpressiveShape]-clipped badge, colored by [weight] — the polygon-shape
 * counterpart to [ConveyBadge]'s circular dot/count indicator. Ported from
 * [conveyance-expressive](https://github.com/HereLiesAz/conveyance-expressive)'s own
 * `ShapeBadge` template (`expressive.badge.shape`), re-parametrized against this library's own
 * [ConveyWeight]/[ConveyColor] vocabulary rather than the `ComposableRequest`/`rank`-string
 * indirection that library's `.azp` composable-manifest system calls for — convey composables
 * take direct Kotlin parameters, not a generic request object.
 *
 * @param shapeName A name [ConveyExpressiveShape.byName] resolves (`"cookie9Sided"`, `"heart"`,
 *   ...). Falls back to `circle` for an unrecognized name.
 */
@Composable
fun ConveyExpressiveBadge(
    label: String,
    shapeName: String,
    modifier: Modifier = Modifier,
    weight: ConveyWeight = ConveyWeight.Secondary,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    onClick: (() -> Unit)? = null,
) {
    val shape = ConveyPolygonShape(ConveyExpressiveShape.byName(shapeName))
    val container = ConveyColor.containerFor(weight)
    val content = ConveyColor.contentFor(weight)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = content, style = conveyExpressiveType().step("labelLarge"))
    }
}

private val PRIMARY_SIZE = 64.dp
private val ACCENT_SIZE = 40.dp

// The accent's own top-left, chosen so its *center* lands on the primary shape's bottom-right
// corner: half of it sits under the primary (the "peeking from behind" read), half genuinely
// extends past the primary's own footprint -- ported unchanged from conveyance-expressive's own
// CompoundBadge geometry.
private val ACCENT_OFFSET = PRIMARY_SIZE - ACCENT_SIZE / 2
private val COMPOUND_SIZE = ACCENT_OFFSET + ACCENT_SIZE

/** The [ConveyWeight] a [ConveyExpressiveCompoundBadge]'s accent shape borrows its color from —
 *  a true 3-cycle over Hero/Primary/Secondary, so the accent's resolved container always differs
 *  from the primary's own. Ghost has no distinct container of its own to cycle into, so it
 *  shares Secondary's slot. */
internal fun accentWeightFor(weight: ConveyWeight): ConveyWeight = when (weight) {
    ConveyWeight.Hero -> ConveyWeight.Primary
    ConveyWeight.Primary -> ConveyWeight.Secondary
    ConveyWeight.Secondary, ConveyWeight.Ghost -> ConveyWeight.Hero
}

/**
 * A compound badge: a smaller accent [ConveyExpressiveShape] polygon peeking from behind the
 * primary shape, drawn in a different [ConveyWeight]'s container than the primary's own — the
 * layered-shape composition M3 Expressive's own reference material uses rather than a single
 * polygon standing alone. Ported from `conveyance-expressive`'s own `CompoundBadge` template
 * (`expressive.badge.compound`). The accent is always `burst` (or `spark` when the primary shape
 * *is* `burst`, so the two are never identical) — a fixed choice, matching the original.
 */
@Composable
fun ConveyExpressiveCompoundBadge(
    label: String,
    shapeName: String,
    modifier: Modifier = Modifier,
    weight: ConveyWeight = ConveyWeight.Secondary,
) {
    val primaryShape = ConveyPolygonShape(ConveyExpressiveShape.byName(shapeName))
    val accentPolygon = if (shapeName == "burst") ConveyExpressiveShape.spark else ConveyExpressiveShape.burst
    val accentShape = ConveyPolygonShape(accentPolygon)
    val accentWeight = accentWeightFor(weight)

    Box(modifier = modifier.size(COMPOUND_SIZE), contentAlignment = Alignment.TopStart) {
        Box(
            modifier = Modifier
                .padding(start = ACCENT_OFFSET, top = ACCENT_OFFSET)
                .size(ACCENT_SIZE)
                .clip(accentShape)
                .background(ConveyColor.containerFor(accentWeight)),
        )
        Box(
            modifier = Modifier
                .size(PRIMARY_SIZE)
                .clip(primaryShape)
                .background(ConveyColor.containerFor(weight)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = ConveyColor.contentFor(weight),
                style = conveyExpressiveType().step("labelLarge"),
            )
        }
    }
}

/**
 * A rectangular [ConveyExpressiveShape]-clipped tile with [label] and an optional [subtitle]
 * beneath it — the same title+detail two-line form `conveyance-h2g2`'s own record tile offers,
 * in M3's real type scale. Ported from `conveyance-expressive`'s own `TitleTile` template
 * (`expressive.tile.title`).
 */
@Composable
fun ConveyExpressiveTile(
    label: String,
    shapeName: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    weight: ConveyWeight = ConveyWeight.Secondary,
    onClick: (() -> Unit)? = null,
) {
    val shape = ConveyPolygonShape(ConveyExpressiveShape.byName(shapeName))
    val content = ConveyColor.contentFor(weight)
    val type = conveyExpressiveType()
    Box(
        modifier = modifier
            .clip(shape)
            .background(ConveyColor.containerFor(weight))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        androidx.compose.foundation.layout.Column {
            Text(text = label, color = content, style = type.step("titleMedium"))
            subtitle?.let { Text(text = it, color = content, style = type.bodyMedium) }
        }
    }
}
