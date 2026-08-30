# Convey — Compose Multiplatform

> "A well-placed construction vehicle can do more than miles of traffic cones."
> — The Conveyance Manifesto

A Compose Multiplatform design system built on the [Conveyance Manifesto](https://github.com/HereLiesAz/Conveyance/).

This is not a component kit. It is a philosophy, encoded as a type system.

---

## What makes this different

Every design library gives you tokens and components. Convey gives you constraints.

The constraints enforce what the Manifesto demands:

**1. Motion carries meaning, or it carries nothing.**

```kotlin
// Wrong — a raw spec that means nothing to anyone reading the code:
animateFloatAsState(target, spring(stiffness = 380f, dampingRatio = 0.8f))

// Right — a declared meaning that can be audited, changed in one place, and read:
animateFloatAsState(target, grammar["navigate"])
```

`ConveyGrammar` is the motion vocabulary for your surface. Every animation must use a declared meaning. Unknown meanings throw at the call site, in debug builds, immediately. Not at render time. Not silently.

**2. One element is one thing across all its states.**

```kotlin
// Wrong — three components for three states:
if (loading) Spinner() else if (success) Checkmark() else SubmitButton()

// Right — one element that becomes its states:
ConveyStateHost(
    state = submitState,
    targetShape = if (loading || success) ConveyShape.Circle else ConveyShape.Large,
    targetWidth = if (loading || success) 52.dp else 160.dp,
    targetColor = when { success -> ConveyColor.Success; else -> ConveyColor.Primary },
) { state ->
    when (state) {
        Idle -> Text("Submit")
        Loading -> Spinner()
        Success -> Checkmark()
    }
}
```

The user never sees a new element appear. They see their action acknowledged by the same element they touched. That continuity is the information. The morph IS the message.

**3. Hierarchy is enforced, not suggested.**

```kotlin
// This THROWS in debug builds if another Hero exists on the same surface:
Modifier.conveyWeight(ConveyWeight.Hero)

// The registry tracks everything. The audit tells you what you have:
registry.snapshot()
// ConveyWeight Snapshot:
//   Hero:      1  (max 1)
//   Primary:   2  (max 3)
//   Secondary: 7
//   Ghost:     3
```

"When everything is primary, nothing is primary." `ConveyWeightRegistry` makes this structural.

**4. Every element must declare its purpose.**

```kotlin
ConveyConstruct(
    purpose = "Advance the user to the next step in checkout",
    weight = ConveyWeight.Hero,
    produces = ConveyOutcome.Navigate("checkout/payment"),
) {
    Button(onClick = { goToPayment() }) { Text("Continue") }
}
```

`ConveyConstruct.audit()` produces a complete map of every declared element on the surface. If you cannot read that map and understand the screen, the screen is not finished.

**5. Elements teach themselves, once.**

```kotlin
FloatingActionButton(
    modifier = Modifier.conveyAffordance(
        ConveyAffordance.PressHint(delay = 800L)
    )
)
```

The FAB performs a subtle press animation 800ms after appearing. The user sees it move and understands: this can be pressed. Then it never does that again. One lesson. Trust.

---

## The grammar is the contract

Before any other API, define your grammar:

```kotlin
val grammar = rememberConveyGrammar {
    meaning("navigate", "User moved to a different place") {
        spring(stiffness = 380f, dampingRatio = 0.82f)
    }
    meaning("confirm", "System acknowledged user input") {
        spring(stiffness = 620f, dampingRatio = 0.74f)
    }
    meaning("dismiss", "Content left — its job is done") {
        tween(180, easing = FastOutLinearInEasing)
    }
    meaning("morph", "One element became another") {
        spring(stiffness = 280f, dampingRatio = 0.78f)
    }
    meaning("error", "Wrong. Stop. Attend to this.") {
        snap() // errors do not animate — they interrupt
    }
}

ConveySystem(grammar = grammar) {
    // Your UI
}
```

Read the grammar out loud. If it does not sound like a description of what the user experiences, revise it. The grammar is documentation that the compiler enforces.

---

## Package structure

```
compose.conveyance
├── ConveyGrammar          — Motion vocabulary. The central contract.
├── ConveyMorph            — Persistent identity morphing between shapes and colors.
├── ConveyWeight           — Visual hierarchy enforcement (Hero/Primary/Secondary/Ghost).
├── ConveyAffordance       — Self-revealing interactivity. Teaches once, then trusts.
├── ConveyInteraction      — Ripple, press, long-press, swipe.
├── ConveyTransform        — Scale, lift, rotate, slide — with grammar-driven specs.
├── ConveyLife             — Continuous idle motion for chrome that reports "alive now."
├── ConveyKineticText      — Text as a composable, not a label: per-glyph life and burst.
│                            (also: ConveyKineticSentence — per-word motion from ConveyVerbClass,
│                            disambiguated per-sentence)
├── ConveyVerb             — Deterministic verb → ConveyLife classification over real Princeton
│                            WordNet + VerbNet data (ConveyVerbClass, ConveyVerbLexicon), incl.
│                            Simplified Lesk WSD; see docs/kinetic-text-verb-classification.md
│                            and internal/ConveyVerbData.kt (generated, do not hand-edit). Also:
│                            ConveyVerbClass.toEventTimeline() reduces a classified verb onto a
│                            ConveyVerbEventTimeline (approaches/contactAtEnd/continuousNoContact/
│                            possessionTransfer) — the physical-event decomposition ConveySvoScene's
│                            force simulator drives from; see the "Implementation status" section
│                            of docs/Procedural Animation of Subject-Verb-Object Typography.md for
│                            exactly which of those four booleans the simulator actually reads.
├── ConveyNoun             — Deterministic noun → (animacy, count/mass) classification over the
│                            same Princeton WordNet 3.0 data and Simplified Lesk WSD shape as
│                            ConveyVerb (ConveyNounProperties, ConveyNounLexicon), backed by
│                            internal/ConveyNounData.kt (generated, do not hand-edit). See docs/
│                            Procedural Animation of Subject-Verb-Object Typography.md.
├── ConveySvoScene         — Orchestrating composable: parseSvoHeuristic splits a sentence into
│                            subject/verb/object, ConveyNounLexicon classifies the nouns,
│                            ConveyVerbClass.toEventTimeline drives foundation/ConveyForceDynamics'
│                            pure-Kotlin simulator to move the subject toward the object, collide,
│                            squash/stretch, and (for an animate subject) bob/tilt in a gait
│                            approximation. Falls back to ConveyKineticSentence when the heuristic
│                            can't confidently split the sentence.
├── ConveyProvider         — ConveySystem root. Activates all enforcement.
│
├── foundation/
│   ├── ConveyStateHost    — Single element across multiple states.
│   ├── ConveyFab          — The morphing FAB. The canonical Conveyance hero.
│   ├── ConveyConstruct    — Purpose declaration and surface auditing.
│   ├── ConveyAttentionGrid — A grid where the attended tile IS Primary, and escalates to Hero.
│   ├── ConveyMorphControl — A control that becomes a structurally different control on demand.
│   ├── ConveyForceDynamics — Pure-Kotlin 2D force-dynamics primitives with no external physics
│   │                        engine dependency: Vec2, attraction/repulsion, circle-circle collision
│   │                        (ConveyForceDynamics), a symplectic-Euler rigid body (ConveyRigidBody),
│   │                        a scalar damped-harmonic "soft body" wobble (ConveySpringMassBody), and
│   │                        a periodic bob/tilt gait approximation (ConveyGaitOscillator). Built
│   │                        for, and consumed by, ConveySvoScene.
│   └── ConveyTopographicalLayout — Static (non-animated) procedural layout (staircase, spiral,
│                            ring...) that picks its own shape from a sentence's own verb via
│                            ConveyVerbLexicon.topographicalCategory.
│
└── tokens/
    ├── ConveyMotion       — Spring presets named for what they communicate.
    ├── ConveyShape        — Corner radius tokens named for what they signal.
    ├── ConveyColor        — Semantic role-based color system.
    └── ConveySize         — Proportional spacing and component sizes.
```

---

## What this library will not do

- Provide a pre-built color palette. Your brand colors are not ours to choose.
- Replace your typography system. Typography belongs to your product.
- Work correctly if you bypass the grammar. That is the point.
- Make bad UX beautiful. Convey encodes good UX principles. Beauty is yours.

---

## Compliance

A Convey surface is compliant with the Conveyance Manifesto when:

- [ ] Every animation uses a declared grammar meaning
- [ ] Exactly one element carries `ConveyWeight.Hero`
- [ ] No more than `maxPrimary` elements carry `ConveyWeight.Primary`
- [ ] Every Primary and Hero element has a declared `ConveyOutcome`
- [ ] Every interactive element has either a `ConveyAffordance` or `conveyInert()`
- [ ] `ConveyConstruct.audit()` can be read and understood in under 60 seconds
- [ ] The grammar has 8 or fewer meanings

If any of these are false, the surface is not finished.
