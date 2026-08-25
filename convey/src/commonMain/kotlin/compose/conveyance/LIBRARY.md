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
├── ConveyProvider         — ConveySystem root. Activates all enforcement.
│
├── foundation/
│   ├── ConveyStateHost    — Single element across multiple states.
│   ├── ConveyFab          — The morphing FAB. The canonical Conveyance hero.
│   └── ConveyConstruct    — Purpose declaration and surface auditing.
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
