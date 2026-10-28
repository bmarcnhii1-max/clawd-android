<p align="center">
  <a href="README.md"><strong>English</strong></a> | <a href="README.ru.md"><strong>Русский</strong></a>
</p>

# Clawd for Android

The same pixel pet as the Windows desktop version — but **without AI and without network**. The manifest does not even request the `INTERNET` permission, so the app physically cannot send anything anywhere.

A tiny pixel creature that lives on top of your Android screen: it walks around, sleeps, dreams, reacts to taps, can be petted, dragged, and flung across the display. It also **dances to your music** and can be raised as a **tamagotchi** with a life counter.

## Features

- **Lives on top of every app** — a system overlay window backed by a foreground service.
- **Only the pet itself is touchable.** The interactive window is sized exactly to the sprite body (≈220×151 px); the speech bubble and particles live in a separate window with `FLAG_NOT_TOUCHABLE`. Taps on YouTube, icons, and everything underneath go to that app, never to the pet.
- **Tap** — the pet hops and replies; a quick second tap is a double jump with sparks; 4 quick taps in a row makes it offended.
- **Long press** — you are petting it: hearts appear.
- **8 taps in a row** — the pet gets fed up, **dissolves into pixels right where it stands** (it does not fly away), hides for 20 seconds, then reassembles itself out of pixels.
- **Drag** — move the pet with your finger; a fast swipe flings it across the whole screen, bouncing off walls and floor.
- **Sleep** — after N minutes without attention (configurable: 1 / 3 / 10 minutes, or never) it falls asleep and **evaporates into pixels**. While asleep it emits floating "Z"s and shows dream thoughts ("I dream of a sea of pixels…"). A tap reassembles it.
- **Dances to music** — while a track is playing (Spotify, YouTube Music, anything) the pet bounces on the beat, waves its arms and blushes; on pause it stops. Detected via `AudioManager.isMusicActive`, so system sounds and ringtones are ignored. Toggle: "Dances to music", **on by default**.
- **Tamagotchi mode** — a second screen with a full life cycle: mood, food and energy meters, feeding, playing, sleeping, and death by neglect. Reviving needs a secret code.
- **Pixel sprite 1:1 with the desktop version** (the same `sprite.py`): paws, arms, eyes, blush, squash-and-stretch.
- **2 languages** for phrases and the whole UI: RU / EN.
- **3 sizes and 6 colors** (Claude, Mint, Lavender, Sky, Pink, Graphite) with a live animated preview in the settings.
- **Toggles** — "Wanders around" (off by default, saves battery), "Talks", "Gravity", "Dances to music".
- **Position is remembered**; the pet never climbs onto the launcher dock or the navigation bar. The "Reset position" button returns it to the corner.
- **Keeps working with the screen off** (mirroring via `scrcpy`): the pet stays visible in the mirror window but does not waste battery wandering.
- **"Hide" button** removes the pet from the screen without stopping the service; bring it back from the notification.
- **Dark interface and dark speech bubbles**, with its own pixel launcher icon and notification icon.
- **Animations 1:1 with the desktop version**: body tilt in flight and while dragging, stretch on jump, pulse while being petted, 7 Hz walk cycle, 5 Hz arm wave.
- **The pet cannot physically leave the screen**: jump height, flight, dragging, and "fleeing" are all clamped to the window bounds.

## Architecture

The app is a foreground service plus two overlay windows:

| Layer | Window | Flags | Purpose |
| --- | --- | --- | --- |
| Visual | `view` | `NOT_TOUCHABLE`, `NOT_FOCUSABLE`, `NOT_TOUCH_MODAL`, alpha 0.8 | Speech bubble, dream bubbles, particles (hearts, sparks, "Z") |
| Interactive | `hitView` | `NOT_FOCUSABLE`, `NOT_TOUCH_MODAL` | Sprite body, receives touch events |

Both windows are `TYPE_APPLICATION_OVERLAY` with `FLAG_LAYOUT_NO_LIMITS` and `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`, so the pet can be positioned across cutouts and the full screen.

The **hit window is re-sized on every frame** to match the pet's actual drawn geometry, rotated to the sprite's body tilt. It uses two rectangular footprints in an 18×13 grid:

- body with paws: cells 1..16 × 2..11
- sleeping pile: cells 4..13 × 8..10

That is what makes empty regions of the overlay transparent to touches — the pet never steals taps from the app below.

### State machine

`PetOverlay` runs a small state machine: `idle`, `walk`, `sleep`, `fly`, `drag`, `pet`, `flee`. Transitions are driven by touch gestures, timers, and system broadcasts; every state has its own tick rate.

### Sleep and dissolve

Sleeping is not a hidden sprite — the body is genuinely **dissolved cell by cell**. A deterministic per-cell hash decides the order in which pixels evaporate, so the effect is identical every time. The same hash drives the reassembly when the pet wakes up.

### Music detection

`PetService` polls `AudioManager.isMusicActive` once per second — the only poll in the app, and it runs **only while the dance toggle is on**. When the state flips, the overlay is told to start or stop dancing. A dancing pet hops on a 560 ms beat, waves its arms and blushes; the sleep timeout and gravity are suspended while it dances, so the pet keeps dancing instead of falling asleep mid-track.

### Tamagotchi

The tamagotchi screen keeps its state in `SharedPreferences` and computes decay from real time, so the pet keeps aging even while the screen is closed:

- Three stats — mood, food, energy — each on a 0..10 scale.
- Every 20 minutes of real time, all three drop by one.
- While sleeping, energy regenerates every 12 seconds instead.
- When all three hit zero, the pet dies.
- **Reviving requires a code.** The app stores only SHA-256 hashes of the codes (`Secret.kt`), never the codes themselves; the entered text is hashed and compared. The riddle is documented in the app's own hint — the hashes live in the source on GitHub.

## Modules

| File | Purpose |
| --- | --- |
| `MainActivity.kt` | Settings screen: language, size, color, sleep timer, toggles, permission shortcuts, live preview, start / stop / hide / reset, entry point to the tamagotchi |
| `PetService.kt` | Foreground service, notification (acts as a pseudo-tray), `SCREEN_ON`/`SCREEN_OFF` and `BATTERY_LOW`/`BATTERY_OKAY` receivers, 1.5 s screen-state watchdog, 1 s music check |
| `PetOverlay.kt` | The engine: state machine, touch handling, physics, canvas rendering, particles, sleep, dissolve, dance, battery awareness |
| `PetPreviewView.kt` | Animated pet preview inside the settings and tamagotchi screens — alive / sad / sleeping / dead modes, feed and play bursts |
| `TamagotchiActivity.kt` | Tamagotchi screen: stat meters, actions, decay, death and code-based revival |
| `Secret.kt` | SHA-256 hashes of the revival codes (no plaintext secrets) |
| `Sprite.kt` | Palettes, sizes, and pixel patterns for particles (port of `sprite.py`) |
| `I18n.kt` | RU / EN UI strings (port of `i18n.py`) |
| `Phrases.kt` | Pet dialogue — greetings, idle chatter, dreams, reactions, dance lines (port of `phrases.py`, no AI keys) |
| `Prefs.kt` | `SharedPreferences` wrapper for all settings, the saved position, and the tamagotchi state |

## Tech stack

| Component | Version |
| --- | --- |
| Language | Kotlin 2.0.20 |
| Android Gradle Plugin | 8.6.1 |
| Gradle | 8.9 |
| JDK | 17 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| UI | Android Views + XML layouts, no Compose |
| Rendering | `Canvas` drawing, no game engine, no spritesheets |
| Dependencies | **Zero** third-party libraries — only the Android SDK |

The app is a single `:app` module with an empty `dependencies { }` block. Everything is drawn by hand with `Canvas.drawRect` on a pixel grid.

## Permissions

| Permission | Why |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Draw the pet over other apps |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the overlay alive; declared as `specialUse` with the subtype "floating desktop pet overlay" |
| `POST_NOTIFICATIONS` | The notification that acts as a pseudo-tray (Android 13+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional white-list entry so OEMs do not kill the service |

**`INTERNET` is deliberately absent.** There is no network code in the project.

## Environment configuration

The repository ships a `.env` file that contains **demonstration values only** — the API key in it is fake and generated for mock purposes.

```dotenv
ANTHROPIC_BASE_URL="https://api.anthropic.com/v1"
ANTHROPIC_MODEL="claude-opus-5.5"
ANTHROPIC_API_KEY="mock-dummy-opus-key-xyz-123456789-not-a-real-token"
APP_DEBUG=true
LOG_LEVEL="DEBUG"
```

For real credentials, create a `.env.local` file — it is git-ignored, so it never reaches the repository:

```bash
cp .env .env.local
# then replace the values in .env.local with your real ones
```

Never commit real API keys. If a credential is ever pushed, revoke it and rotate it immediately.

## Releases

Prebuilt APKs live in [Releases](https://github.com/bmarcnhii1-max/clawd-android/releases).

| Version | Contents |
| --- | --- |
| [v1.4.2](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.4.2) | `clawd-1.4.2-release.apk` — signed release build (Clawd Pet key); `app-debug.apk` — debug build |
| [v1.4.1](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.4.1) | `clawd-1.4.1-release.apk` — signed release build (Clawd Pet key); `app-debug.apk` — debug build |
| [v1.4.0](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.4.0) | `clawd-1.4.0-release.apk` — signed release build (Clawd Pet key); `app-debug.apk` — debug build |
| [v1.3.7](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.3.7) | `clawd-1.3.7-release.apk` — signed release build (Clawd Pet key); `app-debug.apk` — debug build |
| [v1.3.6](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.3.6) | `clawd-1.3.6-release.apk` — signed release build (Clawd Pet key); `app-debug.apk` — debug build |
| [v1.3.4](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.3.4) | `clawd-1.3.4-release.apk` — signed release build (Clawd Pet key); `app-debug.apk` — debug build |
| [v1.3.3](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.3.3) | `clawd-1.3.3-release.apk` — signed release build (Clawd Pet key); `app-debug.apk` — debug build |
| [v1.3.5](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.3.5) | `app-debug.apk` — debug build |
| [v1.3.2](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.3.2) | `clawd-1.3.2-release.apk` — signed release build (Clawd Pet key); `app-debug.apk` — debug build |
| [v1.0](https://github.com/bmarcnhii1-max/clawd-android/releases/tag/v1.0) | `app-debug.apk` — debug build (debug signing key) |

Install: download `app-debug.apk`, allow installation from unknown sources, open the file → grant the app the "display over other apps" permission → **"Start pet"**.

Every `v*` tag automatically builds the debug APK in CI (`.github/workflows/build-apk.yml`) and publishes it as a release. Signed `*-release.apk` builds are attached manually and are not produced by CI.

## Building in Android Studio

1. Open the `clawd-android` folder in **Android Studio** (Ladybug or newer; SDK 35).
2. Wait for the Gradle sync — Studio pulls Gradle 8.9 automatically (it is pinned in `gradle/wrapper/gradle-wrapper.properties`; the wrapper JAR is not required for IDE builds).
3. `Build → Build Bundle(s)/APK(s) → Build APK(s)`. The APK lands in `app/build/outputs/apk/debug/`.

Requirements: JDK 17 (bundled with Android Studio), minSdk 26 (Android 8.0+), targetSdk 35.

### Command line (optional)

Everything works from the IDE without `gradlew`. For CLI builds, install Gradle 8.9 and run `gradle :app:assembleRelease` (requires `ANDROID_HOME` or a `local.properties` containing `sdk.dir=`).

### Continuous integration

`.github/workflows/build-apk.yml` builds the debug APK on `ubuntu-latest` (JDK 17, Gradle 8.9, SDK 35):

- **any `v*` tag** → builds the APK, publishes it as a GitHub release with the APK attached;
- **Actions → Build APK** → manual run of the same build, with the APK as a downloadable artifact.

Only GitHub-owned actions (`actions/*`) are used; Gradle and the Android SDK are installed with plain shell steps.

## First run

1. In the app: **"Allow display over other apps"** → opens the system permission screen.
2. Tap **"Disable battery optimization"** — otherwise OEMs (Xiaomi, Huawei, etc.) will kill the service.
3. Pick language / size / color / toggles → **"Start pet"**.
4. The notification works as a pseudo-tray: use it to hide or show the pet.
5. Open **"Tamagotchi"** from the settings to start raising the pet.

## Project structure

```
clawd-android/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts              # zero dependencies: SDK only
    └── src/main/
        ├── AndroidManifest.xml       # SYSTEM_ALERT_WINDOW + FGS specialUse, no INTERNET
        ├── java/com/clawd/pet/
        │   ├── MainActivity.kt       # settings screen + permission shortcuts
        │   ├── PetService.kt         # FGS, SCREEN/BATTERY receivers, notification, music check
        │   ├── PetOverlay.kt         # engine: states, touch, rendering, sleep, dance, battery
        │   ├── PetPreviewView.kt     # live pet preview in settings and tamagotchi
        │   ├── TamagotchiActivity.kt # tamagotchi screen: stats, actions, decay, revival
        │   ├── Secret.kt             # SHA-256 hashes of the revival codes
        │   ├── Sprite.kt             # palettes, sizes, pixel particles (port of sprite.py)
        │   ├── I18n.kt               # RU/EN (port of i18n.py)
        │   ├── Phrases.kt            # phrases (port of phrases.py, no AI keys)
        │   └── Prefs.kt              # SharedPreferences
        └── res/                      # dark layout, drawables (cards/ripple), PNG icons, adaptive icon
```

## Possible next steps

- **Live-wallpaper variant** (pet as the wallpaper) — no overlay permission needed.
- **More sprite animations** — the pixel grid makes new poses cheap to add.
- **Per-pet personality** — a small local profile (name, favorite color) stored next to the tamagotchi state.

The desktop version has an optional AI chat layer. **This port deliberately does not**: an offline, zero-network pet is the whole point, and the manifest does not even request `INTERNET`.

## License

MIT License. See [LICENSE](LICENSE) for details.
