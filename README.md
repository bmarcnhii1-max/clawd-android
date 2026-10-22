<p align="center">
  <a href="#english">English</a> | <a href="#русский">Русский</a>
</p>

<a id="english"></a>
## English

# Clawd for Android

The same pixel pet as the Windows desktop version — but **without AI and without network**. The manifest does not even request the `INTERNET` permission, so the app physically cannot send anything anywhere.

A tiny pixel creature that lives on top of your Android screen: it walks around, sleeps, dreams, reacts to taps, can be petted, dragged, and flung across the display.

## Features

- **Lives on top of every app** — a system overlay window backed by a foreground service.
- **Only the pet itself is touchable.** The interactive window is sized exactly to the sprite body (≈220×151 px); the speech bubble and particles live in a separate window with `FLAG_NOT_TOUCHABLE`. Taps on YouTube, icons, and everything underneath go to that app, never to the pet.
- **Tap** — the pet hops and replies; a quick second tap is a double jump with sparks; 4 quick taps in a row makes it offended.
- **Long press** — you are petting it: hearts appear.
- **8 taps in a row** — the pet gets fed up, **dissolves into pixels right where it stands** (it does not fly away), hides for 20 seconds, then reassembles itself out of pixels.
- **Drag** — move the pet with your finger; a fast swipe flings it across the whole screen, bouncing off walls and floor.
- **Sleep** — after N minutes without attention (configurable: 1 / 3 / 10 minutes, or never) it falls asleep and **evaporates into pixels**. While asleep it emits floating "Z"s and shows dream thoughts ("I dream of a sea of pixels…"). A tap reassembles it.
- **Pixel sprite 1:1 with the desktop version** (the same `sprite.py`): paws, arms, eyes, blush, squash-and-stretch.
- **2 languages** for phrases and the whole UI: RU / EN.
- **3 sizes and 6 colors** (Claude, Mint, Lavender, Sky, Pink, Graphite) with a live animated preview in the settings.
- **Toggles** — "Wanders around" (off by default, saves battery), "Talks", "Gravity".
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

## Modules

| File | Purpose |
| --- | --- |
| `MainActivity.kt` | Settings screen: language, size, color, sleep timer, toggles, permission shortcuts, live preview, start / stop / hide / reset |
| `PetService.kt` | Foreground service, notification (acts as a pseudo-tray), `SCREEN_ON`/`SCREEN_OFF` and `BATTERY_LOW`/`BATTERY_OKAY` receivers, 1.5 s screen-state watchdog |
| `PetOverlay.kt` | The engine: state machine, touch handling, physics, canvas rendering, particles, sleep, dissolve, battery awareness |
| `PetPreviewView.kt` | Animated pet preview inside the settings screen |
| `Sprite.kt` | Palettes, sizes, and pixel patterns for particles (port of `sprite.py`) |
| `I18n.kt` | RU / EN UI strings (port of `i18n.py`) |
| `Phrases.kt` | Pet dialogue — greetings, idle chatter, dreams, reactions (port of `phrases.py`, no AI keys) |
| `Prefs.kt` | `SharedPreferences` wrapper for all settings and the saved position |

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

## Battery

- **No polling** — everything is driven by touches and system broadcasts.
- Animation via `Handler.postDelayed`: 40 ms in active states, 250 ms idle, 1200 ms asleep. Never a 60 FPS loop.
- **Screen off** → the pet **stays alive but goes quiet**: it does not wander or animate, and the tick rate drops (visible only in a mirror, e.g. `scrcpy --turn-screen-off`). When the screen comes back, the pet is instantly in place. Screen state is checked directly, not only via broadcast, so Android lag never leaves the pet hidden.
- Battery below 15% → the pet "falls asleep" until the device is charged.
- Zero network, GPS, sensors, or wake locks.
- A settings button asks to exclude the app from battery optimization — that is a one-time white-list, not a permanent drain.

## Building in Android Studio

1. Open the `clawd-android` folder in **Android Studio** (Ladybug or newer; SDK 35).
2. Wait for the Gradle sync — Studio pulls Gradle 8.9 automatically (it is pinned in `gradle/wrapper/gradle-wrapper.properties`; the wrapper JAR is not required for IDE builds).
3. `Build → Build Bundle(s)/APK(s) → Build APK(s)`. The APK lands in `app/build/outputs/apk/debug/`.

Requirements: JDK 17 (bundled with Android Studio), minSdk 26 (Android 8.0+), targetSdk 35.

### Command line (optional)

Everything works from the IDE without `gradlew`. For CLI builds, install Gradle 8.9 and run `gradle :app:assembleRelease` (requires `ANDROID_HOME` or a `local.properties` containing `sdk.dir=`).

## First run

1. In the app: **"Allow display over other apps"** → opens the system permission screen.
2. Tap **"Disable battery optimization"** — otherwise OEMs (Xiaomi, Huawei, etc.) will kill the service.
3. Pick language / size / color / toggles → **"Start pet"**.
4. The notification works as a pseudo-tray: use it to hide or show the pet.

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
        │   ├── PetService.kt         # FGS, SCREEN/BATTERY receivers, notification
        │   ├── PetOverlay.kt         # engine: states, touch, rendering, sleep, battery
        │   ├── PetPreviewView.kt     # live pet preview in settings
        │   ├── Sprite.kt             # palettes, sizes, pixel particles (port of sprite.py)
        │   ├── I18n.kt               # RU/EN (port of i18n.py)
        │   ├── Phrases.kt            # phrases (port of phrases.py, no AI keys)
        │   └── Prefs.kt              # SharedPreferences
        └── res/                      # dark layout, drawables (cards/ripple), PNG icons, adaptive icon
```

## Possible next steps

- **AI layer on top**: a chat overlay plus an `ai_client` in Kotlin — that would add the `INTERNET` permission, and could reuse the same pattern as the desktop version.
- **Live wallpaper variant** (pet as the wallpaper) — no overlay permission needed.

---

<a id="русский"></a>
## Русский

# Clawd для Android

Тот же пиксельный питомец, что и на Windows-десктопе — но **без ИИ и без сети**: манифест даже не запрашивает `INTERNET`, приложение физически не может ничего отправить.

Крошечное пиксельное существо, которое живёт поверх экрана Android: гуляет, спит, видит сны, реагирует на тапы. Его можно гладить, перетаскивать и швырять через весь экран.

## Возможности

- **Живёт поверх всех окон** — системный оверлей и foreground-сервис.
- **Тапится только сам питомец**: интерактивное окно размером ровно с тело спрайта (≈220×151 px), пузырь и частицы живут в отдельном окне с `FLAG_NOT_TOUCHABLE` — поэтому тапы по YouTube, иконкам и любому другому приложению уходят приложению, а не питомцу.
- **Тап** — подпрыгивает и реплика; быстрый второй тап — двойной прыжок с искрами; 4 быстрых тапа подряд — обижается.
- **Долгий тап** — гладишь: сердечки.
- **8 тапов подряд** — питомец психует, **распыляется на пиксели прямо на месте** (никуда не улетает) и прячется на 20 секунд, потом собирается обратно из пикселей.
- **Перетаскивание пальцем**; резкий свайп — швыряешь, летит через весь экран (с отскоками от стен и пола, как на ПК).
- **Спит через N минут без внимания** (настраивается: 1 / 3 / 10 минут или никогда) и **испаряется на пиксели**; во сне летит «Z», видит сны («Мне снится море из пикселей…»). Тап — собирается обратно.
- **Пиксельный спрайт 1:1 с десктопом** (тот же `sprite.py`): лапы, руки, глаза, румянец, «сплюснутость».
- **2 языка** фраз и всего интерфейса: RU / EN.
- **3 размера и 6 окрасов** (Claude, мятный, лавандовый, небесный, розовый, графит) — с живым превью в настройках.
- **Флаги**: «гуляет сам» (по умолчанию **выкл** — так меньше батареи), «разговаривает», «гравитация».
- **Позиция запоминается**; питомец не залезает на док лаунчера и панель навигации. Кнопка «Сбросить позицию» вернёт его в угол.
- **Работает и при погашенном дисплее** (зеркалирование через scrcpy): питомец виден в окне зеркала, но не тратит батарею на блуждание.
- **Кнопка «Спрятать»** убирает питомца с экрана, не останавливая сервис; вернуть можно из уведомления.
- **Тёмный интерфейс и тёмные реплики**; своя пиксельная иконка и иконка уведомления.
- **Анимации 1:1 с десктопом**: наклон корпуса в полёте и при перетаскивании, разжимание на прыжке, пульс при поглаживании, 7 Гц шаги, 5 Гц махание рукой.
- **Питомец физически не может уйти за пределы экрана**: высота прыжка, полёт, перетаскивание и «побег» ограничены рамками окна.

## Архитектура

Приложение — это foreground-сервис плюс два оверлейных окна:

| Слой | Окно | Флаги | Назначение |
| --- | --- | --- | --- |
| Визуальный | `view` | `NOT_TOUCHABLE`, `NOT_FOCUSABLE`, `NOT_TOUCH_MODAL`, alpha 0.8 | Речевой пузырь, сны, частицы (сердечки, искры, «Z») |
| Интерактивный | `hitView` | `NOT_FOCUSABLE`, `NOT_TOUCH_MODAL` | Тело спрайта, принимает касания |

Оба окна — `TYPE_APPLICATION_OVERLAY` с `FLAG_LAYOUT_NO_LIMITS` и `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`, поэтому питомца можно ставить в любую точку экрана, включая зону выреза.

**Интерактивное окно пересчитывается каждый кадр** под фактическую нарисованную геометрию питомца с учётом наклона корпуса. Используются два прямоугольных следа в сетке 18×13:

- тело с лапами: клетки 1..16 × 2..11;
- спящая кучка: клетки 4..13 × 8..10.

Именно это делает пустые зоны оверлея прозрачными для касаний — питомец никогда не перехватывает тапы у приложения под ним.

### Машина состояний

В `PetOverlay` работает небольшая машина состояний: `idle`, `walk`, `sleep`, `fly`, `drag`, `pet`, `flee`. Переходы вызываются жестами, таймерами и системными broadcast-ами; у каждого состояния своя частота тиков.

### Сон и распад

Сон — это не спрятанный спрайт: тело **распадается по клеткам**. Детерминированный хеш ячейки задаёт порядок испарения пикселей, поэтому эффект каждый раз одинаковый. Тот же хеш управляет и обратной сборкой, когда питомец просыпается.

## Модули

| Файл | Назначение |
| --- | --- |
| `MainActivity.kt` | Экран настроек: язык, размер, окрас, время засыпания, флаги, кнопки разрешений, живое превью, старт / стоп / спрятать / сброс позиции |
| `PetService.kt` | Foreground-сервис, уведомление (работает как псевдо-трей), ресиверы `SCREEN_ON`/`SCREEN_OFF` и `BATTERY_LOW`/`BATTERY_OKAY`, сторож состояния экрана раз в 1,5 с |
| `PetOverlay.kt` | Движок: машина состояний, обработка касаний, физика, отрисовка на Canvas, частицы, сон, распад, учёт батареи |
| `PetPreviewView.kt` | Анимированное превью питомца в настройках |
| `Sprite.kt` | Палитры, размеры и пиксельные шаблоны частиц (порт из `sprite.py`) |
| `I18n.kt` | RU / EN строки интерфейса (порт из `i18n.py`) |
| `Phrases.kt` | Фразы питомца — приветствия, болтовня, сны, реакции (порт из `phrases.py`, без ИИ-ключей) |
| `Prefs.kt` | Обёртка над `SharedPreferences` для всех настроек и сохранённой позиции |

## Стек технологий

| Компонент | Версия |
| --- | --- |
| Язык | Kotlin 2.0.20 |
| Android Gradle Plugin | 8.6.1 |
| Gradle | 8.9 |
| JDK | 17 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| UI | Android Views + XML-разметка, без Compose |
| Отрисовка | Рисование на `Canvas`, без игровых движков и спрайт-листов |
| Зависимости | **Ноль** сторонних библиотек — только Android SDK |

Приложение состоит из одного модуля `:app` с пустым блоком `dependencies { }`. Всё рисуется вручную через `Canvas.drawRect` по пиксельной сетке.

## Разрешения

| Разрешение | Зачем |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Рисовать питомца поверх других приложений |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Держать оверлей живым; объявлен как `specialUse` с подтипом «floating desktop pet overlay» |
| `POST_NOTIFICATIONS` | Уведомление-аналог трея (Android 13+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Необязательная запись в белый список, чтобы OEM не убивали сервис |

**`INTERNET` намеренно отсутствует.** Сетевого кода в проекте нет.

## Конфигурация окружения

В репозитории лежит файл `.env` с **только демонстрационными значениями** — API-ключ в нём фейковый и создан для показа.

```dotenv
ANTHROPIC_BASE_URL="https://api.anthropic.com/v1"
ANTHROPIC_MODEL="claude-opus-5.5"
ANTHROPIC_API_KEY="mock-dummy-opus-key-xyz-123456789-not-a-real-token"
APP_DEBUG=true
LOG_LEVEL="DEBUG"
```

Для настоящих учётных данных создайте файл `.env.local` — он в игноре Git, поэтому в репозиторий не попадёт:

```bash
cp .env .env.local
# затем подставьте реальные значения в .env.local
```

Реальные API-ключи никогда не коммитьте. Если ключ всё же ушёл в репозиторий — сразу отзовите его и выпустите новый.

## Батарея

- **Ни одного poll-а**: всё на тапах и системных broadcast-ах.
- Анимация — `Handler.postDelayed`: активные состояния 40 мс, покой 250 мс, сон 1200 мс. Никогда не 60 FPS вокруг часов.
- Экран погас → питомец **остаётся живым, но не шумит**: не бродит и не анимируется, тик замедляется (видно только в зеркале, например при `scrcpy --turn-screen-off`). Когда экран включился — питомец сразу на своём месте. Состояние экрана проверяется напрямую (а не только по broadcast), поэтому лаг Android не оставляет питомца спрятанным.
- Заряд < 15% → питомец «уснул» до подзарядки.
- Ноль сети, GPS, сенсоров, wakelock-ов.
- Кнопка в настройках просит исключить приложение из оптимизации батареи — это разовая запись в белый список, а не постоянный расход.

## Сборка в Android Studio

1. Открыть папку `clawd-android` в **Android Studio** (Ladybug или новее; SDK 35).
2. Дождаться Gradle Sync — Studio сама подтянет Gradle 8.9 (он указан в `gradle/wrapper/gradle-wrapper.properties`; для сборки из IDE jar-файл wrapper не нужен).
3. `Build → Build Bundle(s)/APK(s) → Build APK(s)` → apk в `app/build/outputs/apk/debug/`.

Требования: JDK 17 (доступен в Android Studio из коробки), minSdk 26 (Android 8.0+), targetSdk 35.

### Сборка из командной строки (опционально)

Внутри студии всё работает без `gradlew`. Если хочется CLI — установи Gradle 8.9 и запусти `gradle :app:assembleRelease` (нужен `ANDROID_HOME` или `local.properties` с `sdk.dir=`).

## Первый запуск

1. В приложении: **«Разрешить поверх других окон»** → системный экран разрешений.
2. Кнопка **«Отключить оптимизацию батареи»** — иначе OEM (Xiaomi, Huawei и пр.) убьёт сервис.
3. Язык / размер / окрас / флаги → **«Запустить питомца»**.
4. Уведомление работает как псевдо-трей: оттуда же «Спрятать питомца».

## Структура проекта

```
clawd-android/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts              # ноль зависимостей: только SDK
    └── src/main/
        ├── AndroidManifest.xml       # SYSTEM_ALERT_WINDOW + FGS specialUse, без INTERNET
        ├── java/com/clawd/pet/
        │   ├── MainActivity.kt       # экран настроек + кнопки разрешений
        │   ├── PetService.kt         # FGS, ресиверы SCREEN/BATTERY, уведомление
        │   ├── PetOverlay.kt         # движок: стейты, тач, отрисовка, сон, батарея
        │   ├── PetPreviewView.kt     # живое превью питомца в настройках
        │   ├── Sprite.kt             # палитры, размеры и пиксельные частицы (порт из sprite.py)
        │   ├── I18n.kt               # RU/EN (порт i18n.py)
        │   ├── Phrases.kt            # фразы (порт phrases.py, без ИИ-ключей)
        │   └── Prefs.kt              # SharedPreferences
        └── res/                      # тёмный layout, drawable (карточки/ripple), PNG-иконки, adaptive icon
```

## Дальше (опционально)

- **Модуль ИИ поверх**: чат-оверлей + `ai_client` на Kotlin — тогда добавится `INTERNET`; можно собрать тем же паттерном, что и десктоп.
- **Live-wallpaper вариант** (питомец как обои) — без разрешения на оверлей.
