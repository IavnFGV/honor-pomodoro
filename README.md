# Honor Pomodoro

A fullscreen, offline Pomodoro timer that gives an old **Honor 6A** a new job as a dedicated desk companion.

Built and tested on **Honor DLI-TL20 · Android 7.0 · 2 GB RAM · ARMv7**. The interface and voice commands are intentionally **in Russian**; documentation is in English.

![Landscape timer](docs/images/timer.png)

*Device screenshot. The one-minute interval was used for testing; source defaults are 25 / 5 / 15 minutes.*

## Features

- Landscape fullscreen UI. Swipe from an edge to temporarily reveal system navigation.
- Large countdown and a smooth progress ring: red for work, green for breaks.
- Four tomato icons track completed work sessions in the current cycle.
- Start, pause, resume, reset confirmation, and manual work/break selection.
- Configurable intervals, with a long break after every four work sessions.
- Each new interval waits for you to start it; no automatic work/break chain.
- Daily completed-session count, optional screen-on mode, background timing, and completion sound.
- Offline Russian voice control activated by **«Помидорка»** (*pomidorka*, “little tomato”).
- Experimental notification LED controls.

![Settings](docs/images/settings.png)

## Voice commands

Enable voice using the bottom button or Settings, allow microphone access, and wait for **«Жду: Помидорка»**.

Say a complete phrase such as **«Помидорка, пауза»**, or say **«Помидорка»**, wait for the ready tone, and give a command.

| Russian command | Action |
| --- | --- |
| `начать работу` / `работа` | Select and start work |
| `старт` / `начать` / `запусти таймер` | Start or resume the selected interval |
| `пауза` | Pause |
| `продолжить` | Resume |
| `начать перерыв` / `перерыв` | Select and start a break |
| `сколько осталось` | Show remaining minutes on screen; no spoken response yet |
| `остановить таймер` / `стоп` | Pause and request reset confirmation |
| `подтверждаю` | Confirm a requested reset within 15 seconds |
| `отмена` | Cancel the request; the timer stays paused |

Confirm a reset with another addressed command: **«Помидорка, подтверждаю»**.

Voice uses **Vosk 0.3.75** and **vosk-model-small-ru-0.22**, restricted grammars, and a six-second command window. This is speech recognition with an addressed grammar, not a separately trained keyword detector. Only allowlisted complete commands execute. Misses and false activations are possible; single-phrase commands have not all been independently tested.

## Privacy and background behavior

The app has **no Internet permission**, accounts, analytics, or audio recording files. Bundled models are extracted into private app storage on first use.

Optional voice listening uses a foreground service, a visible notification, and a CPU wake lock. It consumes power; overnight use has not been measured. The microphone must be enabled manually after restart.

The running timer uses a monotonic clock and an exact wakeup alarm. After process recreation, the saved interval returns **paused**, estimating remaining time from a saved wall-clock deadline. There is no boot autostart or guaranteed unattended alarm after reboot/force-stop.

## Build on Windows

Requirements:

- PowerShell 7
- JDK 17, with `java`, `javac`, `jar`, and `keytool` on `PATH`
- Android SDK **platforms;android-34**, **build-tools;34.0.0**, and **platform-tools**

No Gradle installation or neighboring project is required.

```powershell
git clone https://github.com/IavnFGV/honor-pomodoro.git
cd honor-pomodoro

# Download pinned dependencies and verify their SHA-256 hashes.
.\prepare-dependencies.ps1

# Test the timer engine, build the APK, and verify its signature.
.\test.ps1
.\build.ps1
```

Output: **`artifacts/pomodoro-honor-debug.apk`**.

First-time setup needs Internet access; the app does not. Downloads stay in ignored `.tools/`. A local test signing key is generated in ignored `.local/`. Keep this key to update existing installations with future builds. This is a development-signed APK, not a Play Store release.

Default SDK location: `%LOCALAPPDATA%\Android\Sdk`. Override it with:

```powershell
.\build.ps1 -AndroidSdk 'C:\Android\Sdk'
```

### Install

Enable USB debugging, connect and unlock the phone, then approve the computer's debugging key.

```powershell
.\install.ps1
# Optional explicit adb path:
.\install.ps1 -Adb 'C:\Android\Sdk\platform-tools\adb.exe'
```

The installer checks for an authorized **DLI-TL20** and targets it explicitly. The APK targets **API 24** and includes only **armeabi-v7a** native libraries. Other phones and Android versions have not been validated.

## Source layout

```text
AndroidManifest.xml           Permissions and landscape activity
src/local/ivand/pomodoro/     Timer, UI, settings, service, speech pipeline
res/drawable/                 Original vector tomato icon
tests/                       JVM timer tests
prepare-dependencies.ps1      Verified dependency downloads
build.ps1                     javac, D8, aapt2, zipalign, apksigner
install.ps1                   Model-specific USB installation
VALIDATION.md                 Measurements and test gaps
```

## Known limitations

- **LED operation is experimental.** The phone stayed green while charging despite a red request. A later patch adds vibration to prevent Android 7's silent-notification demotion; its physical result still needs verification. Completion alerts do not depend on LED operation.
- Long-term battery drain, thermal behavior, EMUI background survival, and false activations per hour have not been characterized.
- No task manager, cloud sync, statistics export, or spoken time announcements.
- Some confirmation dialogs retain Android's standard styling.
- This is an early device-focused prototype. See [validation notes](VALIDATION.md).

## License

Project source and original artwork use the [MIT License](LICENSE). Dependencies retain their own licenses: see [third-party notices](THIRD_PARTY_NOTICES.md).

Thanks to [Vosk](https://github.com/alphacep/vosk-api) and its [Russian model](https://alphacephei.com/vosk/models). No personal voice recordings or personal models are included in this repository.
