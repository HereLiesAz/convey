# Convey

A Compose Multiplatform design system built on the [Conveyance Manifesto](https://github.com/HereLiesAz/Conveyance).

Not a component kit. A philosophy, encoded as a type system.

```kotlin
ConveySystem {
    ConveyOffer(
        purpose = "Send the invoice to the client",
        weight = ConveyWeight.Primary,
        phase = phase,
        onInvoke = { scope.launch { send() } },
    ) { p ->
        when (p) {
            ConveyOfferPhase.Invite, ConveyOfferPhase.Failure -> Text("Send")
            ConveyOfferPhase.Progress -> CircularProgressIndicator()
            ConveyOfferPhase.Success -> Icon(Icons.Default.Check, contentDescription = null)
            ConveyOfferPhase.Interrupted -> Text("Cancelled")
        }
    }
}
```

One element. Four states. No spinner spawned beside it, no separate success toast — the button
*becomes* its own progress, then its own result.

## Why this exists

Most design systems give you tokens and components and leave the rules to a wiki page nobody
reads. Convey gives you the rules as compiler constraints instead:

- **Motion means something, or it doesn't exist.** Every animation runs through a declared
  [`ConveyGrammar`](convey/src/commonMain/kotlin/compose/conveyance/ConveyGrammar.kt) meaning.
  An undeclared meaning throws at the call site in debug builds — not at review time, not never.
- **Hierarchy is enforced, not suggested.** `Modifier.conveyWeight(ConveyWeight.Hero)` throws if
  a second Hero already exists on the surface. A screen with two heroes has no hero, and the
  system tells you that immediately instead of letting it ship.
- **Disabled controls, confirmation dialogs, and spinners don't exist here** — not omitted by
  accident, replaced on purpose by mechanisms that treat the person on the other end of the
  screen with more respect: a blocked action [escorts](convey/src/commonMain/kotlin/compose/conveyance/foundation/ConveyEscort.kt)
  you to what's blocking it instead of graying out; a destructive action collapses into an
  in-place, [reversible residue](convey/src/commonMain/kotlin/compose/conveyance/foundation/ConveyReversal.kt)
  instead of asking "are you sure?"; a busy element [deforms under its own load](convey/src/commonMain/kotlin/compose/conveyance/foundation/ConveyYield.kt)
  instead of spawning a spinner next to itself.

The full argument for why is in [the Manifesto itself](https://github.com/HereLiesAz/Conveyance);
the point-by-point rule → mechanism mapping for *this* implementation is in
[LIBRARY.md](convey/src/commonMain/kotlin/compose/conveyance/LIBRARY.md).

## A second, independent implementation

The Manifesto also has a reference SDK — `conveyance-core`/`conveyance-compose` in the
[Conveyance](https://github.com/HereLiesAz/Conveyance) repo, built around a different vocabulary
(`Act`/`Gate`/`Place` rather than `ConveyGrammar`/`ConveyWeight`/`ConveyOffer`). Convey is a
separate, independently-developed reading of the same manifesto — linked from that repo as a git
submodule, but its own codebase, its own history, its own decisions.

## Getting started

Ten minutes to a real screen, one law violated on purpose so you can see the enforcement fire:
**[docs/GETTING-STARTED.md](docs/GETTING-STARTED.md)**.

For everything currently in the library — every composable, what it replaces, and why —
see **[LIBRARY.md](convey/src/commonMain/kotlin/compose/conveyance/LIBRARY.md)**, and
**[AGENTS.md](AGENTS.md)** for the module layout and how to build this repo yourself.

For the generated, per-class/per-function reference (every public signature and doc comment,
kept current automatically on every push to `main`), see the
**[wiki's API reference](../../wiki/api-reference/index)**.

## What's actually here today

**Built, compiling, tested:** the full grammar/weight/employment enforcement layer; the six
framework-named "Replaces X" mechanisms (Escort, Reversal, Yield, Migration, Offer, Enter);
practice-decay; a first batch of concrete visual components (list item, card, chip, badge,
avatar, switch, segmented control, top bar, navigation bar); a WordNet+VerbNet-backed kinetic
typography layer that drives verb/noun-aware physics from real sentence input, unique to this
implementation.

**Not yet done, and said so where it matters:** the visual components haven't been checked
against a real display in the environment they were built in — see each one's doc comment for
what's genuinely verified vs. not. No Maven Central coordinates yet. iOS and wasmJs targets build
but aren't broadly exercised the way the Android/desktop paths are.

## Building

```
./gradlew :convey:build --stacktrace
```

See [AGENTS.md](AGENTS.md) for SDK requirements, target-by-target caveats, and the dev-loop
modules (`dev-app`, `android-dev-app`, `hotswap`).

## License

[Apache License 2.0](LICENSE).
