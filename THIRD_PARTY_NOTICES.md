# Third-party components

Dependencies are downloaded by `prepare-dependencies.ps1`, not committed as binaries. Their original licenses apply when building or distributing an APK.

| Component | Version | License | Upstream |
| --- | --- | --- | --- |
| Vosk Android | 0.3.75 | Apache-2.0 | https://github.com/alphacep/vosk-api |
| Small Russian model | vosk-model-small-ru-0.22 | Apache-2.0 | https://alphacephei.com/vosk/models |
| Java Native Access | 5.18.1 | Apache-2.0 or LGPL-2.1-or-later | https://github.com/java-native-access/jna |
| R8 / D8 (build tool) | 9.5.26 | BSD-3-Clause plus upstream dependency notices | https://r8.googlesource.com/r8/ |

Android SDK tools are installed separately under their own terms and are not included here.

The UI, tomato marks, and vector icon are original artwork. No Lottie assets, ONNX models, personal voice models, or recordings are included in the current source package.
