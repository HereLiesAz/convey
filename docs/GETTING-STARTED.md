## Getting started

A working screen in about ten minutes, using several real composables together, plus one law
violated on purpose so you can see the enforcement actually fire. If you want the philosophy
first, read [the manifesto](https://github.com/HereLiesAz/Conveyance) or
[LIBRARY.md](../convey/src/commonMain/kotlin/compose/conveyance/LIBRARY.md) — this page assumes
you already want to see it work.

## Adding convey to your project

This isn't on Maven Central yet. Right now, the fastest path is building against source:

```kotlin
// settings.gradle.kts, in the app that wants to use it
includeBuild("../convey")
```

or, to depend on it like a normal published library from your local machine:

```bash
git clone https://github.com/HereLiesAz/convey
cd convey
./gradlew publishToMavenLocal
```

```kotlin
// your app's settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}
```

```kotlin
// your app's build.gradle.kts
dependencies {
    implementation("compose.conveyance:convey:1.0.0")
}
```

## The root: `ConveySystem`

Every Convey surface starts here. Without it, composables still render — they fall back to
[`ConveyGrammar.Default`](../convey/src/commonMain/kotlin/compose/conveyance/ConveyGrammar.kt)
and skip enforcement. With it, the contract is live: violations throw in debug builds.

```kotlin
@Composable
fun App() {
    ConveySystem {
        // your screen
    }
}
```

`ConveyGrammar.Default` already gives you eight meanings — `navigate`, `reveal`, `confirm`,
`dismiss`, `morph`, `load`, `error`, `delight` — which is not a coincidence: LIBRARY.md's own
compliance checklist caps a grammar at eight. Everything below uses the default; defining your
own grammar is one call away (see [the end of this page](#your-own-grammar)) and does not
require touching anything else.

## A first screen

Three real composables, wired together: a top bar, a card containing a list, and an offered act.

```kotlin
ConveySystem {
    Column(Modifier.fillMaxSize()) {
        ConveyTopBar(
            title = { Text("Invoices") },
            titleWeight = ConveyWeight.Hero, // the screen's one defining thing
        )

        ConveyCard(modifier = Modifier.padding(16.dp)) {
            Column {
                invoices.forEach { invoice ->
                    ConveyListItem(
                        title = { Text(invoice.client) },
                        subtitle = { Text(invoice.dueLabel) },
                        trailing = { Text(invoice.amountLabel) },
                        onClick = { openInvoice(invoice) },
                    )
                }
            }
        }

        var phase by remember { mutableStateOf(ConveyOfferPhase.Invite) }
        ConveyOffer(
            purpose = "Send the selected invoices",
            weight = ConveyWeight.Primary,
            phase = phase,
            onInvoke = {
                scope.launch {
                    phase = ConveyOfferPhase.Progress
                    phase = if (sendInvoices()) ConveyOfferPhase.Success else ConveyOfferPhase.Failure
                }
            },
            modifier = Modifier.padding(16.dp),
        ) { p ->
            when (p) {
                ConveyOfferPhase.Invite, ConveyOfferPhase.Failure -> Text("Send")
                ConveyOfferPhase.Progress -> CircularProgressIndicator(Modifier.size(20.dp))
                ConveyOfferPhase.Success -> Icon(Icons.Default.Check, contentDescription = null)
                ConveyOfferPhase.Interrupted -> Text("Cancelled")
            }
        }
    }
}
```

Run it. The send button is one element through invite, progress, and success — not three. That's
[`ConveyStateHost`](../convey/src/commonMain/kotlin/compose/conveyance/foundation/ConveyStateHost.kt)
underneath, which [`ConveyOffer`](../convey/src/commonMain/kotlin/compose/conveyance/foundation/ConveyOffer.kt)
renders through directly.

## Watch it refuse a second Hero

Add a second `ConveyWeight.Hero` anywhere on the same surface — say, on the "Send" button above:

```kotlin
ConveyOffer(
    weight = ConveyWeight.Hero, // <- was Primary
    // ...
)
```

Run it in a debug build. It throws:

```
CONVEY HIERARCHY VIOLATION: 2 Hero elements on one surface.
A surface with multiple heroes has no hero. Demote all but one to Primary.
The hero is the answer to: 'What is the single most important thing here?'
```

That's [`ConveyWeightRegistry`](../convey/src/commonMain/kotlin/compose/conveyance/ConveyWeight.kt)
— every `Modifier.conveyWeight()` call (including the one `ConveyTopBar`'s `titleWeight` and
every other weight-aware component in this library makes internally) registers into the same
ambient registry `ConveySystem` provides. There is no separate audit step to remember to run;
the constraint is checked on every composition, live. Put the weight back and it's gone.
`ConveyEmployment`'s Job-enum minimum (see LIBRARY.md) enforces the same way, on a different axis.

## Your own grammar

The default grammar is a reasonable starting point, not a mandate. Define your own and every
composable that reads `LocalConveyGrammar.current` — which is all of them — picks it up
automatically:

```kotlin
ConveySystem(
    grammar = rememberConveyGrammar {
        meaning("navigate", "User moved to a different place") {
            spring(stiffness = 400f, dampingRatio = 0.82f)
        }
        meaning("confirm", "System acknowledged user input") {
            spring(stiffness = 700f, dampingRatio = 0.68f)
        }
        meaning("dismiss", "Content left — its job is done") {
            tween(200, easing = FastOutLinearInEasing)
        }
        meaning("morph", "One element became another") {
            spring(stiffness = 300f, dampingRatio = 0.76f)
        }
        meaning("error", "Wrong. Stop. Attend to this.") {
            snap()
        }
    },
) {
    // your screen
}
```

Read it out loud once you've written it. If it doesn't sound like a description of what the
person using your product actually experiences, that's the signal to revise it — the grammar is
documentation the compiler enforces, not decoration.

## Where to go from here

- **[LIBRARY.md](../convey/src/commonMain/kotlin/compose/conveyance/LIBRARY.md)** — the full
  package map: every composable in the library, what it replaces, and why. Start here for
  anything not covered above — `ConveyEscort`, `ConveyReversal`, `ConveyMigration`,
  `ConveyEnter`, the chip/badge/avatar/switch/segmented-control/navigation-bar catalog, and the
  WordNet+VerbNet-backed kinetic typography layer (`ConveyKineticText`, `ConveySvoScene`).
- **[AGENTS.md](../AGENTS.md)** — module layout, build requirements, and the dev-loop tooling
  (`dev-app`'s live Compose Hot Reload, `android-dev-app` + `hotswap` for on-device iteration).
