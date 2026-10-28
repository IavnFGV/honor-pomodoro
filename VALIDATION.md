# Validation — Honor DLI-TL20

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
