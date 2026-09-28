# AstroChat · Add Money (insufficient balance) — Jetpack Compose

A working Compose rebuild of the Add Money / insufficient-balance screen, built as a **reference
for developers**: real tokens, real components, real motion, no ViewModel and no networking. Lift
`ui/theme/Tokens.kt` and the `ui/components/` files into the app; everything else is scaffolding.

Reference prototype (the source of truth for all numbers below):
https://astrochat-pricing-v2.vercel.app/

## Run it

```
./gradlew installDebug
adb shell monkey -p com.astrochat.insufficient 1
```

Requires JDK 17+ (Android Studio's bundled JBR works). No API keys, no `local.properties` entries.

## READ THIS BEFORE FIXING ANY "IT DOESN'T LOOK LIKE THE PROTOTYPE" BUG

The prototype is a **360px-wide frame** scaled to the viewport with
`transform: scale(viewport / 360)`. To make 1 Compose `dp` equal 1 prototype design px, this app
overrides `LocalDensity` by `screenWidthDp / 360` (see `MainActivity.kt`). So:

- every dp in this codebase is a **prototype design pixel**, not a physical Android dp;
- on a 1440px-wide device that is exactly 4 device px per design dp;
- comparing a screenshot against the prototype only means anything if both are at the same width.

Do not "correct" a size because it looks off on a device without measuring it first.

---

# Motion tokens

All curves and durations live in one place — **`Tokens.Motion`** in
`app/src/main/java/com/astrochat/insufficient/ui/theme/Tokens.kt`. Use the token, not a literal.

```kotlin
object Motion {
    val easeOut: Easing    = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)   // the house curve
    val easeGentle: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)   // settling, not responding
    val spring: Easing     = CubicBezierEasing(0.2f, 1.3f, 0.4f, 1f)   // overshoots on purpose
    val easeInOut: Easing  = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)   // CSS ease-in-out

    const val expandMs = 360;  const val collapseMs = 520
    const val fadeMs = 300;    const val tileSelectMs = 280
    const val popupInMs = 400; const val popupHoldMs = 2300; const val popupOutMs = 250
    const val bandSwapMs = 420
    const val popupBadgeDelayMs = 300; const val popupBadgeWiggleMs = 1200
    const val chipSweepMs = 3200      // one pass of light across a bonus badge
    const val bandSweepMs = 2800      // the same idea on the coupon band
    const val summaryOpenMs = 400; const val veilFadeMs = 300
}
```

`spring` overshoots (y = 1.3 > 1). That is deliberate. Do not clamp it.

The three arc curves for the badge flight are private to `AmountTile.kt` because nothing else may
use them:

```kotlin
private val ARC_LAUNCH = CubicBezierEasing(0.12f, 0.8f,  0.25f, 1f)  // off the tile
private val ARC_HANG   = CubicBezierEasing(0.45f, 0.0f,  0.55f, 1f)  // the direction change
private val ARC_FALL   = CubicBezierEasing(0.4f,  0.0f,  0.8f,  0.7f) // into the card
```

## Two Compose helpers you will need

Compose has no equivalent of a CSS `@keyframes` block with more than two stops, and
`animateFloatAsState(1f)` will not animate when start and end are the same value. Both problems
are solved in `InsufficientScreen.kt`:

```kotlin
/** Reads a 0..1 run back as a multi-stage curve — this IS a CSS @keyframes block. */
private fun stages(p: Float, vararg stops: Pair<Float, Float>): Float { … }

private fun tiltCurve(p: Float)   = stages(p, 0f to 0f, 0.30f to 1f, 0.62f to -0.39f, 1f to 0f)
private fun absorbCurve(p: Float) = stages(p, 0f to 0f, 0.24f to 0.14f, 0.58f to -0.025f, 1f to 0f)
```

Drive them with an `Animatable` you `snapTo(0f)` then `animateTo(1f)` — **not**
`animateFloatAsState`. An animation whose start and end are both `0` gets optimised away.

---

## 1 · Light sweep — `Sweep.kt`, `AlertBand.kt`, `AmountTile.kt`

One bar of light crossing a surface, then a pause. Two surfaces use it and they must slant the
same way, so the drawing is shared in `DrawScope.drawSweepBar()`.

**The bar is diagonal, and that is the entire effect.** Two things make it so:

```kotlin
private const val SKEW_TAN = 0.3249f      // skewX(-18deg) — the bar is a parallelogram
private const val GRADIENT_DX = 0.9848f   // linear-gradient(100deg, …) — the FADE is tilted too,
private const val GRADIENT_DY = 0.1736f   // so soft edges run parallel to the slanted sides
```

Drawn upright it reads as a skeleton-loader shimmer sliding sideways. Slanted, it reads as light
glancing off a surface. Also: build the gradient with **explicit `start`/`end` offsets**. A
`Brush.horizontalGradient` with no bounds resolves against the whole component, and the bar comes
out flat.

**Badge sweep** (`BonusBadge`) — `chipSweepMs` 3200, `LinearEasing`, infinite:

```kotlin
val phase = (sweepT + sweepDelayMs / Tokens.Motion.chipSweepMs.toFloat()) % 1f
val run   = (phase / 0.42f).coerceAtMost(1f)      // crosses in 42%, waits for 58%
val band  = size.width * 0.38f
drawSweepBar(left = band * (-1.2f + 4.2f * run), width = band, stops = …)
```

- Stagger is **380ms per tile** (`sweepDelayMs`) so the row does not pulse in unison.
- The pause is the point. A bar that crosses continuously reads as a barber pole.
- It only runs while the selected amount earns **nothing** — it is bait for a bonus you are not
  taking. Pick ₹100 and the badges go still. It is also off whenever a countdown is on the band.
  Switch it off by not drawing it (`if (!sweep) return@drawWithContent`), never by pausing the
  clock — a paused sweep freezes mid-pill as a visible pale streak.

**Band sweep** (`AlertBand.kt`, coupon band only — the offer band deliberately has none) —
`bandSweepMs` 2800:

```kotlin
val run = ((t - 0.40f) / 0.40f).coerceIn(0f, 1f)   // still 40%, travels 40%, still 20%
drawSweepBar(left = size.width * (-0.60f + 1.90f * run), width = size.width * 0.40f, …)
```

## 2 · Card movement on SKU change — `InsufficientScreen.kt`

Every tap replays one tip of the congratulations card. One animation on the card's own transform;
nothing inside it moves independently.

```kotlin
val tiltRun = remember { Animatable(1f) }

LaunchedEffect(selected) {
    launch {
        tiltRun.snapTo(0f)
        tiltRun.animateTo(1f, tween(620, easing = Tokens.Motion.easeOut))
    }
    …
}
// read back: tiltCurve(tiltRun.value) → rotateX 9°, rotateY -6°, scale 1.03 at its peak
```

Keying the `LaunchedEffect` on `selected` is what gives the replay. If a re-tap of the
**same** amount must also replay it, key on a counter you bump per tap instead.

The tile being tapped runs its own, in `AmountTile.kt`:

| Element | Kotlin |
|---|---|
| Press | `animateFloatAsState(if (pressed) 0.97f else 1f, tween(120))` |
| Orange border + corner | `wedge.animateTo(1f, tween(280, easing = Tokens.Motion.spring))` |
| Corner tick | `tick.animateTo(1f, tween(350, delayMillis = 80, easing = Tokens.Motion.spring))` |

The delay on the tick is what makes the corner read as arriving first and the tick as landing into
it. These three are also available as a standalone Lottie on a transparent background —
`sku-selected.json`, see the Lottie section below.

## 3 · Number animation — `InsufficientScreen.kt`

The card figure **counts** because the number is the reward: a value that lands instantly reads as
a label, one that climbs reads as something being earned.

```kotlin
private suspend fun countTo(from: Int, to: Int, durationMs: Int, set: (Int) -> Unit) {
    if (from == to) { set(to); return }
    animate(from.toFloat(), to.toFloat(), animationSpec = tween(durationMs, easing = Tokens.Motion.easeOut)) {
        v, _ -> set(v.roundToInt())
    }
    set(to)
}
```

Three phases, **sequential on purpose** — the bonus must not start moving before the thing that
earns it has arrived:

```kotlin
countTo(shown, selected, 360) { shown = it }             // 1. up to the recharge amount
flight = Flight(bonus, from, 0f)                         // 2. throw the chip (640ms)
animate(0f, 1f, tween(640, easing = LinearEasing)) { p, _ -> flight = flight?.copy(progress = p) }
flight = null
launch { absorbRun.snapTo(0f); absorbRun.animateTo(1f, tween(520, easing = LinearEasing)) }
countTo(selected, credit, 700) { shown = it }            // 3. count the bonus on as it lands
```

It is **skipped entirely** — straight `countTo(shown, credit, 360)` — when any of these hold:

1. the bonus is zero (₹50 — nothing to count on);
2. the badge or the figure has not been measured yet;
3. **the amount has already dropped once this visit.** `dropped.add(selected)` returns false on a
   repeat. Pick ₹100 and it flies; come back to ₹100 later and only the figure refreshes. A chip
   that replays on every tap stops reading as a reward.

Also: `LaunchedEffect(credit) { if (flight == null) shown = credit }` — anything that changes the
figure without a pick (the countdown flipping the band onto a coupon) still has to keep the card
honest, but must not fight a running flight.

## 4 · Badge drop — `AmountTile.kt`, `BonusFlyer`

The screen's signature move, and the thing most likely to be got wrong. The selected tile's
"+₹150 more" pill lifts off, arcs down, and is absorbed by the card; the card starts counting at
the moment it lands. Without it the figure just changes and nothing tells the user where the
number came from.

```kotlin
val (leg, q) = when {
    p < 0.22f -> 0 to ARC_LAUNCH.transform(p / 0.22f)
    p < 0.34f -> 1 to ARC_HANG.transform((p - 0.22f) / 0.12f)
    else      -> 2 to ARC_FALL.transform((p - 0.34f) / 0.66f)
}
when (leg) {
    0 -> { tx = lerp(0f, dx*0.03f);      ty = lerp(0f, apex);  s = lerp(1f, 0.92f);    rot = lerp(0f, -9f) }
    1 -> { tx = lerp(dx*0.03f, dx*0.12f); ty = lerp(apex, hang); s = lerp(0.92f, 0.86f); rot = lerp(-9f, -3f) }
    else -> { tx = lerp(dx*0.12f, dx);   ty = lerp(hang, dy);  s = lerp(0.86f, 0.34f); rot = lerp(-3f, 5f) }
}
```

`apex = -76.dp`, `hang = -68.dp`. Six things are load-bearing:

1. **Render it at ROOT level**, not inside the card. It travels across both halves of the screen;
   anything parented lower gets clipped by the receipt card on the way down.
2. **Measure both ends at call time** (`badgeBounds` / `figureAt`, in root coordinates,
   centre-to-centre). The tiles move when the row expands and the card moves when the summary
   opens, so any hardcoded path is wrong half the time.
3. **The velocity profile IS the brief: fast → almost stopped → fastest on impact.** The obvious
   version — a sine lift over an ease-OUT fall — gets the shape roughly right and the speed exactly
   backwards. It arrives slowly and stops, which reads as the pill being set down instead of
   caught. That is the difference between this looking like the prototype and not.
4. **Drive the whole flight with `LinearEasing` and carry the easing per leg.** A single curve over
   the whole animation remaps progress and the pill arrives long before the count starts.
5. **The fall is ONE interval.** Every extra waypoint restarts its own easing from zero velocity
   and puts a visible hitch just before the card. The 86% opacity step does not split it, because
   alpha is interpolated separately from the transform.
6. **Scale only ever decreases — every value ≤ 1.** It reads as the card absorbing the pill; an
   overshoot above 1 at the apex reads as the pill popping at the user.

`transformOrigin = TransformOrigin.Center` so scale and rotation pivot on the chip's middle as CSS
does, while `translationX/Y` still place its top-left. The flyer reuses `BonusBadge` itself
(`elevated = true`) so its fill can never drift from the badge it left.

Landing puts arrival on the same frame as the figure's hit — `absorbCurve`, 520ms: +14% at 24%,
−2.5% at 58%, flat by the end.

## 5 · Payment summary — `InsufficientScreen.kt`

```kotlin
AnimatedVisibility(
    visible = summaryOpen,
    enter = expandVertically(tween(Tokens.Motion.summaryOpenMs, easing = Tokens.Motion.easeOut)) +
            fadeIn(tween(Tokens.Motion.fadeMs, delayMillis = 100)),
    exit  = shrinkVertically(tween(Tokens.Motion.summaryOpenMs, easing = Tokens.Motion.easeOut)) +
            fadeOut(tween(200))
)
```

The fade is **asymmetric on purpose**: 300ms with a 100ms delay opening, 200ms with no delay
closing. Opening, the box starts moving before the text appears; closing, the text is gone before
the box finishes collapsing. Symmetric timings make the text visibly collide with the card edge on
the way down. Do not "tidy" these into one duration.

The card's own padding and corner radius loosen with it (350ms), and the chevron flips 180° over
400ms on `easeOut` — slightly slower than the height, so the arrow is still turning as the box
settles.

---

## Known deltas from the prototype

Honest list. None is a bug you need to chase; each is a decision or a gap.

| | Prototype | Here |
|---|---|---|
| Card tilt curve | `cubic-bezier(.22,.9,.28,1)` | `Motion.easeOut` `(.2,.8,.2,1)` |
| Selection border / tick | 280ms `(.2,.8,.2,1)`; tick 500ms `(.3,1.5,.5,1)` @50ms | 280ms `Motion.spring`; tick 350ms @80ms |
| Band sweep travel | eased `ease-in-out` across the 40→80% window | linear across the same window |
| Loading skeleton shimmer | `shimmer 4.2s ease-in-out` | not ported |

## Lottie

Five of these animations are also published as standalone Lottie JSONs, so they can be dropped in
without reimplementing them — the popup discount seal, the SKU selection marks (border + corner +
tick, transparent background), both band seals, and the wallet chip. Links are in the
**Behaviour rules → Lottie files** panel on the right of the prototype:
https://astrochat-pricing-v2.vercel.app/

The CSS-side version of everything above, for web devs, is in the prototype repo:
https://github.com/radhikarathi256-art/astrochat-pricing-v2#motion-tokens
