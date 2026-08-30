package compose.conveyance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.conveyance.foundation.ConveyTopographicalLayout
import compose.conveyance.tokens.ConveyColor
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.IIOImage
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders this module's own kinetic typography composables to real, headlessly-captured frames --
 * no display server needed (same technique Conveyance's own RenderMotion test uses). Every frame
 * comes from the actual compiled composables running against real WordNet/VerbNet-classified
 * input, not a mockup.
 */
class RenderKineticTypography {

    private val out = File("build/motion").apply { mkdirs() }
    private val density = 2f

    private fun film(
        name: String,
        width: Int,
        height: Int,
        frames: Int,
        content: @Composable () -> Unit,
        script: Camera.(Int) -> Unit = {},
    ) {
        val scene = ImageComposeScene(width = width, height = height, density = Density(density)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF14141C)).padding(24.dp)) {
                ConveySystem { content() }
            }
        }
        val camera = Camera(scene)
        val captured = mutableListOf<BufferedImage>()
        try {
            var nanos = 0L
            repeat(frames) { frame ->
                camera.script(frame)
                nanos += 33_000_000L
                val skia = scene.render(nanos)
                captured += ImageIO.read(skia.encodeToData()!!.bytes.inputStream())
            }
        } finally {
            scene.close()
        }
        writeGif(File(out, "$name.gif"), captured, delayMs = 33)
        assertTrue(File(out, "$name.gif").length() > 0, "$name produced no film")
    }

    private class Camera(val scene: ImageComposeScene) {
        fun tap(x: Float, y: Float) {
            scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
            scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
        }
    }

    @Test
    fun `kinetic text burst`() = film(
        "01-kinetic-text-burst",
        width = 500,
        height = 220,
        frames = 90,
        content = {
            var struck by remember { mutableStateOf(0) }
            ConveyKineticText(
                text = "CONVEY",
                idle = ConveyLife.Wobble(period = 4500L),
                triggerKey = struck,
                onClick = { struck++ },
                style = TextStyle(color = ConveyColor.OnSurface, fontSize = 40.sp),
                modifier = Modifier.padding(top = 20.dp),
            )
        },
        script = { frame -> if (frame == 45) tap(70f, 90f) },
    )

    @Test
    fun `kinetic sentence per-word verb motion`() = film(
        "02-kinetic-sentence",
        width = 1050,
        height = 180,
        frames = 70,
        content = {
            ConveyKineticSentence(
                text = "The cheetah sprints and pounces",
                style = TextStyle(color = ConveyColor.OnSurface, fontSize = 26.sp),
            )
        },
    )

    @Test
    fun `svo scene force dynamics`() = film(
        "03-svo-scene",
        width = 980,
        height = 320,
        frames = 130,
        content = {
            ConveySvoScene(
                sentence = "The cheetah hunts the gazelle",
                style = TextStyle(color = ConveyColor.OnSurface, fontSize = 22.sp),
                sceneWidth = 140.dp,
            )
        },
    )

    @Test
    fun `topographical layout by verb`() = film(
        "04-topographical-layout",
        width = 650,
        height = 950,
        frames = 1,
        content = {
            Column {
                ConveyTopographicalLayout(
                    text = "The leaves fell to the ground",
                    style = TextStyle(color = ConveyColor.OnSurface, fontSize = 18.sp),
                )
                ConveyTopographicalLayout(
                    text = "The balloon rose into the sky",
                    style = TextStyle(color = ConveyColor.OnSurface, fontSize = 18.sp),
                    modifier = Modifier.padding(top = 32.dp),
                )
                ConveyTopographicalLayout(
                    text = "The wolves surrounded the camp",
                    style = TextStyle(color = ConveyColor.OnSurface, fontSize = 18.sp),
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
        },
    )

    private fun writeGif(target: File, frames: List<BufferedImage>, delayMs: Int) {
        require(frames.isNotEmpty()) { "nothing to write" }
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        FileImageOutputStream(target).use { output ->
            writer.output = output
            val params = writer.defaultWriteParam
            val type = javax.imageio.ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB)
            val metadata = writer.getDefaultImageMetadata(type, params)
            val format = metadata.nativeMetadataFormatName

            val root = metadata.getAsTree(format) as IIOMetadataNode
            val control = IIOMetadataNode("GraphicControlExtension").apply {
                setAttribute("disposalMethod", "none")
                setAttribute("userInputFlag", "FALSE")
                setAttribute("transparentColorFlag", "FALSE")
                setAttribute("delayTime", (delayMs / 10).toString())
                setAttribute("transparentColorIndex", "0")
            }
            root.appendChild(control)
            val extensions = IIOMetadataNode("ApplicationExtensions")
            extensions.appendChild(
                IIOMetadataNode("ApplicationExtension").apply {
                    setAttribute("applicationID", "NETSCAPE")
                    setAttribute("authenticationCode", "2.0")
                    userObject = byteArrayOf(0x1, 0x0, 0x0)
                },
            )
            root.appendChild(extensions)
            metadata.setFromTree(format, root)

            writer.prepareWriteSequence(null)
            frames.forEach { frame ->
                val rgb = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB)
                rgb.createGraphics().apply { drawImage(frame, 0, 0, null); dispose() }
                writer.writeToSequence(IIOImage(rgb, null, metadata), params)
            }
            writer.endWriteSequence()
        }
        writer.dispose()
    }
}
