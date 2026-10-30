# Validation — Honor DLI-TL20

## 0.5.0 spoken remaining time — 2026-10-29

Added Russian Android TextToSpeech replies for remaining minutes/seconds and paused state. Voice discovery runs off the service main thread; network-only and not-installed voices are excluded. Added a Settings test action. Microphone frames are drained but not recognized during speech and for 600 ms afterward; a bounded gate prevents indefinite muting after an engine failure. Added `сделай интервал` to both duration parsing and recognition grammar.

All JVM suites pass, including Russian spoken-time plural/rounding cases and the new duration alias. APK signature checked. Initial discovery found Google TTS but all Russian local voices marked notInstalled. Downloaded the native Russian voice pack (~29 MB) and restarted the app; local Russian voices then reported missing=false and network=false. Selected ru-ru-x-dfc#female_3-local; network-only voices were skipped.

The Settings speech test with microphone listening enabled produced SAY “Осталось 25 минут. Таймер на паузе.” at 11:32:33, START, and DONE at 11:32:37. Voice-worker logs showed SPEECH_GATE on during playback and off about 600 ms after completion; no self-triggered command appeared. There were no new am_anr/am_crash events. Full live wake-phrase-to-query and the new `сделай` alias have not yet been independently exercised after this update. A stalled speech request is stopped before the safety gate is released.

## 0.4.2 reset confirmation — 2026-10-29

The user showed system bars appearing with the old Android AlertDialog on reset. Replaced reset and running-stage-switch confirmations with the same rounded dark palette and FullscreenDialog behavior used elsewhere. Installed on Honor; reset dialog visually inspected without either system bar. Cancel returned to the unchanged 25:00 paused timer; no reset was confirmed during this UI check. The reset dialog was reopened for the user to inspect. APK built and signature verified.

## 0.4.0 presets and ANR mitigation — 2026-10-29

Added three immutable built-ins (15/5/15 every 4, 25/5/15 every 4, 50/10/20 every 2), local custom-preset creation/edit/copy/delete, and explicit application. Applied configurations are snapshots: saving a definition does not reset the current timer. Applying starts a fresh cycle but preserves daily/lifetime totals. Cycle progress is separate from total completed work; old installations migrate their previous completed count into cycle progress. One-off voice durations remain independent.

JVM preset tests pass for two- and three-work cycles, applying while running, clearing a one-off duration, preserving completed totals, and invalid names/cycle lengths. Existing timer, transition, duration-parser and delayed-display tests pass.

The user reported an ANR in installed 0.3.2. Captured `/data/anr/traces.txt` showed the main thread blocked in `ThreadedRenderer.nCreateProxy` → `Dialog.show` → `TransitionDialog.show` → `UiUpdates.send`; the audio thread was reading the microphone normally and the Java heap was small. The event log recorded `executing service local.ivand.pomodoro/.PomodoroService`. Hardware acceleration is now disabled at Application level and the custom canvas no longer forces a hardware layer. This removes the observed renderer creation path; it does not prove that every possible ANR has been eliminated. The frozen process was stopped without clearing app data.

Device inspection confirmed the preset browser and custom editor fit the landscape display. The name dialog was reduced after the keyboard covered its buttons; the corrected input and Done button both fit above the keyboard. Created QA-20261029 with 1/1/1 minute intervals and a two-work cycle, saved/applied it, and verified it remained after force-stop/relaunch. Editing its saved work duration to two minutes left the active one-minute timer unchanged until application. Deletion required two clicks and removed only the test definition. Applying Classic restored 25/5/15, a fresh four-work cycle, and kept the daily total.

A real one-minute work → ten-second decision → automatic one-minute rest → work decision sequence completed with voice listening enabled. Both completion windows opened and were visually inspected. No new am_anr/am_crash event appeared; the event log retained only the earlier 0.3.2 ANR. Voice inference in this short software-rendering run averaged about 28 ms per 80 ms frame, with an observed peak of 137 ms. This is a short regression check, not proof of long-term stability.

0.4.1 pins custom-preset Edit/Delete buttons outside the scrolling details, so deletion is always visible. Built-ins remain protected and offer copying instead. Deleting a definition does not interrupt an applied timer snapshot. The pinned Delete button and confirmation were exercised with an additional temporary copy, which was removed after testing. Preset and Settings dialogs now reapply immersive mode after focus returns from child windows. The final custom-card screenshot has no system bars. Final device state is Classic 25/5/15, four-work cycle, ready but not running, with voice enabled.

## 0.3.1 active interval editing — 2026-10-29

The user selected the semantics “15 minutes from the command,” rather than total duration including elapsed work. Added addressed duration grammar during normal listening and duration commands after a wake phrase. A running stage receives a new one-off duration and monotonic deadline from now, retaining its work/rest identity and completion count. A paused stage stays paused; regular preset durations remain unchanged. Pending next-stage edits still wait for an explicit start.

JVM regression checks pass for a command five minutes into work: fifteen minutes remain, the stage is counted once at the new deadline, and the following break restores its normal preset. Paused edits do not start a timer; invalid edits do not move its deadline. All parser, transition, and delayed-display tests pass; APK signature checked. Live active-interval speech acceptance remains to be checked after installation.

## 0.3.0 next-stage editor — 2026-10-29

Added one-off duration selection, minute steppers and stage-specific quick presets, direct voice duration commands, and independent saved auto-start preferences (work off, rest on). Editing cancels the current countdown permanently; only Start/Yes launches the edited stage. Future completed stages use the configured auto-start policy again. One-off minutes do not modify saved work/short/long durations.

JVM checks passed for all Russian minute values 1–120 including feminine forms, invalid/negative/seconds rejection; work and rest auto-start combinations; edit cancellation beyond the original deadline; one-off duration expiry and normal preset restoration; rest maximum validation. APK signature verified. On Honor the two checkboxes had their expected defaults; toggling them and reopening Settings preserved both new values, after which defaults were restored.

The next-stage panel was visually inspected, including the long-rest default after the fourth work. Choosing the ten-minute preset left the panel waiting after the original countdown deadline, with no timer alarm scheduled. Live voice events recorded the full requests to change to ten minutes and to three minutes. Settings remained at their normal 1/5/15 test values while the selected stage used its separate override. Microphone grammar changes completed without a runtime error.

During testing the user reported seconds appearing to freeze then catch up. The old UI rebased relative remaining time to broadcast receipt, which could distort the display if Android queued snapshots. Replaced OS broadcasts with synchronous same-process main-thread updates and based both timer and decision countdowns on absolute elapsedRealtime deadlines. Added a regression test delivering stale snapshots at simulated later times: the display must show real remaining time, not restart a relative countdown. Pause and expiry tests also pass. Updated APK installed; sustained smoothness under all EMUI states is not yet characterized.

## 0.2.0 interval handoff — 2026-10-29

Added five final work-second warning tones, a ten-second work-to-rest decision, automatic rest on silence, and an indefinite rest-to-work prompt requiring an explicit start. Voice replies are direct only in the corresponding prompt state and only if microphone listening is enabled. Manual pause/reset/selection cancels a pending auto-start; pending decisions are not restored after process death.

JVM transition tests pass: exact timeout boundary, early Yes, No cancelling timeout, manual pause, no automatic work even after a very long wait, long-rest duration, and fresh-state recovery. Warning and transition processing acquire a bounded CPU wake lock so the short countdown can run with the screen off.

Physical one-minute work test with the display asleep: ENDING events 5, 4, 3, 2, 1 were logged, the display woke to the completion dialog above keyguard, and AUTO_REST started ten seconds after completion with no answer. A one-minute rest test then woke to the work prompt; it was still waiting more than thirty seconds later with no timer alarm scheduled. Both dialog layouts were inspected in screenshots. Live spoken Yes/No acceptance has not yet been confirmed; its grammar is enabled only in the rest decision window. Original five-minute short-rest setting was temporarily reduced to one minute for this check.

The first completion-dialog build revealed EMUI status/navigation bars on wake. The follow-up build adds an explicit FULLSCREEN flag and reapplies immersive mode after dialog focus. Repeated physical rest-completion test confirmed a prompt without either system bar; dumpsys reported keyguard showing=true, occluded=true, and mForceStatusBar=false. Lock-screen settings have not been changed. The short rest is restored to its previous five minutes after testing; work remains at the existing one-minute test setting.

## 0.1.1 lock-screen update — 2026-10-29

Physical follow-up: installed on Honor; screen-off then normal wake showed the fullscreen timer while keyguard remained showing/occluded. An explicit WAKE_SCREEN Activity intent alone did not wake an already paused Activity. Added a 1500 ms SCREEN_BRIGHT_WAKE_LOCK with ACQUIRE_CAUSES_WAKEUP in the service before foregrounding; end-to-end voice wake needs a live check after this fix.

Added API-24-compatible SHOW_WHEN_LOCKED, transient TURN_SCREEN_ON, and foregrounding from both wake-only and addressed-command voice paths. No keyguard dismissal, overlay permission, or lock-setting changes. Build and timer tests checked locally. No USB ADB device was connected during this update, so physical lock-screen and wake behavior remain to be tested on Honor. An enabled voice service is required for voice wake; it is not automatically enabled after reboot or APK replacement.

Checks performed on 2026-10-28, Europe/Kyiv. Android 7.0/API 24, ARMv7, 2 GB RAM. This is a short acceptance check, not a reliability study.

## Verified

- APK signature verification, installation, and launch on the physical Honor.
- Landscape 1280×720 UI with status/navigation bars hidden.
- Start, pause, resume, and remaining-time display.
- A one-minute work test completed with the screen off, incremented the count once, and selected a five-minute break without starting it.
- JVM tests: pause/resume, deadline boundary, exactly-once completion, reset, and long break after four work sessions.
- Live Vosk recognition of `помидорка`, with start/stop command events. Pause/resume were also observed during the earlier voice prototype.
- Restricted Vosk grammar processing averaged about 29 ms per 80 ms audio frame, with an observed maximum of 170 ms. Zero audio loss has not been proven.
- App PSS was approximately 178 MiB in one snapshot; battery temperature was 30.2 °C. These are not long-term bounds.
- Settings and voice help visually inspected. Layout fits without clipping; increment/decrement controls exercised and returned to the initial value without applying changes.

## LED

Android accepted a red light request (0xffff0000, 500 ms on / 1500 ms off). The user observed green while charging at 100%, and reported no useful change without the cable.

The initial silent notification was demoted by Android. A later patch adds an actual 80 ms vibration. Physical LED behavior after that patch remains unconfirmed.

## Remaining checks

- Controlled tests of every single-phrase command and reset-confirmation flow.
- False activations per hour, noise, and distance.
- Overnight power use, sustained temperature, audio buffering, and EMUI process management.
- Reboot, force-stop, and wall-clock changes during recovery.
- Other phones and Android versions.

Local logs are ignored. Screenshots contain a diagnostic session count and one-minute test setting. Source defaults remain 25 / 5 / 15 minutes.

0.3.2: updated in-app Voice Commands help to include running-interval and next-stage duration examples, remaining-from-command semantics, and minute limits. Previous help had omitted these commands despite their presence in recognition and service code. APK built, signed, and installed on Honor; help layout checked on device.

## 0.6.0 — tomato reset menu and removal of statistics (2026-10-30)

- Removed daily/lifetime counters from the engine, service state, UI updates, and screen. Upgrade removes the old `completed`, `date`, and `todayCount` preference keys; existing cycle progress is retained.
- Added a tomato reset target with a 48 dp hit area and a styled immersive two-option menu. One large Start/Pause button replaces the previous split controls.
- JVM tests cover resetting an overridden long rest without losing cycle progress, then resetting the full cycle to paused work with the saved duration and a fresh short-rest sequence. All six suites pass.
- APK 0.6.0 (version code 11) builds and its signature verifies. No authorized ADB device was connected during this change, so installation, on-device layout, and lock-screen interaction remain unverified for this version. README screenshots are explicitly marked as the previous version where applicable.

### Installed-device follow-up (2026-10-30)

- Version 0.6.0 / code 11 was successfully installed and launched on Honor. The user reports the new controls working.
- Read-only log review after installation: crash buffer empty; no app ANR/crash events in the retained event buffer. The recorded kill at installation was the expected package-update stop of the previous process. Log buffers are finite, so this does not establish an exhaustive lifetime history.
- Current app PID remained active with 315,500 captured 80 ms audio frames (about seven hours of audio). Vosk cumulative inference average 28.62 ms per 80 ms frame; reported maximum 208 ms. Occasional frames exceed the frame budget; no end-to-end audio-loss claim is made.
- Snapshot: total app PSS about 185 MiB; battery temperature 35.5 C, battery 100%, full status. These are point-in-time readings, not a thermal or battery endurance test.
- Accepted as the first usable desk-device version. The remaining controlled robustness checks above remain optional follow-up work.
