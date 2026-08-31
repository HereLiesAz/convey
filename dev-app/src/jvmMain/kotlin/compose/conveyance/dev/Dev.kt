package compose.conveyance.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import compose.conveyance.ConveyKineticSentence
import compose.conveyance.ConveyKineticText
import compose.conveyance.ConveyLife
import compose.conveyance.ConveySvoScene
import compose.conveyance.ConveySystem
import compose.conveyance.foundation.ConveyTopographicalLayout
import compose.conveyance.tokens.ConveyColor

/**
 * `./gradlew :convey:hotRunDesktop` -- a live-reloadable window over the composables this file
 * exercises. Edit any of them (or this gallery itself) and save; Compose Hot Reload swaps the
 * changed code into the already-running JVM instead of restarting it. This is dev-only scaffolding
 * for iterating on the library, not part of what gets published -- see convey/build.gradle.kts's
 * own comment on the `compose.desktop.application` block this main() backs.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Convey -- kinetic typography (dev)") {
        ConveySystem {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF14141C))
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                var struck by remember { mutableStateOf(0) }
                ConveyKineticText(
                    text = "CONVEY",
                    idle = ConveyLife.Wobble(period = 4500L),
                    triggerKey = struck,
                    onClick = { struck++ },
                    style = TextStyle(color = ConveyColor.OnSurface, fontSize = 40.sp),
                )
                ConveyKineticSentence(
                    text = "The cheetah sprints and pounces",
                    style = TextStyle(color = ConveyColor.OnSurface, fontSize = 26.sp),
                )
                ConveySvoScene(
                    sentence = "The cheetah hunts the gazelle",
                    style = TextStyle(color = ConveyColor.OnSurface, fontSize = 22.sp),
                    sceneWidth = 140.dp,
                )
                ConveyTopographicalLayout(
                    text = "The leaves fell to the ground",
                    style = TextStyle(color = ConveyColor.OnSurface, fontSize = 18.sp),
                )
                ConveyTopographicalLayout(
                    text = "The balloon rose into the sky",
                    style = TextStyle(color = ConveyColor.OnSurface, fontSize = 18.sp),
                )
            }
        }
    }
}
