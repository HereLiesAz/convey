package compose.conveyance.devapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import compose.conveyance.ConveyKineticSentence
import compose.conveyance.ConveySvoScene
import compose.conveyance.ConveySystem
import compose.conveyance.foundation.ConveyBody
import compose.conveyance.foundation.ConveyBodyLine
import compose.conveyance.foundation.ConveyBodyRole
import compose.conveyance.foundation.ConveyTopographicalLayout
import compose.conveyance.tokens.ConveyColor
import compose.conveyance.tokens.ConveyTypePreset
import compose.conveyance.tokens.conveyTypeFontFamily

/**
 * `./gradlew :android-dev-app:installDebug`, then the `:hotswap` tool (see its README) redefines
 * changed classes on this already-running process over JDWP and broadcasts [RELOAD_ACTION] to
 * force a full recomposition -- the on-device counterpart to `:dev-app:hotRunJvm`'s desktop Hot
 * Reload. Unlike JetBrains' Compose Hot Reload, this is not built into any tooling; it's this
 * repo's own mechanism, since Compose Hot Reload is explicitly desktop-only.
 */
const val RELOAD_ACTION = "compose.conveyance.devapp.RELOAD"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var generation by remember { mutableIntStateOf(0) }
            val context = LocalContext.current
            remember {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        generation++
                    }
                }
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(RELOAD_ACTION),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                receiver
            }

            // `key(generation)` forces Compose to throw away and re-run this whole subtree
            // (rather than merely recompose it) whenever a redefinition lands -- redefined method
            // bodies only take effect for *new* stack frames, so anything already-composed with
            // the old code needs to be freshly invoked to actually run the new bodies.
            key(generation) {
                ConveySystem {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF14141C))
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        Text(
                            text = "Azrienoch",
                            style = TextStyle(
                                color = ConveyColor.OnSurface,
                                fontSize = 40.sp,
                                fontFamily = conveyTypeFontFamily(ConveyTypePreset.Bold),
                            ),
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
                        ConveyBody(
                            lines = listOf(
                                ConveyBodyLine(
                                    "The cheetah sprints across the plain, chasing the fleeing gazelle with relentless speed.",
                                    ConveyBodyRole.Paragraph,
                                ),
                                ConveyBodyLine(
                                    "Speed and grace are not opposites; they are the same motion, seen from two directions.",
                                    ConveyBodyRole.Quote,
                                ),
                            ),
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            color = ConveyColor.OnSurface,
                        )
                    }
                }
            }
        }
    }
}
