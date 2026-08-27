# conveyance-convey

Two things live in this repository, and they are not the same project wearing two hats — one
documents the other.

## `convey/` — the real library

A Kotlin Multiplatform Compose design system (group `compose.conveyance`, artifact `convey`)
built on the [Conveyance Manifesto](https://github.com/HereLiesAz/Conveyance/) — not a themed
component kit, but the manifesto's constraints encoded as a type system: a closed motion grammar,
enforced visual hierarchy, persistent-identity shape/color morphing, self-teaching interaction
affordances, and a live self-report audit. See
[`convey/src/commonMain/kotlin/compose/conveyance/LIBRARY.md`](convey/src/commonMain/kotlin/compose/conveyance/LIBRARY.md)
for the library's own philosophy and full API.

Targets Android and desktop, built with Gradle, published to JitPack as
`com.github.HereLiesAz.conveyance-convey:convey:main-SNAPSHOT`.

```
./gradlew build          # compile + test both targets
./gradlew desktopTest     # run the unit test suite
```

## `src/` — the design-token preview

A React + Vite + Tailwind app, scaffolded and run through
[Figma Make](https://www.figma.com/make) (which only builds React apps — hence the second stack
in a Kotlin repo). It is an interactive, browsable rendering of the *same* tokens `convey/`
implements: `ConveyShape`, `ConveyColor`, `ConveyMotion`, `ConveySize`, and the transform/
interaction/animation vocabulary, labeled with their real Kotlin identifiers (`ConveyColor.Primary`,
`ConveyShape.Circle`, …) so a change can be eyeballed without building an Android or desktop app.
It is documentation you can click through, not a separate product — see `AGENTS.md` for its own
project structure and conventions.

```
pnpm install
pnpm dev                  # serves the preview on $PORT (default 8443)
```

## Keeping them in sync

The preview hand-mirrors `convey/`'s token values (see `src/App.tsx`'s `SHAPES` / `COLOR_ROLES` /
etc. constants against `convey/src/commonMain/kotlin/compose/conveyance/tokens/`). There is no
build-time link between the two — a token changed in one place does not update the other. When you
change a token in `convey/`, update its entry in `src/App.tsx` in the same change.
