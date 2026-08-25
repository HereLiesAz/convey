import { useState, useRef, useCallback, useEffect } from 'react'
import type { ReactNode } from 'react'

// ── Helpers ────────────────────────────────────────────────────────────────

function cn(...classes: (string | false | undefined | null)[]) {
  return classes.filter(Boolean).join(' ')
}

// ── Spring physics hook ────────────────────────────────────────────────────

function useSpringAnim(stiffness: number, damping: number) {
  const [value, setValue] = useState(0)
  const rafRef = useRef<number | null>(null)
  const stateRef = useRef({ p: 0, v: 0 })

  const animateTo = useCallback(
    (target: number) => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
      stateRef.current = { p: 0, v: 0 }
      const dt = 1 / 60
      const step = () => {
        const { p, v } = stateRef.current
        const force = -stiffness * (p - target) - damping * v
        const newV = v + force * dt
        const newP = p + newV * dt
        stateRef.current = { p: newP, v: newV }
        setValue(newP)
        if (Math.abs(newP - target) > 0.0005 || Math.abs(newV) > 0.0005) {
          rafRef.current = requestAnimationFrame(step)
        } else {
          setValue(target)
        }
      }
      rafRef.current = requestAnimationFrame(step)
    },
    [stiffness, damping],
  )

  useEffect(() => () => { if (rafRef.current) cancelAnimationFrame(rafRef.current) }, [])
  return { value, animateTo }
}

// ── Ripple hook ────────────────────────────────────────────────────────────

interface Ripple { x: number; y: number; id: number }

function useRipple(color = 'rgba(123,86,248,0.35)') {
  const [ripples, setRipples] = useState<Ripple[]>([])
  const onRipple = useCallback((e: React.MouseEvent<HTMLElement>) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const id = Date.now()
    setRipples(p => [...p, { x: e.clientX - rect.left, y: e.clientY - rect.top, id }])
    setTimeout(() => setRipples(p => p.filter(r => r.id !== id)), 800)
  }, [])
  const RippleEl = useCallback(() => (
    <>
      {ripples.map(r => (
        <span
          key={r.id}
          style={{
            position: 'absolute', left: r.x, top: r.y, width: 14, height: 14,
            borderRadius: '50%', background: color, pointerEvents: 'none',
            animation: 'cv-ripple 800ms ease-out forwards',
          }}
        />
      ))}
    </>
  ), [ripples, color])
  return { onRipple, RippleEl }
}

// ── Long press hook ────────────────────────────────────────────────────────

function useLongPress(onLongPress: () => void, duration = 500) {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const [progress, setProgress] = useState(0)
  const progressRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const start = useCallback(() => {
    let elapsed = 0
    progressRef.current = setInterval(() => {
      elapsed += 16
      setProgress(Math.min(elapsed / duration, 1))
    }, 16)
    timerRef.current = setTimeout(() => {
      onLongPress()
      setProgress(0)
      if (progressRef.current) clearInterval(progressRef.current)
    }, duration)
  }, [onLongPress, duration])

  const cancel = useCallback(() => {
    if (timerRef.current) clearTimeout(timerRef.current)
    if (progressRef.current) clearInterval(progressRef.current)
    setProgress(0)
  }, [])

  return { start, cancel, progress }
}

// ── Constants ──────────────────────────────────────────────────────────────

const NAV_ITEMS = [
  { id: 'shapes', label: 'Shapes' },
  { id: 'colors', label: 'Colors' },
  { id: 'motion', label: 'Motion' },
  { id: 'sizes', label: 'Sizes' },
  { id: 'transforms', label: 'Transforms' },
  { id: 'interactions', label: 'Interactions' },
  { id: 'animations', label: 'Animations' },
]

const SHAPES = [
  { id: 'circle', label: 'Circle', style: { borderRadius: '50%' }, spec: 'ConveyShape.Circle', desc: '50% radius — highest interactivity signal' },
  { id: 'squircle', label: 'Squircle', style: { borderRadius: '35%' }, spec: 'ConveyShape.Squircle', desc: 'Superellipse — friendly and approachable' },
  { id: 'xl', label: 'Extra Large', style: { borderRadius: '28px' }, spec: 'ConveyShape.ExtraLarge', desc: 'Cards and large surfaces' },
  { id: 'lg', label: 'Large', style: { borderRadius: '16px' }, spec: 'ConveyShape.Large', desc: 'Dialogs and sheets' },
  { id: 'md', label: 'Medium', style: { borderRadius: '12px' }, spec: 'ConveyShape.Medium', desc: 'Chips and input fields' },
  { id: 'sm', label: 'Small', style: { borderRadius: '8px' }, spec: 'ConveyShape.Small', desc: 'Badges and compact elements' },
  { id: 'none', label: 'None', style: { borderRadius: '0px' }, spec: 'ConveyShape.None', desc: 'Full-bleed and structural layouts' },
  {
    id: 'cut', label: 'Cut', spec: 'ConveyShape.Cut', desc: 'Mechanical precision — settings, system UI',
    style: { borderRadius: '0px', clipPath: 'polygon(12px 0%,calc(100% - 12px) 0%,100% 12px,100% calc(100% - 12px),calc(100% - 12px) 100%,12px 100%,0% calc(100% - 12px),0% 12px)' },
  },
]

const COLOR_ROLES = [
  { group: 'Primary', roles: [
    { name: 'Primary', token: 'ConveyColor.Primary', hex: '#7B56F8', bg: '#7B56F8', fg: '#FFFFFF' },
    { name: 'On Primary', token: 'ConveyColor.OnPrimary', hex: '#FFFFFF', bg: '#FFFFFF', fg: '#04040C' },
    { name: 'Primary Container', token: 'ConveyColor.PrimaryContainer', hex: '#1B1050', bg: '#1B1050', fg: '#C4AAFF' },
    { name: 'On Primary Container', token: 'ConveyColor.OnPrimaryContainer', hex: '#C4AAFF', bg: '#C4AAFF', fg: '#04040C' },
  ]},
  { group: 'Secondary', roles: [
    { name: 'Secondary', token: 'ConveyColor.Secondary', hex: '#00CBA9', bg: '#00CBA9', fg: '#002820' },
    { name: 'On Secondary', token: 'ConveyColor.OnSecondary', hex: '#002820', bg: '#002820', fg: '#6DF5D4' },
    { name: 'Secondary Container', token: 'ConveyColor.SecondaryContainer', hex: '#003D33', bg: '#003D33', fg: '#6DF5D4' },
    { name: 'On Secondary Container', token: 'ConveyColor.OnSecondaryContainer', hex: '#6DF5D4', bg: '#6DF5D4', fg: '#002820' },
  ]},
  { group: 'Tertiary', roles: [
    { name: 'Tertiary', token: 'ConveyColor.Tertiary', hex: '#FF8B5E', bg: '#FF8B5E', fg: '#FFFFFF' },
    { name: 'Tertiary Container', token: 'ConveyColor.TertiaryContainer', hex: '#4A1800', bg: '#4A1800', fg: '#FF8B5E' },
  ]},
  { group: 'Semantic', roles: [
    { name: 'Error', token: 'ConveyColor.Error', hex: '#FF4D6A', bg: '#FF4D6A', fg: '#FFFFFF' },
    { name: 'Warning', token: 'ConveyColor.Warning', hex: '#FFAD42', bg: '#FFAD42', fg: '#04040C' },
    { name: 'Success', token: 'ConveyColor.Success', hex: '#34E89E', bg: '#34E89E', fg: '#04040C' },
  ]},
  { group: 'Surface', roles: [
    { name: 'Surface', token: 'ConveyColor.Surface', hex: '#04040C', bg: '#04040C', fg: '#ECEDF5' },
    { name: 'Surface Container', token: 'ConveyColor.SurfaceContainer', hex: '#0D0D22', bg: '#0D0D22', fg: '#ECEDF5' },
    { name: 'On Surface', token: 'ConveyColor.OnSurface', hex: '#ECEDF5', bg: '#ECEDF5', fg: '#04040C' },
    { name: 'On Surface Muted', token: 'ConveyColor.OnSurfaceMuted', hex: '#9899BC', bg: '#9899BC', fg: '#04040C' },
  ]},
]

const SPRING_PRESETS = [
  { name: 'Snappy', stiffness: 600, damping: 35, token: 'ConveySpring.Snappy', color: '#7B56F8', desc: 'Immediate response, minimal overshoot. Use for pressed states and quick feedback.' },
  { name: 'Standard', stiffness: 380, damping: 30, token: 'ConveySpring.Standard', color: '#00CBA9', desc: 'Balanced feel for most UI transitions. The default for navigation and layout shifts.' },
  { name: 'Gentle', stiffness: 200, damping: 25, token: 'ConveySpring.Gentle', color: '#FF8B5E', desc: 'Slow, deliberate motion. For revealing content and non-urgent state changes.' },
  { name: 'Bouncy', stiffness: 300, damping: 12, token: 'ConveySpring.Bouncy', color: '#FFAD42', desc: 'Playful overshoot. Reserve for hero moments and delightful empty states.' },
]

const SIZE_SCALE = [
  { token: 'ConveySize.None', dp: 0, px: 0 },
  { token: 'ConveySize.XSmall', dp: 4, px: 4 },
  { token: 'ConveySize.Small', dp: 8, px: 8 },
  { token: 'ConveySize.Medium', dp: 16, px: 16 },
  { token: 'ConveySize.Large', dp: 24, px: 24 },
  { token: 'ConveySize.XLarge', dp: 32, px: 32 },
  { token: 'ConveySize.XXLarge', dp: 48, px: 48 },
  { token: 'ConveySize.Huge', dp: 64, px: 64 },
  { token: 'ConveySize.Hero', dp: 96, px: 96 },
]

// ── Shared components ──────────────────────────────────────────────────────

function CodeBlock({ code }: { code: string }) {
  const [copied, setCopied] = useState(false)
  const copy = () => {
    navigator.clipboard.writeText(code).catch(() => {})
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }
  return (
    <div className="rounded-2xl bg-canvas border border-wire/8 overflow-hidden text-sm font-mono">
      <div className="flex items-center justify-between px-5 py-3 border-b border-wire/6 bg-panel">
        <div className="flex items-center gap-2.5">
          <span className="w-2.5 h-2.5 rounded-full bg-primary/70" />
          <span className="text-ink-muted text-xs tracking-wide">kotlin · Compose Multiplatform</span>
        </div>
        <button
          onClick={copy}
          className="text-xs text-ink-muted hover:text-primary-dim transition-colors cursor-pointer px-2 py-0.5 rounded"
        >
          {copied ? '✓ copied' : 'copy'}
        </button>
      </div>
      <pre className="px-5 py-5 text-ink-dim leading-relaxed overflow-x-auto whitespace-pre text-xs">
        {code}
      </pre>
    </div>
  )
}

function Section({ id, children }: { id: string; children: ReactNode }) {
  return (
    <section id={id} className="min-h-screen py-28 px-6 md:px-14 border-t border-wire/6 max-w-7xl mx-auto w-full">
      {children}
    </section>
  )
}

function SectionHead({ eyebrow, title, subtitle }: { eyebrow: string; title: string; subtitle: string }) {
  return (
    <div className="mb-16">
      <p className="font-mono text-primary text-xs tracking-[0.2em] uppercase mb-4">{eyebrow}</p>
      <h2 className="font-display font-black text-ink leading-none tracking-tight mb-4" style={{ fontSize: 'clamp(3rem, 8vw, 6rem)' }}>
        {title}
      </h2>
      <p className="text-ink-dim text-lg max-w-xl leading-relaxed">{subtitle}</p>
    </div>
  )
}

// ── NavBar ─────────────────────────────────────────────────────────────────

function NavBar({ active }: { active: string }) {
  const scrollTo = (id: string) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  return (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-canvas/85 backdrop-blur-xl border-b border-wire/6">
      <div className="max-w-7xl mx-auto px-6 flex items-center justify-between h-14">
        <button
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          className="flex items-center gap-3 group"
        >
          <span
            className="w-7 h-7 bg-primary flex items-center justify-center text-white text-xs font-black font-display transition-all duration-500 group-hover:shadow-[0_0_16px_4px_rgba(123,86,248,0.4)]"
            style={{ borderRadius: '8px' }}
          >
            C
          </span>
          <span className="font-display font-black text-ink text-base tracking-wider">CONVEY</span>
          <span className="text-ink-muted text-xs font-mono hidden md:block opacity-60">compose.conveyance</span>
        </button>
        <div className="flex items-center gap-0.5">
          {NAV_ITEMS.map(item => (
            <button
              key={item.id}
              onClick={() => scrollTo(item.id)}
              className={cn(
                'px-3 py-1.5 rounded-lg text-xs font-mono font-medium transition-all duration-200',
                active === item.id
                  ? 'bg-primary/15 text-primary'
                  : 'text-ink-muted hover:text-ink hover:bg-wire/5',
              )}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>
    </nav>
  )
}

// ── Hero ───────────────────────────────────────────────────────────────────

function HeroSection() {
  return (
    <section className="relative min-h-screen flex flex-col justify-center px-6 md:px-14 pt-14 overflow-hidden">
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-1/3 right-1/4 w-[500px] h-[500px] animate-cv-morph animate-cv-float animate-cv-glow"
          style={{ background: 'radial-gradient(circle, rgba(123,86,248,0.18) 0%, transparent 70%)', filter: 'blur(40px)' }} />
        <div className="absolute bottom-1/4 left-1/3 w-80 h-80 animate-cv-morph animate-cv-float animate-cv-glow"
          style={{ animationDelay: '3s', background: 'radial-gradient(circle, rgba(0,203,169,0.12) 0%, transparent 70%)', filter: 'blur(30px)' }} />
      </div>

      <div className="relative max-w-7xl mx-auto w-full">
        <p className="font-mono text-primary text-xs tracking-[0.25em] uppercase mb-6 animate-cv-slide-up" style={{ animationDelay: '0ms' }}>
          compose.conveyance · v1.0.0 · Compose Multiplatform
        </p>

        <h1
          className="font-display font-black text-ink leading-[0.9] tracking-tighter mb-8 animate-cv-slide-up"
          style={{ fontSize: 'clamp(5rem, 15vw, 13rem)', animationDelay: '60ms' }}
        >
          CONVEY
        </h1>

        <div className="grid md:grid-cols-2 gap-10 animate-cv-slide-up" style={{ animationDelay: '120ms' }}>
          <div>
            <p className="text-ink-dim text-xl leading-relaxed mb-6">
              A Compose Multiplatform design system where every shape, motion, color, and gesture teaches through interaction — never through instruction.
            </p>
            <p className="text-ink-muted text-sm font-mono leading-relaxed">
              Built on the Conveyance Manifesto.<br />
              Compassionate design for Kotlin Multiplatform.
            </p>
          </div>
          <div className="flex flex-col gap-3">
            <div className="px-5 py-4 rounded-2xl bg-card border border-wire/8 font-mono text-sm">
              <span className="text-ink-muted">// build.gradle.kts</span>
              <br />
              <span className="text-ink-muted">implementation</span>
              <span className="text-secondary ml-2">"compose.conveyance:convey:1.0.0"</span>
            </div>
            <div className="px-5 py-4 rounded-2xl bg-card border border-wire/8 font-mono text-xs text-ink-muted">
              <span className="text-success">✓</span> Android · iOS · Desktop · Web
            </div>
          </div>
        </div>

        <div className="mt-20 grid grid-cols-2 md:grid-cols-4 gap-4 animate-cv-slide-up" style={{ animationDelay: '200ms' }}>
          {[
            { n: '01', title: 'Lead by Example', body: 'Design teaches through interaction, not instruction. Every element demonstrates its own purpose as it moves.' },
            { n: '02', title: 'Resourceful Minimalism', body: 'Nothing stands around watching. A single component transitions through multiple states — button, loader, confirmation.' },
            { n: '03', title: 'Motion is Grammar', body: 'One meaning per animation signature, applied consistently. Physics-based spring motion trains muscle memory.' },
            { n: '04', title: 'Engineer the Hero', body: "Reserve peak expression for the product's emotional core. Every system deserves its defining moment." },
          ].map(p => (
            <div key={p.n} className="p-5 rounded-2xl bg-card/60 border border-wire/6 hover:border-primary/20 hover:bg-card transition-all duration-400 group">
              <span className="font-mono text-xs text-primary/50 group-hover:text-primary/80 transition-colors block mb-2">{p.n}</span>
              <h3 className="font-display font-bold text-ink text-sm mb-2 leading-tight">{p.title}</h3>
              <p className="text-ink-muted text-xs leading-relaxed">{p.body}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

// ── Shapes Section ─────────────────────────────────────────────────────────

function ShapesSection() {
  const [activeIdx, setActiveIdx] = useState(0)
  const shape = SHAPES[activeIdx]

  return (
    <Section id="shapes">
      <SectionHead
        eyebrow="ConveyShape"
        title="Shape"
        subtitle="Corner radii carry intent. Circular forms invite touch; sharp edges define structure. Radius is a signal before content speaks."
      />
      <div className="grid lg:grid-cols-2 gap-12">
        <div>
          <div className="aspect-square rounded-2xl bg-card border border-wire/6 flex items-center justify-center relative overflow-hidden mb-5">
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="w-full h-px bg-wire/4" />
            </div>
            <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
              <div className="w-px h-full bg-wire/4" />
            </div>
            <div
              className="w-48 h-48 bg-primary-well border border-primary/30 flex flex-col items-center justify-center gap-2"
              style={{
                ...shape.style,
                transition: 'border-radius 700ms cubic-bezier(0.34,1.56,0.64,1), clip-path 500ms ease',
              }}
            >
              <span className="font-mono text-on-primary-well text-xs text-center px-3 leading-relaxed">{shape.spec}</span>
            </div>
          </div>
          <div className="grid grid-cols-4 gap-2">
            {SHAPES.map((s, i) => (
              <button
                key={s.id}
                onClick={() => setActiveIdx(i)}
                className={cn(
                  'py-4 px-2 rounded-xl border flex flex-col items-center gap-2.5 text-xs font-mono transition-all duration-200 cursor-pointer',
                  activeIdx === i
                    ? 'border-primary/40 bg-primary/10 text-primary'
                    : 'border-wire/6 bg-card text-ink-muted hover:border-wire/14 hover:bg-raised',
                )}
              >
                <div
                  className="w-7 h-7 bg-primary/50 transition-all duration-500"
                  style={s.style}
                />
                <span className="text-center leading-tight text-[10px]">{s.label}</span>
              </button>
            ))}
          </div>
          {shape.desc && (
            <p className="mt-4 text-ink-muted text-xs font-mono px-1 transition-all duration-300">{shape.desc}</p>
          )}
        </div>

        <div className="flex flex-col gap-5">
          <CodeBlock code={`object ConveyShape {
    // Token library — radius encodes purpose
    val Circle     = RoundedCornerShape(percent = 50)
    val Squircle   = RoundedCornerShape(percent = 35)
    val ExtraLarge = RoundedCornerShape(28.dp)
    val Large      = RoundedCornerShape(16.dp)
    val Medium     = RoundedCornerShape(12.dp)
    val Small      = RoundedCornerShape(8.dp)
    val None       = RoundedCornerShape(0.dp)
    val Cut        = CutCornerShape(12.dp)

    // Animated morphing between any two shapes
    fun morph(
        from: Shape,
        to: Shape,
        progress: Float,
    ): Shape = MorphableShape(from, to, progress)
}

// Declarative morph — motion teaches the transition
val shape by animateConveyShape(
    targetShape = ConveyShape.Circle,
    spec = ConveyMotion.Spring.Standard,
)

Box(
    modifier = Modifier
        .background(ConveyColor.PrimaryContainer, shape)
        .clip(shape)
        .conveyInteraction(ConveyRipple.Bounded)
)`} />
          <div className="p-5 rounded-2xl bg-primary-well/40 border border-primary/12">
            <p className="text-primary font-mono text-xs mb-2 uppercase tracking-wider">Manifesto Rule</p>
            <p className="text-on-primary-well text-sm leading-relaxed">
              "Point at a control never touched — if the user can say what it does, you are done." Shape is the first affordance. Radius communicates clickability before color, label, or icon.
            </p>
          </div>
        </div>
      </div>
    </Section>
  )
}

// ── Colors Section ─────────────────────────────────────────────────────────

function ColorsSection() {
  const [hovered, setHovered] = useState<string | null>(null)
  return (
    <Section id="colors">
      <SectionHead
        eyebrow="ConveyColor"
        title="Color"
        subtitle="Dynamic color implicitly prioritizes. Primary-secondary-tertiary contrast directs attention without literal arrows or bold labels."
      />
      <div className="grid lg:grid-cols-2 gap-12">
        <div className="flex flex-col gap-6">
          {COLOR_ROLES.map(group => (
            <div key={group.group}>
              <p className="text-ink-muted text-xs font-mono uppercase tracking-widest mb-3">{group.group}</p>
              <div className="grid grid-cols-2 gap-2">
                {group.roles.map(role => (
                  <div
                    key={role.name}
                    className="rounded-xl p-3.5 cursor-default transition-all duration-200 hover:scale-[1.02]"
                    style={{ background: role.bg, color: role.fg }}
                    onMouseEnter={() => setHovered(role.token)}
                    onMouseLeave={() => setHovered(null)}
                  >
                    <p className="text-[10px] font-mono opacity-70 leading-tight">{role.token}</p>
                    <p className="text-xs font-mono mt-1 font-medium">{role.hex}</p>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="flex flex-col gap-5">
          <CodeBlock code={`object ConveyColor {
    // Primary role hierarchy
    val Primary            = Color(0xFF7B56F8)
    val OnPrimary          = Color(0xFFFFFFFF)
    val PrimaryContainer   = Color(0xFF1B1050)
    val OnPrimaryContainer = Color(0xFFC4AAFF)

    // Secondary — complementary contrast
    val Secondary            = Color(0xFF00CBA9)
    val OnSecondary          = Color(0xFF002820)
    val SecondaryContainer   = Color(0xFF003D33)
    val OnSecondaryContainer = Color(0xFF6DF5D4)

    // Tertiary — accent for hero moments only
    val Tertiary          = Color(0xFFFF8B5E)
    val TertiaryContainer = Color(0xFF4A1800)

    // Semantic states
    val Error   = Color(0xFFFF4D6A)
    val Warning = Color(0xFFFFAD42)
    val Success = Color(0xFF34E89E)

    // Surface system
    val Surface          = Color(0xFF04040C)
    val SurfaceContainer = Color(0xFF0D0D22)
    val OnSurface        = Color(0xFFECEDF5)
    val OnSurfaceMuted   = Color(0xFF9899BC)
}

// Usage: role-based, not value-based
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = ConveyColor.Primary,
        contentColor = ConveyColor.OnPrimary,
    )
)`} />
          <div className="p-5 rounded-2xl bg-card border border-wire/6">
            <p className="font-mono text-xs text-ink-muted uppercase tracking-widest mb-3">Color Roles in Context</p>
            <div className="rounded-xl overflow-hidden border border-wire/8">
              <div className="px-4 py-3 flex items-center justify-between" style={{ background: '#0D0D22' }}>
                <span className="text-[#ECEDF5] text-sm font-display font-semibold">Card Title</span>
                <span className="text-[#9899BC] text-xs font-mono">OnSurfaceMuted</span>
              </div>
              <div className="px-4 py-3 flex items-center gap-3" style={{ background: '#080818' }}>
                <div className="flex-1 h-2 rounded-full" style={{ background: '#131330' }}>
                  <div className="h-full rounded-full w-2/3" style={{ background: '#7B56F8' }} />
                </div>
                <span className="text-[#7B56F8] text-xs font-mono">Primary</span>
              </div>
              <div className="px-4 py-3 flex gap-2" style={{ background: '#0D0D22' }}>
                <button className="px-3 py-1.5 rounded-lg text-xs font-mono" style={{ background: '#7B56F8', color: '#FFFFFF' }}>Primary</button>
                <button className="px-3 py-1.5 rounded-lg text-xs font-mono" style={{ background: '#003D33', color: '#6DF5D4' }}>Secondary</button>
                <button className="px-3 py-1.5 rounded-lg text-xs font-mono" style={{ background: '#4A1800', color: '#FF8B5E' }}>Tertiary</button>
              </div>
            </div>
          </div>
          {hovered && (
            <div className="animate-cv-scale-in p-4 rounded-xl bg-raised border border-wire/10">
              <p className="font-mono text-xs text-primary">{hovered}</p>
            </div>
          )}
        </div>
      </div>
    </Section>
  )
}

// ── Motion Section ─────────────────────────────────────────────────────────

function SpringCard({
  name, stiffness, damping, token, color, desc
}: typeof SPRING_PRESETS[number]) {
  const { value, animateTo } = useSpringAnim(stiffness, damping)
  const [toggled, setToggled] = useState(false)
  const toggle = () => {
    const next = !toggled
    setToggled(next)
    animateTo(next ? 1 : 0)
  }
  const translateY = (1 - value) * 88

  return (
    <div
      onClick={toggle}
      className="rounded-2xl bg-card border border-wire/6 p-5 cursor-pointer hover:border-wire/14 transition-colors select-none"
    >
      <div className="h-32 relative mb-4 flex items-end justify-center">
        <div className="absolute bottom-0 left-0 right-0 h-px bg-wire/10" />
        <div
          className="w-10 h-10 rounded-full absolute"
          style={{ background: color, bottom: 0, transform: `translateY(${-translateY}px)`, transition: 'none' }}
        />
      </div>
      <p className="font-mono text-xs mb-1" style={{ color }}>{token}</p>
      <p className="font-display font-bold text-ink text-base mb-2">{name}</p>
      <div className="flex gap-3 mb-3">
        <span className="font-mono text-xs text-ink-muted bg-raised px-2 py-0.5 rounded">k={stiffness}</span>
        <span className="font-mono text-xs text-ink-muted bg-raised px-2 py-0.5 rounded">d={damping}</span>
      </div>
      <p className="text-ink-muted text-xs leading-relaxed">{desc}</p>
    </div>
  )
}

function MotionSection() {
  return (
    <Section id="motion">
      <SectionHead
        eyebrow="ConveyMotion"
        title="Motion"
        subtitle="Motion is grammar. One animation signature carries one meaning. Physics-based springs train muscle memory through repetition."
      />
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-12">
        {SPRING_PRESETS.map(s => <SpringCard key={s.name} {...s} />)}
      </div>
      <p className="text-ink-muted text-xs font-mono mb-8 flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-primary animate-cv-glow inline-block" />
        click any card to animate — observe how each spring teaches a different intent
      </p>
      <div className="grid lg:grid-cols-2 gap-5">
        <CodeBlock code={`object ConveyMotion {

    // Spring presets — physics, not curves
    object Spring {
        val Snappy   = spring<Float>(stiffness = 600f, dampingRatio = 0.72f)
        val Standard = spring<Float>(stiffness = 380f, dampingRatio = 0.80f)
        val Gentle   = spring<Float>(stiffness = 200f, dampingRatio = 0.85f)
        val Bouncy   = spring<Float>(stiffness = 300f, dampingRatio = 0.42f)
    }

    // Easing curves for tween animations
    object Easing {
        val Standard             = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        val Emphasized           = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }

    // M3-aligned duration tokens
    object Duration {
        const val Short1  = 50;  const val Short4  = 200
        const val Medium1 = 250; const val Medium4 = 400
        const val Long1   = 450; const val Long4   = 600
        const val ExtraLong1 = 700; const val ExtraLong4 = 1000
    }
}`} />
        <CodeBlock code={`// Animated value with spring spec
val offsetY by animateFloatAsState(
    targetValue = if (visible) 0f else 40f,
    animationSpec = ConveyMotion.Spring.Standard,
)

// Shared element transitions
SharedTransition {
    Box(
        modifier = Modifier
            .sharedElement(rememberSharedContentState("hero-card"))
            .conveyMotion(ConveyMotion.Spring.Bouncy)
    )
}

// Compose-native spring for lists
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemCard(
            modifier = Modifier.animateItem(
                fadeInSpec = ConveyMotion.Spring.Gentle,
                placementSpec = ConveyMotion.Spring.Standard,
            )
        )
    }
}`} />
      </div>
    </Section>
  )
}

// ── Sizes Section ──────────────────────────────────────────────────────────

function SizesSection() {
  const [selected, setSelected] = useState<string | null>(null)
  return (
    <Section id="sizes">
      <SectionHead
        eyebrow="ConveySize"
        title="Size"
        subtitle="A proportional scale that ensures compositional harmony. Every token is a deliberate ratio, not an arbitrary number."
      />
      <div className="grid lg:grid-cols-2 gap-12">
        <div>
          <div className="rounded-2xl bg-card border border-wire/6 p-6 overflow-x-auto">
            <div className="flex items-end gap-3 min-w-max">
              {SIZE_SCALE.filter(s => s.px > 0).map(s => (
                <button
                  key={s.token}
                  onClick={() => setSelected(s.token === selected ? null : s.token)}
                  className="flex flex-col items-center gap-2 group cursor-pointer"
                >
                  <div
                    className="rounded transition-all duration-300"
                    style={{
                      width: s.px,
                      height: s.px,
                      background: selected === s.token ? '#7B56F8' : 'rgba(123,86,248,0.25)',
                      border: selected === s.token ? '2px solid rgba(123,86,248,0.8)' : '1px solid rgba(123,86,248,0.2)',
                    }}
                  />
                  <span className="font-mono text-[9px] text-ink-muted group-hover:text-ink-dim transition-colors text-center" style={{ writingMode: 'vertical-rl' }}>
                    {s.dp}dp
                  </span>
                </button>
              ))}
            </div>
          </div>
          {selected && (
            <div className="mt-4 p-4 rounded-xl bg-raised border border-wire/8 animate-cv-scale-in">
              <p className="font-mono text-primary text-xs">{selected}</p>
              <p className="text-ink-muted text-xs mt-1">{SIZE_SCALE.find(s => s.token === selected)?.dp}dp</p>
            </div>
          )}
          <div className="mt-6 rounded-2xl bg-card border border-wire/6 p-5">
            <p className="font-mono text-xs text-ink-muted mb-4 uppercase tracking-widest">Spacing application</p>
            <div className="flex flex-col gap-3">
              {['None', 'XSmall', 'Small', 'Medium', 'Large'].map((name, i) => {
                const sizes = [0, 4, 8, 16, 24]
                return (
                  <div key={name} className="flex items-center gap-3">
                    <span className="font-mono text-xs text-ink-muted w-20">ConveySize.{name}</span>
                    <div className="flex-1 h-px bg-wire/8" />
                    <div
                      className="bg-primary/40 rounded h-3 transition-all duration-300"
                      style={{ width: Math.max(sizes[i], 1) }}
                    />
                    <span className="font-mono text-xs text-ink-muted w-8">{sizes[i]}dp</span>
                  </div>
                )
              })}
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-5">
          <CodeBlock code={`object ConveySize {
    // Spacing scale — compositional harmony
    val None    = 0.dp
    val XSmall  = 4.dp
    val Small   = 8.dp
    val Medium  = 16.dp
    val Large   = 24.dp
    val XLarge  = 32.dp
    val XXLarge = 48.dp
    val Huge    = 64.dp
    val Hero    = 96.dp

    // Component sizes
    object Component {
        val IconSmall  = 16.dp
        val IconMedium = 24.dp
        val IconLarge  = 32.dp

        val ButtonHeight  = 40.dp
        val FabSize       = 56.dp
        val FabLargeSize  = 96.dp
        val NavigationBar = 80.dp
        val TopAppBar     = 64.dp
    }

    // Touch target — accessibility floor
    val MinTouchTarget = 48.dp
}

// Usage
Card(
    modifier = Modifier.padding(
        horizontal = ConveySize.Medium,
        vertical = ConveySize.Small,
    )
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ConveySize.Small),
        modifier = Modifier.padding(ConveySize.Large),
    ) { /* content */ }
}`} />
        </div>
      </div>
    </Section>
  )
}

// ── Transforms Section ─────────────────────────────────────────────────────

function TransformCard({ title, token, desc, transformStyle, transformHovered }: {
  title: string; token: string; desc: string;
  transformStyle: React.CSSProperties; transformHovered: React.CSSProperties
}) {
  const [hov, setHov] = useState(false)
  return (
    <div
      className="rounded-2xl bg-card border border-wire/6 p-5 overflow-hidden cursor-pointer hover:border-wire/14 transition-colors"
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}
    >
      <div className="h-28 flex items-center justify-center mb-4">
        <div
          className="w-16 h-16 bg-primary-well border border-primary/30 rounded-2xl"
          style={{
            transition: 'transform 450ms cubic-bezier(0.34,1.56,0.64,1)',
            ...(hov ? transformHovered : transformStyle),
          }}
        />
      </div>
      <p className="font-mono text-xs text-primary mb-1">{token}</p>
      <p className="font-display font-bold text-ink text-sm mb-2">{title}</p>
      <p className="text-ink-muted text-xs leading-relaxed">{desc}</p>
    </div>
  )
}

function TransformsSection() {
  return (
    <Section id="transforms">
      <SectionHead
        eyebrow="ConveyTransform"
        title="Transform"
        subtitle="Transforms reveal state through motion. Scale communicates press. Lift communicates hover. Rotation communicates direction."
      />
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
        <TransformCard
          title="Scale on Press" token="conveyTransform { scaleOnPress() }"
          desc="Provides physical press feedback. Signals interactivity before the user lifts their finger."
          transformStyle={{ transform: 'scale(1)' }}
          transformHovered={{ transform: 'scale(0.88)' }}
        />
        <TransformCard
          title="Lift on Hover" token="conveyTransform { liftOnHover() }"
          desc="Elevation increase on hover teaches the element is clickable. The shadow amplifies the affordance."
          transformStyle={{ transform: 'scale(1)', boxShadow: 'none' }}
          transformHovered={{ transform: 'scale(1.06) translateY(-4px)', boxShadow: '0 16px 40px rgba(123,86,248,0.25)' }}
        />
        <TransformCard
          title="Rotate on Hover" token="conveyTransform { rotateOnHover() }"
          desc="Subtle rotation confirms cursor position and adds energy to icon buttons and toggles."
          transformStyle={{ transform: 'rotate(0deg)' }}
          transformHovered={{ transform: 'rotate(12deg)' }}
        />
        <TransformCard
          title="Scale In Reveal" token="conveyTransform { scaleIn() }"
          desc="Entering elements scale from 0.85 to 1. The growth direction communicates origin and presence."
          transformStyle={{ transform: 'scale(0.85)', opacity: 0.3 }}
          transformHovered={{ transform: 'scale(1)', opacity: 1 }}
        />
      </div>
      <CodeBlock code={`// Modifier DSL — compose transforms declaratively
fun Modifier.conveyTransform(
    block: ConveyTransformScope.() -> Unit
): Modifier

class ConveyTransformScope {
    fun scaleOnPress(
        scale: Float = 0.94f,
        spec: AnimationSpec<Float> = ConveyMotion.Spring.Snappy,
    )
    fun liftOnHover(
        elevation: Dp = 8.dp,
        scaleUp: Float = 1.04f,
        spec: AnimationSpec<Float> = ConveyMotion.Spring.Standard,
    )
    fun rotateOnHover(
        degrees: Float = 8f,
        spec: AnimationSpec<Float> = ConveyMotion.Spring.Bouncy,
    )
    fun scaleIn(
        initial: Float = 0.85f,
        spec: AnimationSpec<Float> = ConveyMotion.Spring.Standard,
    )
    fun fadeIn(
        initial: Float = 0f,
        spec: AnimationSpec<Float> = ConveyMotion.Spring.Gentle,
    )
}

// Hover over cards above to see each transform
Button(
    modifier = Modifier.conveyTransform {
        scaleOnPress(0.96f)
        liftOnHover(12.dp, scaleUp = 1.03f)
    },
    onClick = { /* ... */ }
) { Text("Interact with me") }`} />
    </Section>
  )
}

// ── Interactions Section ───────────────────────────────────────────────────

function RippleDemo() {
  const { onRipple, RippleEl } = useRipple('rgba(123,86,248,0.40)')
  return (
    <div
      onClick={onRipple}
      className="relative overflow-hidden rounded-2xl h-36 bg-primary-well border border-primary/20 flex items-center justify-center cursor-pointer select-none"
    >
      <RippleEl />
      <div className="text-center pointer-events-none">
        <p className="font-mono text-primary text-xs mb-1">ConveyRipple.Bounded</p>
        <p className="text-on-primary-well text-sm font-display">tap to demonstrate</p>
      </div>
    </div>
  )
}

function PressDemo() {
  const [pressed, setPressed] = useState(false)
  return (
    <div
      className="relative rounded-2xl h-36 border flex items-center justify-center cursor-pointer select-none"
      style={{
        background: '#003D33', borderColor: 'rgba(0,203,169,0.25)',
        transform: pressed ? 'scale(0.93)' : 'scale(1)',
        transition: pressed
          ? 'transform 80ms cubic-bezier(0.4,0,1,1)'
          : 'transform 550ms cubic-bezier(0.34,1.56,0.64,1)',
      }}
      onMouseDown={() => setPressed(true)}
      onMouseUp={() => setPressed(false)}
      onMouseLeave={() => setPressed(false)}
      onTouchStart={() => setPressed(true)}
      onTouchEnd={() => setPressed(false)}
    >
      <div className="text-center pointer-events-none">
        <p className="font-mono text-secondary text-xs mb-1">ConveyPress.Scale(0.93f)</p>
        <p className="text-secondary-dim text-sm font-display">hold to feel</p>
      </div>
    </div>
  )
}

function LongPressDemo() {
  const [triggered, setTriggered] = useState(false)
  const { start, cancel, progress } = useLongPress(() => {
    setTriggered(true)
    setTimeout(() => setTriggered(false), 1200)
  }, 600)

  return (
    <div
      className="relative rounded-2xl h-36 border overflow-hidden flex items-center justify-center cursor-pointer select-none"
      style={{ background: '#4A1800', borderColor: 'rgba(255,139,94,0.25)' }}
      onMouseDown={start}
      onMouseUp={cancel}
      onMouseLeave={cancel}
      onTouchStart={start}
      onTouchEnd={cancel}
    >
      <div
        className="absolute bottom-0 left-0 h-1 bg-tertiary transition-none"
        style={{ width: `${progress * 100}%`, transition: progress === 0 ? 'none' : undefined }}
      />
      {triggered ? (
        <div className="animate-cv-scale-in text-center">
          <p className="text-on-tertiary text-sm font-display font-bold">Long press triggered</p>
        </div>
      ) : (
        <div className="text-center pointer-events-none">
          <p className="font-mono text-tertiary text-xs mb-1">ConveyLongPress(500ms)</p>
          <p className="text-[#FF8B5E] text-sm font-display">hold 500ms</p>
        </div>
      )}
    </div>
  )
}

function SwipeDemo() {
  const [dir, setDir] = useState<'left' | 'right' | null>(null)
  const startX = useRef(0)

  const handleStart = (x: number) => { startX.current = x }
  const handleEnd = (x: number) => {
    const delta = x - startX.current
    if (Math.abs(delta) > 30) {
      setDir(delta > 0 ? 'right' : 'left')
      setTimeout(() => setDir(null), 1000)
    }
  }

  return (
    <div
      className="relative rounded-2xl h-36 border flex items-center justify-center cursor-grab select-none overflow-hidden"
      style={{ background: '#131330', borderColor: 'rgba(255,255,255,0.08)' }}
      onMouseDown={e => handleStart(e.clientX)}
      onMouseUp={e => handleEnd(e.clientX)}
      onTouchStart={e => handleStart(e.touches[0].clientX)}
      onTouchEnd={e => handleEnd(e.changedTouches[0].clientX)}
    >
      {dir ? (
        <div className="animate-cv-scale-in text-center">
          <p className="text-ink text-sm font-display font-bold">Swiped {dir}</p>
          <p className="text-ink-muted text-xs font-mono">onSwipe(SwipeDirection.{dir === 'left' ? 'Left' : 'Right'})</p>
        </div>
      ) : (
        <div className="text-center">
          <p className="font-mono text-ink-muted text-xs mb-2">ConveySwipeable</p>
          <div className="flex items-center gap-2 text-ink-dim text-sm font-display">
            <span className="animate-cv-swipe">←</span>
            swipe to dismiss
            <span className="animate-cv-swipe" style={{ animationDirection: 'reverse' }}>→</span>
          </div>
        </div>
      )}
    </div>
  )
}

function InteractionsSection() {
  return (
    <Section id="interactions">
      <SectionHead
        eyebrow="ConveyInteraction"
        title="Interaction"
        subtitle="Gesture response teaches users what elements do. Ripple marks the touch point. Press scale confirms receipt. Each confirms a different layer of intent."
      />
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
        <RippleDemo />
        <PressDemo />
        <LongPressDemo />
        <SwipeDemo />
      </div>
      <CodeBlock code={`// Bounded ripple — confirms tap position
fun Modifier.conveyRipple(
    color: Color = ConveyColor.OnSurface,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
): Modifier

// Press scale — immediate physical feedback
fun Modifier.conveyPress(
    scale: Float = 0.94f,
    spec: AnimationSpec<Float> = ConveyMotion.Spring.Snappy,
    onClick: (() -> Unit)? = null,
): Modifier

// Long press — progressive disclosure
fun Modifier.conveyLongPress(
    durationMs: Long = 500L,
    haptic: Boolean = true,
    showProgress: Boolean = true,
    onLongPress: () -> Unit,
): Modifier

// Swipe — directional intent
fun Modifier.conveySwipeable(
    directions: Set<SwipeDirection> = SwipeDirection.Horizontal,
    threshold: Dp = 56.dp,
    onSwipe: (direction: SwipeDirection, progress: Float) -> Unit,
): Modifier

// Composed: complete interactive surface
Box(
    modifier = Modifier
        .conveyRipple(color = ConveyColor.Primary)
        .conveyPress(scale = 0.96f, onClick = { handleAction() })
        .conveyLongPress { showContextMenu() }
)`} />
    </Section>
  )
}

// ── Animations Section ─────────────────────────────────────────────────────

type ButtonState = 'idle' | 'loading' | 'success'

function ProgressButton() {
  const [state, setState] = useState<ButtonState>('idle')
  const handle = () => {
    if (state !== 'idle') return
    setState('loading')
    setTimeout(() => setState('success'), 2000)
    setTimeout(() => setState('idle'), 3600)
  }
  const isCollapsed = state !== 'idle'
  return (
    <div className="flex items-center justify-center h-28">
      <button
        onClick={handle}
        disabled={state !== 'idle'}
        className="flex items-center justify-center bg-primary text-white overflow-hidden"
        style={{
          width: isCollapsed ? 52 : 148,
          height: 52,
          borderRadius: isCollapsed ? '50%' : 16,
          cursor: state === 'idle' ? 'pointer' : 'default',
          transition: 'width 420ms cubic-bezier(0.34,1.56,0.64,1), border-radius 420ms cubic-bezier(0.34,1.56,0.64,1)',
          boxShadow: '0 4px 24px rgba(123,86,248,0.35)',
        }}
      >
        {state === 'idle' && <span className="font-display font-bold text-sm whitespace-nowrap">Submit Form</span>}
        {state === 'loading' && (
          <svg className="animate-cv-spin" width="22" height="22" viewBox="0 0 22 22" fill="none">
            <circle cx="11" cy="11" r="9" stroke="rgba(255,255,255,0.25)" strokeWidth="2.5" />
            <path d="M11 2a9 9 0 019 9" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
          </svg>
        )}
        {state === 'success' && (
          <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
            <path
              d="M4 11l5.5 5.5 8.5-9"
              stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
              strokeDasharray="32"
              style={{ animation: 'cv-check 450ms cubic-bezier(0.34,1.56,0.64,1) forwards' }}
            />
          </svg>
        )}
      </button>
    </div>
  )
}

function FabHero() {
  const [open, setOpen] = useState(false)
  const actions = [
    { icon: '✦', label: 'New Project', color: '#7B56F8' },
    { icon: '⬡', label: 'Import File', color: '#00CBA9' },
    { icon: '⬟', label: 'Quick Capture', color: '#FF8B5E' },
  ]
  return (
    <div className="relative rounded-2xl bg-card border border-wire/6 overflow-hidden" style={{ height: 320 }}>
      <div className="absolute inset-0 p-5">
        <p className="font-mono text-xs text-ink-muted">ConveyStateButton · FAB → Sheet</p>
      </div>
      {open && (
        <div
          className="absolute inset-0 backdrop-blur-sm"
          style={{ background: 'rgba(4,4,12,0.7)' }}
          onClick={() => setOpen(false)}
        />
      )}
      {open && (
        <div className="absolute bottom-24 right-6 flex flex-col gap-2 z-10">
          {actions.map((a, i) => (
            <div
              key={a.label}
              className="flex items-center gap-3 px-4 py-3 rounded-xl bg-raised border border-wire/10 animate-cv-slide-up"
              style={{ animationDelay: `${i * 50}ms` }}
            >
              <span style={{ color: a.color }} className="text-base">{a.icon}</span>
              <span className="font-display font-semibold text-ink text-sm">{a.label}</span>
            </div>
          ))}
        </div>
      )}
      <button
        onClick={() => setOpen(o => !o)}
        className="absolute bottom-6 right-6 z-10 w-14 h-14 bg-primary flex items-center justify-center shadow-[0_8px_32px_rgba(123,86,248,0.4)]"
        style={{
          borderRadius: open ? '16px' : '50%',
          transform: open ? 'rotate(45deg)' : 'rotate(0deg)',
          transition: 'border-radius 400ms cubic-bezier(0.34,1.56,0.64,1), transform 400ms cubic-bezier(0.34,1.56,0.64,1)',
        }}
      >
        <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
          <path d="M11 4v14M4 11h14" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
        </svg>
      </button>
    </div>
  )
}

function MorphCard() {
  const [expanded, setExpanded] = useState(false)
  return (
    <div
      onClick={() => setExpanded(e => !e)}
      className="rounded-2xl bg-card border border-wire/6 cursor-pointer overflow-hidden transition-all duration-700"
      style={{
        padding: expanded ? 24 : 20,
        borderRadius: expanded ? '16px' : '28px',
      }}
    >
      <p className="font-mono text-xs text-ink-muted mb-2">ConveyMorph · tap to {expanded ? 'collapse' : 'expand'}</p>
      <h3 className="font-display font-bold text-ink text-lg leading-tight">
        Conveyance Card
      </h3>
      <div
        className="overflow-hidden transition-all duration-500"
        style={{ maxHeight: expanded ? 200 : 0, opacity: expanded ? 1 : 0 }}
      >
        <p className="text-ink-dim text-sm leading-relaxed mt-4">
          This card demonstrates shape morphing — the corner radius softens as content expands, and the padding breathes. The element teaches its own expandability by the way it moves.
        </p>
        <div className="mt-4 flex gap-2">
          <span className="px-3 py-1 rounded-full bg-primary/15 text-primary text-xs font-mono">Conveyance</span>
          <span className="px-3 py-1 rounded-full bg-secondary-well text-secondary-dim text-xs font-mono">Kotlin</span>
        </div>
      </div>
      <div className="mt-3 flex items-center gap-2 text-ink-muted">
        <div
          className="w-4 h-4 rounded-full border border-ink-muted/40 flex items-center justify-center transition-transform duration-400"
          style={{ transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)' }}
        >
          <svg width="8" height="8" viewBox="0 0 8 8" fill="none">
            <path d="M1.5 3L4 5.5L6.5 3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </div>
        <span className="text-xs font-mono">{expanded ? 'less' : 'more'}</span>
      </div>
    </div>
  )
}

function AnimationsSection() {
  return (
    <Section id="animations">
      <SectionHead
        eyebrow="ConveyAnimation"
        title="Animations"
        subtitle="The hero moments. Reserve peak expression for the emotional core of your product. A system that understands user intent proves itself here."
      />
      <div className="grid lg:grid-cols-3 gap-6 mb-10">
        <div>
          <p className="font-mono text-xs text-ink-muted mb-3 uppercase tracking-widest">Progress Button</p>
          <div className="rounded-2xl bg-card border border-wire/6 p-5">
            <p className="font-mono text-xs text-ink-muted mb-4">ConveyStateButton: idle → loading → success</p>
            <ProgressButton />
          </div>
        </div>
        <div>
          <p className="font-mono text-xs text-ink-muted mb-3 uppercase tracking-widest">FAB Expansion</p>
          <FabHero />
        </div>
        <div>
          <p className="font-mono text-xs text-ink-muted mb-3 uppercase tracking-widest">Shape Morph</p>
          <MorphCard />
        </div>
      </div>
      <CodeBlock code={`// ConveyStateButton — one element, three states
@Composable
fun ConveyStateButton(
    onClick: () -> Unit,
    state: ConveyButtonState = ConveyButtonState.Idle,
    shape: Shape = ConveyShape.Large,
    spec: AnimationSpec<Float> = ConveyMotion.Spring.Snappy,
    idleContent: @Composable () -> Unit,
    loadingContent: @Composable () -> Unit = { ConveySpinner() },
    successContent: @Composable () -> Unit = { ConveyCheckmark() },
)

// ConveyFab — FAB that teaches its own expansion
@Composable
fun ConveyFab(
    onClick: () -> Unit,
    expanded: Boolean = false,
    expandSpec: AnimationSpec<Float> = ConveyMotion.Spring.Standard,
    actions: List<ConveyFabAction> = emptyList(),
    icon: @Composable () -> Unit,
)

// ConveyMorph — shape and layout morph
@Composable
fun ConveyMorph(
    visible: Boolean,
    fromShape: Shape = ConveyShape.Medium,
    toShape: Shape = ConveyShape.Circle,
    spec: AnimationSpec<Float> = ConveyMotion.Spring.Standard,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
)

// ConveyReveal — circular reveal from origin
@Composable
fun ConveyReveal(
    visible: Boolean,
    origin: Offset = Offset.Unspecified,
    spec: AnimationSpec<Float> = ConveyMotion.Spring.Bouncy,
    content: @Composable () -> Unit,
)`} />
    </Section>
  )
}

// ── Footer ─────────────────────────────────────────────────────────────────

function Footer() {
  return (
    <footer className="border-t border-wire/6 py-16 px-6 md:px-14">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-start md:items-center justify-between gap-8">
        <div>
          <p className="font-display font-black text-ink text-2xl tracking-wider mb-1">CONVEY</p>
          <p className="font-mono text-xs text-ink-muted">compose.conveyance · Compose Multiplatform</p>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-xs font-mono text-ink-muted">
          {['ConveyShape', 'ConveyColor', 'ConveyMotion', 'ConveySize', 'ConveyTransform', 'ConveyInteraction', 'ConveyAnimation'].map(m => (
            <span key={m} className="hover:text-primary-dim transition-colors cursor-default">{m}</span>
          ))}
        </div>
        <div className="text-xs font-mono text-ink-muted text-right">
          <p className="text-primary-dim mb-1">Conveyance Manifesto</p>
          <p>Every element earns its place.</p>
          <p>Motion is grammar.</p>
        </div>
      </div>
    </footer>
  )
}

// ── App ────────────────────────────────────────────────────────────────────

export default function App() {
  const [activeSection, setActiveSection] = useState('')

  useEffect(() => {
    const observer = new IntersectionObserver(
      entries => {
        entries.forEach(entry => {
          if (entry.isIntersecting) setActiveSection(entry.target.id)
        })
      },
      { threshold: 0.3 },
    )
    NAV_ITEMS.forEach(item => {
      const el = document.getElementById(item.id)
      if (el) observer.observe(el)
    })
    return () => observer.disconnect()
  }, [])

  return (
    <div className="bg-canvas text-ink min-h-screen">
      <NavBar active={activeSection} />
      <main>
        <HeroSection />
        <div className="max-w-7xl mx-auto">
          <ShapesSection />
          <ColorsSection />
          <MotionSection />
          <SizesSection />
          <TransformsSection />
          <InteractionsSection />
          <AnimationsSection />
        </div>
      </main>
      <Footer />
    </div>
  )
}
