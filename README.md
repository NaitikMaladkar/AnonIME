# AnonIME — Phase 1

A privacy-first Android keyboard. **Nothing leaves the device.**

Phase 1 ships a minimal but functional QWERTY keyboard with a number row and
shift/caps lock, hosted in Jetpack Compose, themed to match the system, and
hardened against data exfiltration by construction.

---

## Privacy guarantees (Phase 1)

| Guarantee | How it is enforced |
| --- | --- |
| No telemetry | No analytics SDK, no crash-reporting SDK, no network code anywhere. |
| No personalized learning | No user dictionary, no frequency table, no n-gram store — typing never persists. |
| No input history | No transient buffers are written to disk; `onFinishInput` wipes in-memory state. |
| Incognito flag honored | `EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING` is explicitly read and respected. |
| Offline-only | `AndroidManifest.xml` declares **no `INTERNET` permission** and `usesCleartextTraffic=false`. Even if a future code path tried to call out, the OS would block it. |
| No backup | `allowBackup=false`, `dataExtractionRules` excludes everything, `backup_rules` is empty. |

---

## Project layout

```
AndroidIME/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml          # version catalog
│   └── wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml      # no INTERNET permission
        ├── java/com/anonime/
        │   ├── AnonIMEApplication.kt
        │   ├── MainActivity.kt       # onboarding: enable + set default
        │   ├── ime/
        │   │   ├── AnonIMEService.kt           # InputMethodService
        │   │   ├── KeyboardScreen.kt           # Compose UI
        │   │   ├── KeyClickModifier.kt         # silent-click Modifier
        │   │   ├── KeyDefinitions.kt           # QWERTY + number row
        │   │   └── KeyModel.kt                 # KeyAction / Key / ShiftState
        │   └── ui/theme/
        │       ├── Color.kt
        │       ├── Theme.kt           # Material3 + dynamic color
        │       └── Type.kt
        └── res/
            ├── values/{strings,colors,themes,bools}.xml
            ├── values-night/themes.xml
            ├── xml/{method,backup_rules,data_extraction_rules}.xml
            ├── drawable/ic_launcher_foreground.xml
            └── mipmap-anydpi-v26/ic_launcher{,_round}.xml
```

---

## Build

Requirements:
- JDK 17
- Android SDK Platform 35 + Build Tools 35.x
- Android Gradle Plugin 8.7+ (handled by the version catalog)
- Gradle 8.10+ (the wrapper pins it; you only need JDK on PATH)

### From Android Studio
1. **File → Open → `AndroidIME/`**
2. Let Gradle sync.
3. Pick a device or emulator running Android 10 (API 29) or newer.
4. **Run ▶** the `app` configuration.

### From the command line
```bash
cd AndroidIME
./gradlew :app:assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> The repo does not ship the Gradle wrapper jar (no `gradlew` script).
> If you don't have Gradle installed, run `gradle wrapper --gradle-version 8.10.2`
> once from inside `AndroidIME/` to generate it. Or open the folder in
> Android Studio, which will offer to do the same.

---

## Enable the keyboard (after install)

1. Open the **AnonIME** app from the launcher → tap **Open system settings** → toggle **AnonIME Keyboard** on.
2. Back in the app → tap **Choose keyboard** → pick **AnonIME**.
3. Tap any text field — the AnonIME keyboard appears.

The onboarding screen auto-refreshes its status badges each time you return
to it, so you can flip between Settings and AnonIME and watch the checkmarks update.

---

## Phase 1 feature checklist

- [x] Android app shell (Application, MainActivity, IME service) targeting API 29+
- [x] QWERTY letter layout — 3 rows
- [x] Number row — always visible above letters
- [x] Shift key with three states: Off → OnNext → Locked → Off
- [x] Backspace, Space, Enter (context-aware label/icon)
- [x] Jetpack Compose UI hosted inside `InputMethodService`
- [x] Material 3 theme with system auto dark/light + dynamic color (API 31+)
- [x] Silent keypresses — no ripple, no vibration, no sound (per spec)
- [x] No INTERNET permission, no telemetry, no persistence, no backup
- [x] Honors `IME_FLAG_NO_PERSONALIZED_LEARNING`
- [x] Onboarding Activity with enable + set-default flows

### Known Phase 1 limits (intentional)
- No symbols panel — `?123` button is a no-op stub for Phase 2.
- No autocomplete / suggestions — by design (anonymous typing).
- No multi-language subtypes beyond `en_US`.
- No long-press accents.
- No emoji panel.

---

## Roadmap

| Phase | Theme | Planned highlights |
| --- | --- | --- |
| **1** *(this)* | Foundation | QWERTY, number row, shift/caps, anonymous guarantees |
| 2 | Layouts | Symbols panel, long-press accents, emoji picker |
| 3 | UX | Optional haptic/sound settings, key popups, theme picker |
| 4 | Languages | Multi-locale subtypes, Dvorak / Colemak layouts |
| 5 | Accessibility | TalkBack key descriptions, larger-key variant, high-contrast theme |

---

## License

TBD — pick one before shipping outside a dev device.
