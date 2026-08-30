# conveyance-convey

Kotlin Multiplatform / Compose Multiplatform library. The real, actively-developed project is
the Gradle module `convey/` (source under `convey/src/commonMain/kotlin/compose/conveyance/`) —
a Compose design system built on the [Conveyance Manifesto](https://github.com/HereLiesAz/Conveyance/),
plus a WordNet+VerbNet-backed natural-language layer that drives verb/noun-aware kinetic
typography and physics.

## About the other files at this repo root

The repo root also contains a `src/`, `package.json`, `vite.config.ts`, `index.html`, and related
files — a leftover React 19 + Vite + Tailwind CSS scaffold ("figma-make-app") from an earlier,
unrelated experiment. **It is dead and unused**: nothing in `convey/` depends on it, no build in
`.github/workflows/` touches it, and it does not describe this repository. It has not been
deleted because that isn't this document's call to make unasked — but do not treat it as the
project, and do not add to it. If you're looking for the actual codebase, everything relevant is
under `convey/`.

## Building

From the repo root:

```
./gradlew :convey:build --stacktrace
```

- Requires JDK 21 (see `.github/workflows/build.yml`) and an Android SDK with
  `platforms;android-35` / `build-tools;35.0.0` installed (the `androidTarget` in
  `convey/build.gradle.kts` compiles against `compileSdk = 35`). Locally this means either
  `ANDROID_HOME`/`ANDROID_SDK_ROOT` pointing at an installed SDK, or a `local.properties` file at
  the repo root with `sdk.dir=/path/to/android-sdk` (there is no `local.properties` checked in).
  Without a usable Android SDK, `:convey:build` fails at the `androidTarget` compile/lint tasks.
- Kotlin Multiplatform targets: `androidTarget`, `iosX64`/`iosArm64`/`iosSimulatorArm64`, a
  desktop `jvm("desktop")`, and `wasmJs { browser() }` (see `convey/build.gradle.kts`).
- **iOS targets need a macOS host.** Kotlin/Native's iOS toolchain won't run on Linux; on a
  non-macOS machine, `kotlin.native.ignoreDisabledTargets=true` (set in `gradle.properties`) lets
  Gradle silently skip the iOS targets instead of failing the whole build — same as CI's Linux
  runner does.
- **wasmJs browser tests need a real Chrome or Firefox binary** on the machine running the build;
  without one, the `wasmJsBrowserTest` task fails even though compilation itself succeeds. CI and
  most sandboxed environments may not have one — don't attribute a wasmJs test failure to a code
  change without first checking whether a browser binary is actually available.
- CI (`.github/workflows/build.yml`) runs exactly `./gradlew :convey:build --stacktrace` on
  Ubuntu with JDK 21 and the Android SDK packages above — treat that workflow as the source of
  truth for how this project is built if this document and it ever disagree.

## Module shape

Top-level source files in `convey/src/commonMain/kotlin/compose/conveyance/` (a `foundation/`,
`internal/`, and `tokens/` subpackage hold the rest — see
[`LIBRARY.md`](convey/src/commonMain/kotlin/compose/conveyance/LIBRARY.md) for the full package
map and public-API-level detail):

- `ConveyGrammar` — the motion vocabulary contract (`ConveyMeaning` → `AnimationSpec`); every
  other composable's motion is driven through this, not a raw spec.
- `ConveyWeight` — visual-hierarchy enforcement (Hero/Primary/Secondary/Ghost) with a registry
  that throws in debug builds on a violated constraint (e.g. two Heroes).
- `ConveyAffordance` — self-revealing interactivity (teaches a gesture once, then stops).
- `ConveyInteraction` — ripple, press, long-press, swipe, grammar-driven.
- `ConveyTransform` — scale/lift/rotate/slide transforms, grammar-driven.
- `ConveyMorph` — persistent-identity morphing between shapes and colors.
- `ConveyLife` — continuous idle motion for chrome that should read as "alive."
- `ConveyKineticText` — per-glyph kinetic text; also `ConveyKineticSentence`, which drives
  per-word motion from `ConveyVerb`'s classification of each word.
- `ConveyVerb` — deterministic verb classification (`ConveyVerbClass`, `ConveyVerbLexicon`) over
  real Princeton WordNet 3.0 + VerbNet 3.3 data (Simplified Lesk WSD included), plus
  `ConveyVerbClass.toEventTimeline()`, a verb's reduction onto the physical-event booleans
  `ConveySvoScene`'s force simulator drives from. See
  [`docs/kinetic-text-verb-classification.md`](convey/docs/kinetic-text-verb-classification.md).
- `ConveyNoun` — deterministic noun classification (`ConveyNounLexicon`) — animacy and count/mass
  — over the same WordNet data and disambiguation shape as `ConveyVerb`. See
  [`docs/Procedural Animation of Subject-Verb-Object Typography.md`](<convey/docs/Procedural Animation of Subject-Verb-Object Typography.md>).
- `ConveySvoScene` — the orchestrating composable: splits a sentence into subject/verb/object
  (`parseSvoHeuristic`), classifies the nouns and verb, and drives `foundation/ConveyForceDynamics`
  to animate the subject toward the object (translate, collide, squash/stretch, gait bob/tilt for
  an animate subject). Falls back to `ConveyKineticSentence` when the heuristic can't split the
  sentence.
- `ConveyProvider` — the `ConveySystem` root that activates all enforcement.
- `internal/ConveyVerbData.kt` and `internal/ConveyNounData.kt` — **generated, do not hand-edit**;
  compiled WordNet/VerbNet data blobs. See each doc file's own "Generation pipeline" section to
  regenerate them, and [`docs/THIRD_PARTY_NOTICES.md`](convey/docs/THIRD_PARTY_NOTICES.md) for
  their license attribution.

`foundation/` holds composables built on the above: `ConveyStateHost`, `ConveyFab`,
`ConveyConstruct`, `ConveyAttentionGrid`, `ConveyMorphControl`, `ConveyTopographicalLayout`
(procedural static layout keyed to a sentence's verb), and `ConveyForceDynamics` (the pure-Kotlin
2D physics primitives `ConveySvoScene` consumes — no external physics engine dependency).
`tokens/` holds `ConveyMotion`/`ConveyShape`/`ConveyColor`/`ConveySize`.

## Docs accuracy discipline

The three files in `convey/docs/` are research reports with an "Implementation status" section
added at the top of each, stating plainly what's real (built against actual WordNet/VerbNet data,
not a hand-curated word list), what's an honest approximation and why, and what's explicitly out
of scope and why it can't be built here. When touching the code those sections describe, check
the claims against the current source before leaving them — they drift like any other doc, and
this repo's convention is to keep them honest rather than aspirational.
