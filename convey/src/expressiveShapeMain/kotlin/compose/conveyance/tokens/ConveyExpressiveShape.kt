package compose.conveyance.tokens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.collection.FloatFloatPair
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.PointTransformer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The real Material 3 Expressive 35-polygon [MaterialShapes](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialShapes)
 * vocabulary, ported directly from Google's own AOSP source
 * ([`MaterialShapes.kt`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialShapes.kt),
 * Apache 2.0 -- see `docs/THIRD_PARTY_NOTICES.md`) rather than depended on through the
 * `androidx.compose.material3` artifact directly: that artifact is pinned far ahead of this
 * project's own Compose/Kotlin versions (it ships M3 Expressive as an alpha feature current
 * only on a much newer toolchain than convey's proven pin), and pulling in a second, differently
 * -versioned material3 alongside the JetBrains Compose Multiplatform `compose.material3` this
 * module already depends on risks real classpath conflicts. This file depends only on
 * `androidx.graphics:graphics-shapes` (the lower-level, genuinely Kotlin-Multiplatform geometry
 * library `MaterialShapes` itself is built on -- verified to publish real `androidJvm`/`jvm`/
 * `wasmJs`/`iosArm64`/`iosSimulatorArm64`/`iosX64` variants at 1.1.0+, and to actually compile
 * against `:convey:compileKotlinDesktop`/`compileDebugKotlinAndroid`/`compileKotlinWasmJs` in
 * this repo), so every one of convey's targets gets the real shapes with no version bump.
 *
 * These are deliberate parity with [conveyance-expressive](https://github.com/HereLiesAz/conveyance-expressive)'s
 * own `ExpressiveSurface` (which *can* depend on the real `androidx.compose.material3` artifact
 * directly, since it only targets android+desktop on a newer, already-bumped toolchain) -- same
 * shape vocabulary, same exact geometry, reached by a different, more conservative dependency
 * path so it works on every platform this project actually ships.
 *
 * Every [RoundedPolygon] here is `.normalized()`, matching `MaterialShapes`'s own contract.
 * Use [ConveyExpressiveShape.shapeOf] to get a static (non-morphing) Compose [Shape] for direct
 * use as a `Modifier.clip`/background shape; a real morph between two of these needs
 * `androidx.graphics.shapes.Morph` directly (not wrapped here -- this file provides the named
 * shape vocabulary, not the morphing engine, matching `ConveyMorph`'s own existing scope).
 */
object ConveyExpressiveShape {

    private val cornerRound15 = CornerRounding(radius = .15f)
    private val cornerRound20 = CornerRounding(radius = .2f)
    private val cornerRound30 = CornerRounding(radius = .3f)
    private val cornerRound50 = CornerRounding(radius = .5f)
    private val cornerRound100 = CornerRounding(radius = 1f)

    val circle: RoundedPolygon by lazy { RoundedPolygon.circle(numVertices = 10).normalized() }
    val square: RoundedPolygon by lazy { RoundedPolygon.rectangle(width = 1f, height = 1f, rounding = cornerRound30).normalized() }
    val slanted: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.926f, 0.970f), CornerRounding(0.189f, 0.811f)),
                PointNRound(Offset(-0.021f, 0.967f), CornerRounding(0.187f, 0.057f)),
            ),
            2,
        ).normalized()
    }
    val arch: RoundedPolygon by lazy {
        RoundedPolygon(
            numVertices = 4,
            perVertexRounding = listOf(cornerRound100, cornerRound100, cornerRound20, cornerRound20),
        ).rotatedDegrees(-135f).normalized()
    }
    val fan: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(1.004f, 1.000f), CornerRounding(0.148f, 0.417f)),
                PointNRound(Offset(0.000f, 1.000f), CornerRounding(0.151f)),
                PointNRound(Offset(0.000f, -0.003f), CornerRounding(0.148f)),
                PointNRound(Offset(0.978f, 0.020f), CornerRounding(0.803f)),
            ),
            1,
        ).normalized()
    }
    val arrow: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 0.892f), CornerRounding(0.313f)),
                PointNRound(Offset(-0.216f, 1.050f), CornerRounding(0.207f)),
                PointNRound(Offset(0.499f, -0.160f), CornerRounding(0.215f, 1.000f)),
                PointNRound(Offset(1.225f, 1.060f), CornerRounding(0.211f)),
            ),
            1,
        ).normalized()
    }
    val semiCircle: RoundedPolygon by lazy {
        RoundedPolygon.rectangle(
            width = 1.6f,
            height = 1f,
            perVertexRounding = listOf(cornerRound20, cornerRound20, cornerRound100, cornerRound100),
        ).normalized()
    }
    val oval: RoundedPolygon by lazy {
        RoundedPolygon.circle().scaled(1f, 0.64f).rotatedDegrees(-45f).normalized()
    }
    val pill: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.961f, 0.039f), CornerRounding(0.426f)),
                PointNRound(Offset(1.001f, 0.428f)),
                PointNRound(Offset(1.000f, 0.609f), CornerRounding(1.000f)),
            ),
            reps = 2,
            mirroring = true,
        ).normalized()
    }
    val triangle: RoundedPolygon by lazy {
        RoundedPolygon(numVertices = 3, rounding = cornerRound20).rotatedDegrees(-90f).normalized()
    }
    val diamond: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 1.096f), CornerRounding(0.151f, 0.524f)),
                PointNRound(Offset(0.040f, 0.500f), CornerRounding(0.159f)),
            ),
            2,
        ).normalized()
    }
    val clamShell: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.171f, 0.841f), CornerRounding(0.159f)),
                PointNRound(Offset(-0.020f, 0.500f), CornerRounding(0.140f)),
                PointNRound(Offset(0.170f, 0.159f), CornerRounding(0.159f)),
            ),
            2,
        ).normalized()
    }
    val pentagon: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, -0.009f), CornerRounding(0.172f)),
                PointNRound(Offset(1.030f, 0.365f), CornerRounding(0.164f)),
                PointNRound(Offset(0.828f, 0.970f), CornerRounding(0.169f)),
            ),
            reps = 1,
            mirroring = true,
        ).normalized()
    }
    val gem: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.499f, 1.023f), CornerRounding(0.241f, 0.778f)),
                PointNRound(Offset(-0.005f, 0.792f), CornerRounding(0.208f)),
                PointNRound(Offset(0.073f, 0.258f), CornerRounding(0.228f)),
                PointNRound(Offset(0.433f, -0.000f), CornerRounding(0.491f)),
            ),
            1,
            mirroring = true,
        ).normalized()
    }
    val sunny: RoundedPolygon by lazy {
        RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = .8f, rounding = cornerRound15).normalized()
    }
    val verySunny: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 1.080f), CornerRounding(0.085f)),
                PointNRound(Offset(0.358f, 0.843f), CornerRounding(0.085f)),
            ),
            8,
        ).normalized()
    }
    val cookie4Sided: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(1.237f, 1.236f), CornerRounding(0.258f)),
                PointNRound(Offset(0.500f, 0.918f), CornerRounding(0.233f)),
            ),
            4,
        ).normalized()
    }
    val cookie6Sided: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.723f, 0.884f), CornerRounding(0.394f)),
                PointNRound(Offset(0.500f, 1.099f), CornerRounding(0.398f)),
            ),
            6,
        ).normalized()
    }
    val cookie7Sided: RoundedPolygon by lazy {
        RoundedPolygon.star(numVerticesPerRadius = 7, innerRadius = .75f, rounding = cornerRound50)
            .rotatedDegrees(-90f).normalized()
    }
    val cookie9Sided: RoundedPolygon by lazy {
        RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = .8f, rounding = cornerRound50)
            .rotatedDegrees(-90f).normalized()
    }
    val cookie12Sided: RoundedPolygon by lazy {
        RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = .8f, rounding = cornerRound50)
            .rotatedDegrees(-90f).normalized()
    }
    val ghostish: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 0f), CornerRounding(1.000f)),
                PointNRound(Offset(1f, 0f), CornerRounding(1.000f)),
                PointNRound(Offset(1f, 1.140f), CornerRounding(0.254f, 0.106f)),
                PointNRound(Offset(0.575f, 0.906f), CornerRounding(0.253f)),
            ),
            reps = 1,
            mirroring = true,
        ).normalized()
    }
    val clover4Leaf: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 0.074f)),
                PointNRound(Offset(0.725f, -0.099f), CornerRounding(0.476f)),
            ),
            reps = 4,
            mirroring = true,
        ).normalized()
    }
    val clover8Leaf: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 0.036f)),
                PointNRound(Offset(0.758f, -0.101f), CornerRounding(0.209f)),
            ),
            reps = 8,
        ).normalized()
    }
    val burst: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, -0.006f), CornerRounding(0.006f)),
                PointNRound(Offset(0.592f, 0.158f), CornerRounding(0.006f)),
            ),
            reps = 12,
        ).normalized()
    }
    val softBurst: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.193f, 0.277f), CornerRounding(0.053f)),
                PointNRound(Offset(0.176f, 0.055f), CornerRounding(0.053f)),
            ),
            reps = 10,
        ).normalized()
    }
    val boom: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.457f, 0.296f), CornerRounding(0.007f)),
                PointNRound(Offset(0.500f, -0.051f), CornerRounding(0.007f)),
            ),
            reps = 15,
        ).normalized()
    }
    val softBoom: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.733f, 0.454f)),
                PointNRound(Offset(0.839f, 0.437f), CornerRounding(0.532f)),
                PointNRound(Offset(0.949f, 0.449f), CornerRounding(0.439f, 1.000f)),
                PointNRound(Offset(0.998f, 0.478f), CornerRounding(0.174f)),
            ),
            reps = 16,
            mirroring = true,
        ).normalized()
    }
    val flower: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.370f, 0.187f)),
                PointNRound(Offset(0.416f, 0.049f), CornerRounding(0.381f)),
                PointNRound(Offset(0.479f, 0.001f), CornerRounding(0.095f)),
            ),
            reps = 8,
            mirroring = true,
        ).normalized()
    }
    val puffy: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 0.053f)),
                PointNRound(Offset(0.545f, -0.040f), CornerRounding(0.405f)),
                PointNRound(Offset(0.670f, -0.035f), CornerRounding(0.426f)),
                PointNRound(Offset(0.717f, 0.066f), CornerRounding(0.574f)),
                PointNRound(Offset(0.722f, 0.128f)),
                PointNRound(Offset(0.777f, 0.002f), CornerRounding(0.360f)),
                PointNRound(Offset(0.914f, 0.149f), CornerRounding(0.660f)),
                PointNRound(Offset(0.926f, 0.289f), CornerRounding(0.660f)),
                PointNRound(Offset(0.881f, 0.346f)),
                PointNRound(Offset(0.940f, 0.344f), CornerRounding(0.126f)),
                PointNRound(Offset(1.003f, 0.437f), CornerRounding(0.255f)),
            ),
            reps = 2,
            mirroring = true,
        ).scaled(1f, 0.742f).normalized()
    }
    val puffyDiamond: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.870f, 0.130f), CornerRounding(0.146f)),
                PointNRound(Offset(0.818f, 0.357f)),
                PointNRound(Offset(1.000f, 0.332f), CornerRounding(0.853f)),
            ),
            reps = 4,
            mirroring = true,
        ).normalized()
    }
    val pixelCircle: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 0.000f)),
                PointNRound(Offset(0.704f, 0.000f)),
                PointNRound(Offset(0.704f, 0.065f)),
                PointNRound(Offset(0.843f, 0.065f)),
                PointNRound(Offset(0.843f, 0.148f)),
                PointNRound(Offset(0.926f, 0.148f)),
                PointNRound(Offset(0.926f, 0.296f)),
                PointNRound(Offset(1.000f, 0.296f)),
            ),
            reps = 2,
            mirroring = true,
        ).normalized()
    }
    val pixelTriangle: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.110f, 0.500f)),
                PointNRound(Offset(0.113f, 0.000f)),
                PointNRound(Offset(0.287f, 0.000f)),
                PointNRound(Offset(0.287f, 0.087f)),
                PointNRound(Offset(0.421f, 0.087f)),
                PointNRound(Offset(0.421f, 0.170f)),
                PointNRound(Offset(0.560f, 0.170f)),
                PointNRound(Offset(0.560f, 0.265f)),
                PointNRound(Offset(0.674f, 0.265f)),
                PointNRound(Offset(0.675f, 0.344f)),
                PointNRound(Offset(0.789f, 0.344f)),
                PointNRound(Offset(0.789f, 0.439f)),
                PointNRound(Offset(0.888f, 0.439f)),
            ),
            reps = 1,
            mirroring = true,
        ).normalized()
    }
    val bun: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.796f, 0.500f)),
                PointNRound(Offset(0.853f, 0.518f), CornerRounding(1f)),
                PointNRound(Offset(0.992f, 0.631f), CornerRounding(1f)),
                PointNRound(Offset(0.968f, 1.000f), CornerRounding(1f)),
            ),
            reps = 2,
            mirroring = true,
        ).normalized()
    }
    val heart: RoundedPolygon by lazy {
        customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 0.268f), CornerRounding(0.016f)),
                PointNRound(Offset(0.792f, -0.066f), CornerRounding(0.958f)),
                PointNRound(Offset(1.064f, 0.276f), CornerRounding(1.000f)),
                PointNRound(Offset(0.501f, 0.946f), CornerRounding(0.129f)),
            ),
            reps = 1,
            mirroring = true,
        ).normalized()
    }

    /** Kept as friendlier aliases, matching `conveyance-expressive`'s own `ExpressiveSurface`. */
    val badge: RoundedPolygon get() = circle
    val bloom: RoundedPolygon get() = clover4Leaf
    val spark: RoundedPolygon get() = sunny
    val cookie: RoundedPolygon get() = cookie9Sided

    private val byNameMap: Map<String, () -> RoundedPolygon> by lazy {
        mapOf(
            "circle" to { circle }, "square" to { square }, "slanted" to { slanted }, "arch" to { arch },
            "fan" to { fan }, "arrow" to { arrow }, "semiCircle" to { semiCircle }, "oval" to { oval },
            "pill" to { pill }, "triangle" to { triangle }, "diamond" to { diamond }, "clamShell" to { clamShell },
            "pentagon" to { pentagon }, "gem" to { gem }, "sunny" to { sunny }, "verySunny" to { verySunny },
            "cookie4Sided" to { cookie4Sided }, "cookie6Sided" to { cookie6Sided },
            "cookie7Sided" to { cookie7Sided }, "cookie9Sided" to { cookie9Sided },
            "cookie12Sided" to { cookie12Sided }, "ghostish" to { ghostish },
            "clover4Leaf" to { clover4Leaf }, "clover8Leaf" to { clover8Leaf }, "burst" to { burst },
            "softBurst" to { softBurst }, "boom" to { boom }, "softBoom" to { softBoom }, "flower" to { flower },
            "puffy" to { puffy }, "puffyDiamond" to { puffyDiamond }, "pixelCircle" to { pixelCircle },
            "pixelTriangle" to { pixelTriangle }, "bun" to { bun }, "heart" to { heart },
            "badge" to { badge }, "bloom" to { bloom }, "spark" to { spark }, "cookie" to { cookie },
        )
    }

    /** Looks up a polygon by name (matching M3's own constant names, lowercased-first-letter). */
    fun byName(name: String): RoundedPolygon = byNameMap[name]?.invoke() ?: circle

    /** [byName] converted to a static Compose [Shape], for a non-morphing element. */
    fun shapeOf(name: String): Shape = ConveyPolygonShape(byName(name))

    private fun RoundedPolygon.rotatedDegrees(degrees: Float): RoundedPolygon {
        val radians = degrees / 180f * PI.toFloat()
        val cosA = cos(radians)
        val sinA = sin(radians)
        val cx = centerX
        val cy = centerY
        return transformed(PointTransformer { x, y ->
            val dx = x - cx
            val dy = y - cy
            FloatFloatPair(cx + dx * cosA - dy * sinA, cy + dx * sinA + dy * cosA)
        })
    }

    private fun RoundedPolygon.scaled(sx: Float, sy: Float): RoundedPolygon =
        transformed(PointTransformer { x, y -> FloatFloatPair(x * sx, y * sy) })

    private data class PointNRound(val o: Offset, val r: CornerRounding = CornerRounding.Unrounded)

    private fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero): Offset {
        val a = angle / 360f * 2 * PI.toFloat()
        val off = this - center
        return Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
    }

    private fun Offset.angleDegrees(): Float = kotlin.math.atan2(y, x) * 180f / PI.toFloat()

    private fun doRepeat(points: List<PointNRound>, reps: Int, center: Offset, mirroring: Boolean): List<PointNRound> =
        if (mirroring) {
            buildList {
                val angles = points.map { (it.o - center).angleDegrees() }
                val distances = points.map { (it.o - center).getDistance() }
                val actualReps = reps * 2
                val sectionAngle = 360f / actualReps
                repeat(actualReps) { it2 ->
                    points.indices.forEach { index ->
                        val i = if (it2 % 2 == 0) index else points.lastIndex - index
                        if (i > 0 || it2 % 2 == 0) {
                            val a = (sectionAngle * it2 + if (it2 % 2 == 0) angles[i] else sectionAngle - angles[i] + 2 * angles[0]) / 360f * 2 * PI.toFloat()
                            val finalPoint = Offset(cos(a), sin(a)) * distances[i] + center
                            add(PointNRound(finalPoint, points[i].r))
                        }
                    }
                }
            }
        } else {
            val np = points.size
            (0 until np * reps).map {
                val point = points[it % np].o.rotateDegrees((it / np) * 360f / reps, center)
                PointNRound(point, points[it % np].r)
            }
        }

    private fun customPolygon(
        pnr: List<PointNRound>,
        reps: Int,
        center: Offset = Offset(0.5f, 0.5f),
        mirroring: Boolean = false,
    ): RoundedPolygon {
        val actualPoints = doRepeat(pnr, reps, center, mirroring)
        return RoundedPolygon(
            vertices = FloatArray(actualPoints.size * 2) { ix ->
                actualPoints[ix / 2].o.let { if (ix % 2 == 0) it.x else it.y }
            },
            perVertexRounding = actualPoints.map { it.r },
            centerX = center.x,
            centerY = center.y,
        )
    }
}

/** Builds [path] from a Bezier-cubic outline (as returned by [RoundedPolygon.cubics]), rewound and closed. */
private fun pathFromCubics(path: Path, cubics: List<Cubic>): Path {
    path.rewind()
    cubics.forEachIndexed { index, cubic ->
        if (index == 0) path.moveTo(cubic.anchor0X, cubic.anchor0Y)
        path.cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y,
        )
    }
    path.close()
    return path
}

/** A static (non-morphing) [Shape] for one [RoundedPolygon], scaled to fill its assigned box. */
private class ConveyPolygonShape(polygon: RoundedPolygon) : Shape {
    private val shapePath: Path = pathFromCubics(Path(), polygon.cubics)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply { addPath(shapePath) }
        path.transform(Matrix().apply { scale(x = size.width, y = size.height) })
        // Recenter on the box rather than assuming the scaled path already sits at (0,0):
        // MaterialShapes polygons are `.normalized()`-ed into (0,0)-(1,1), but not perfectly
        // centered there for every one of the 35 shapes.
        val bounds = path.getBounds()
        path.translate(Offset(size.width / 2f - bounds.center.x, size.height / 2f - bounds.center.y))
        return Outline.Generic(path)
    }
}
