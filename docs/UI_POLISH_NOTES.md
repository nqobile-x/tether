# UI Polish Pass: Change Record

Every change from the polish pass, with the reasoning. No BLE, location, or alerting logic was touched.

| Before | After | Why |
|---|---|---|
| No shared motion utilities | New `ui/common/Motion.kt` with `pressScale`, `staggeredEntrance`, and `rememberReducedMotion` | One place to keep timing and reduced-motion behavior consistent across screens |
| Buttons and tappable cards gave only the default ripple | Every primary tappable (Send Test Alert, quick access cards, Scan Again, Pair, contacts FAB, history rows) scales to 0.97f on press, 140ms ease-out via `graphicsLayer` | Feedback: confirms the tap registered, on the compositing layer so it never triggers recomposition |
| Home status card snapped instantly between connected and disconnected colors | Container and content colors animate with `spring(DampingRatioLowBouncy, StiffnessLow)` | State indication: this is the emotionally central moment of the app, a calm spring reads as trustworthy where a snap reads as glitchy |
| Home status icon and copy swapped with no transition | `AnimatedContent`: enter fades in over 220ms and scales from 0.95f with the same low-stiffness spring, exit fades out in 120ms at 0.97f | State indication with asymmetric enter/exit, and nothing enters from scale 0, nothing in a real interface appears out of nothing |
| Pairing scan indicator and Scan Again button swapped instantly | Crossfade via `AnimatedContent`, 200ms in, 120ms out | Prevents a jarring layout jump between the two states, system response (exit) is faster than the new state settling in |
| List items in Pairing, Contacts, History appeared all at once | `itemsIndexed` with stable keys, `animateItem()`, and a 40ms-per-item entrance stagger (fade plus 8dp upward drift, 220ms, capped at 8 items) | Spatial continuity: a stagger reads as the list arriving, not flooding, the cap keeps long lists from feeling slow |
| List reorders and deletes jumped instantly | `Modifier.animateItem()` with stable keys on all three LazyColumns | Spatial continuity for deletes, primary-contact reorder, and newly logged alerts |
| Empty states ("no devices," "no contacts," "no alerts") popped in instantly | `AnimatedVisibility` fade-in (220ms) plus slight upward slide, fade-out only (120ms) | Empty states should settle in, not pop, and the faster exit keeps the system feeling responsive |
| No reduced-motion handling | `rememberReducedMotion` reads `Settings.Global.ANIMATOR_DURATION_SCALE`, and when it is 0 all scale and translation motion is dropped while alpha and color transitions are kept | Accessibility: respects the system animation preference without making state changes invisible |
| Settings screen | Unchanged | Only animate with a purpose: its Switch and Slider already have native Material motion, nothing else there has a one-sentence reason to move |

All durations are within the discipline: press feedback 140ms, crossfades and entrances 200-220ms, exits 120ms, nothing over 300ms.
