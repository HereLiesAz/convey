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
  `convey/karma.config.d/no-sandbox.js` adds a custom `ChromeHeadlessNoSandbox` Karma launcher
  (`--no-sandbox --disable-gpu --disable-dev-shm-usage`) so the suite also runs inside a
  container with no D-Bus session and no user namespaces — set `CHROME_BIN` to point at whatever
  Chromium/Chrome binary is available (e.g. Playwright's bundled Chromium) and run
  `./gradlew :convey:wasmJsBrowserTest`. Verified for real in this repo: 77/77 tests pass this
  way against a Playwright-provisioned Chromium with no display and no D-Bus.
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
- `ConveyEmployment` — Law 4 enforcement: every element declares at least 4 of the 11 `ConveyJob`s
  or is explicitly `ambient` (budgeted per surface), via the same registry pattern as `ConveyWeight`.
- `ConveyPractice` — practice-decay (§6.3): `ConveyPracticeRegistry` counts an element's genuine
  operations; `conveyPracticeDecay()` turns that into a `1f->floor` multiplier spent two ways —
  `AnimationSpec.decayed()` shortens a tween/stiffens a spring, `conveyPracticedAffordance()`
  silences a Tell after the first real operation. In-memory/session-scoped by default; `seed()`
  is the hook for an app's own persistence.
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
(procedural static layout keyed to a sentence's verb), `ConveyForceDynamics` (the pure-Kotlin
2D physics primitives `ConveySvoScene` consumes — no external physics engine dependency),
`ConveyEscort` (replaces disabled controls — a blocked act shakes and travels to its gate's
location instead of greying out), `ConveyReversal` (replaces confirm dialogs/undo snackbars —
a destroyed item collapses to a reversible residue in place; not `ConveyGhost`, since
`ConveyWeight.Ghost` already names a different, unrelated concept), `ConveyYield` (replaces
spinners/progress bars — the engaged element fills and compresses in place instead of a separate
progress object appearing beside it), `ConveyMigration` (replaces empty-state illustrations —
an empty collection's creation control sits full-size and centered, then relocates to its
permanent corner and shrinks on first use), `ConveyOffer` (the framework's Act, offered —
composes `ConveyConstruct`+`ConveyEscort`+`ConveyStateHost` into one declaration with a gate, an
interrupt, and Invite/Progress/Success/Failure/Interrupted states; a destructive act's inverse is
`ConveyReversal` wrapping it, not a parameter on it), `ConveyEnter` (Law 2 continuity for
navigation — a destination grows from the `Modifier.conveyOrigin`-marked element that led to it,
a scale/translate approximation of a shared-element transition, not yet visually verified against
a real display), `ConveyDesign`/`ConveyDesignPage` (Part XI of the Manifesto, "The Design
Block" — automatic composition for semantic-level text: `ConveyDesignSolver` adjusts
size/weight/condensation/tracking so a block's (or, via `ConveyDesignPage`, a whole page's —
§11.7's cross-block propagation) silhouette reads as balanced; condensation and weight render
through real Azrienoch `wdth`/`wght` axes via `conveyTypeFontFamily`, not an approximation).
`ConveyDesignSolver`'s functions default to `naturalWidth`'s fixed per-character advance-width
approximation (unchanged for `ConveyDesignSolverTest`'s own direct calls) but take an optional
trailing `ConveyDesignMeasure` to swap that out — `ConveyDesign`/`ConveyDesignPage` default
theirs to `rememberConveyDesignMeasure()`, a real `TextMeasurer`-backed one that varies
`fontSize`/`letterSpacing` for real measurement on the (dominant, tried-first) size lever, with
condensation applied afterward as a linear scale correction rather than a second real
measurement — rebuilding a `FontFamily` at a different `wdth` per binary-search candidate would
need `conveyTypeFontFamily`'s `@Composable`/async-loaded `Font()` resolved fresh each time,
which can't run synchronously inside a solver loop; see that function's own doc comment for the
full accounting,
`ConveyDecoration` (`ConveyActText`/`ConveyDecoratedText` — Part IV §4.2, "Text as an Act": the
Decoration channel, a persistent marker on text that is itself an Act, plus a one-time Tell
burst through the kinetic-typography engine for an unpracticed instance — `ConveyDesignLine`'s
`isAct` wires this straight into a `DESIGN` block's own lines), `ConveyScrollParallax` (the
scroll-linked-animation primitive Part XII calls for — neither Compose nor this library had a
way to continuously map scroll position to a transform before this; `Modifier.conveyScrollParallax`
reads live element position in the draw phase via `graphicsLayer`, so a scroll gesture re-layers
affected elements without recomposing them), `ConveyBody` (Part XII of the Manifesto, "The Body
Block" — `DESIGN`'s sibling for body-level prose: `Paragraph`/`Quote` lines are classified
word-by-word by the existing `ConveyVerbLexicon`/`ConveyNounLexicon` engine, and that one pass
drives both emotive idle motion, via the existing `ConveyVerbClass.toConveyLife()` mapping
`ConveyKineticSentence` already uses, and a fluid Azrienoch `wght` value via
`ConveyBodyClassifier`'s own ratio-tool weight-delta buckets — applied to every line inside the
block, mandatory rather than offered, since prose read at length that sometimes moves and
sometimes doesn't reads as broken. `ConveyBody` owns its own scroll container and every line
performs a mandatory scroll-linked entrance via `conveyScrollParallax`, direction keyed to role:
`Paragraph` horizontal, `Quote` vertical. Wired into both dev galleries (`dev-app`/
`android-dev-app`, see "Dev loop" below) and actually visually verified rendering in this
environment — Compose Desktop under Xvfb with software Skiko rendering (no GL context available
here), screenshotted via `java.awt.Robot`: per-word weight variation is visible in the captured
output, e.g. "sprints"/"chasing" (heavy-verb bucket) rendering bolder than "considered"/
"weighing" (cognition-verb bucket) in the same line), and a first batch of concrete visual components:
`ConveyListItem`, `ConveyCard`, `ConveyAvatar`, `ConveyBadge`, `ConveyChip`, `ConveySwitch`,
`ConveySegmentedControl`, `ConveyTopBar`, and `ConveyNavigationBar` (see LIBRARY.md for each).
`tokens/` holds `ConveyMotion`/`ConveyShape`/`ConveyColor`/`ConveySize`/`ConveyType`.
`ConveyType` is this library's official typeface — [Azrienoch](https://github.com/HereLiesAz/Azrienoch),
a multiplex variable font (SIL OFL 1.1) exposing `wght`/`wdth`/`SERF`/`GRAD` as one family
instead of a family per weight or style; the compiled font ships as a Compose resource
(`src/commonMain/composeResources/font/azrienoch_vf.ttf`). Compose's `TextStyle` has no live
`fontVariationSettings` field (confirmed against the actual class this project's pinned Compose
Multiplatform version compiles against), so `conveyTypeFontFamily(variation)` bakes a
`ConveyTypeVariation` (a fixed axis point — see `ConveyTypePreset` for named ones) into its own
`FontFamily`; a live slider UI just calls it again each recomposition. See `docs/THIRD_PARTY_NOTICES.md`
for the font's license (`docs/Azrienoch-OFL.txt` travels with it) and `ConveyType.kt`'s own doc
comment for the honest per-platform caveat on the two custom axes (`SERF`/`GRAD`): this module
compiles clean on `androidTarget`/`desktop`/`wasmJs` (and `iosArm64`/`iosSimulatorArm64` compile
too, though no macOS host here to run them). Desktop's actual rendered output (including this
specimen) has been visually verified in this environment — `:dev-app:run` under Xvfb, screenshotted
via `java.awt.Robot` — with software Skiko rendering, since no real GL context is available here
(`Cannot create Linux GL context`, Skiko falls back automatically); Android and wasmJs's actual
on-screen rendering has not (no emulator/device, no display for a real browser here respectively,
though wasmJs's own component tests now run for real against headless Chromium — see "Building"
above) — `wght`/`wdth` are safe everywhere regardless.

## Dev loop

Three extra Gradle modules exist purely to iterate on `convey/`'s composables faster; none of
them are published, and none change what `:convey:build` produces:

- `dev-app` — a plain JVM application module hosting a small gallery
  (`dev-app/src/jvmMain/kotlin/compose/conveyance/dev/Dev.kt`) that exercises the kinetic
  typography composables. `./gradlew :dev-app:hotRunJvm` runs it under [Compose Hot
  Reload](https://kotlinlang.org/docs/multiplatform/compose-hot-reload.html): edit a composable
  and save, and the running JVM picks up the change without restarting. It has to be a separate
  application module rather than living inside `convey` itself — Hot Reload doesn't register any
  `hotRun*` task applied inside a Kotlin Multiplatform *library* module. Every plugin `dev-app`
  shares with `convey` (Kotlin, Compose) is declared once, unapplied, at the root
  `build.gradle.kts`; declaring it per-subproject instead loads two separate instances of the
  Kotlin Gradle Plugin, which silently breaks Hot Reload's plugin-identity check with no build
  failure, only a logged warning — see that file's comment if this needs touching again.
- `android-dev-app` — the same idea for an actual Android device: a real, installable
  `com.android.application` module (`convey` itself can't be `adb install`ed, it's a library) with
  a `MainActivity` hosting the same gallery. Needs its own `alias(libs.plugins.kotlinAndroid)` in
  `android-dev-app/build.gradle.kts` (plus a matching `jvmTarget`/`compileOptions` pin to `JVM_11`,
  same as `convey`'s own `androidTarget`) — without it Gradle registers no `compileDebugKotlin`
  task for this module at all and silently drops `MainActivity.kt` from the build with no error,
  producing an APK with no launcher activity; confirmed and fixed for real, not just inferred from
  reading the build script (`./gradlew :android-dev-app:assembleDebug` now actually compiles and
  packages it).
- `hotswap` — a free, from-scratch, on-device hot-swap tool for `android-dev-app`, since Compose
  Hot Reload is desktop-only and the equivalent third-party tool (HotSwan) is paid. It redefines a
  changed class on an already-running debuggable process over JDWP (the same ART primitive Android
  Studio's own "Apply Changes" is built on), then broadcasts a reload trigger. See
  `hotswap/README.md` for how it works and, importantly, what is and isn't actually verified — this
  sandbox has no Android device, so the JDWP wire protocol is unit-tested but the real on-device
  redefinition path is not.

## Docs accuracy discipline

The three files in `convey/docs/` are research reports with an "Implementation status" section
added at the top of each, stating plainly what's real (built against actual WordNet/VerbNet data,
not a hand-curated word list), what's an honest approximation and why, and what's explicitly out
of scope and why it can't be built here. When touching the code those sections describe, check
the claims against the current source before leaving them — they drift like any other doc, and
this repo's convention is to keep them honest rather than aspirational.
