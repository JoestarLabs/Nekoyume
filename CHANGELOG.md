# Changelog

## [0.0.2](https://github.com/JoestarLabs/Nekoyume/compare/v0.0.1...v0.0.2) (2026-10-17)


### Features

* Add app icon (foreground, background and adaptive monochrome) ([28f812d](https://github.com/JoestarLabs/Nekoyume/commit/28f812dedab17086f8da8cbea7a34c465d8fb952))
* Add app icon (foreground, background and adaptive monochrome) ([7b4bd72](https://github.com/JoestarLabs/Nekoyume/commit/7b4bd72e886eeb0480f3652b6e8155405764a9e3))
* add feature specification and requirements checklist for audio loop engine ([78fe2ee](https://github.com/JoestarLabs/Nekoyume/commit/78fe2ee55b9132dc2057e9d3b54741b171d48a80))
* **audio:** implement background playback service and Hilt dependency injection ([f1e2829](https://github.com/JoestarLabs/Nekoyume/commit/f1e2829dc06f7e3421cea4232fa511c44b35ddc1))
* **audio:** implement domain models and asset sound catalog repository ([c6ee8ed](https://github.com/JoestarLabs/Nekoyume/commit/c6ee8edecb28683ffee016408a84944e040d224b))
* **audio:** implement idle timeout, task removal handling, and mix state restoration ([7eb319b](https://github.com/JoestarLabs/Nekoyume/commit/7eb319bc4d3fbd3ca23afc45918a07bc939124c9))
* **audio:** implement independent and master volume attenuation with 30ms anti-zipper ramp ([271735b](https://github.com/JoestarLabs/Nekoyume/commit/271735b5d7487afe59ff59ced40a58c46fe67813))
* **audio:** implement multi-stream looping playback engine with media3 player pool ([aea38de](https://github.com/JoestarLabs/Nekoyume/commit/aea38de604d6f3c20918646a38237f7c7dace539))
* **audio:** implement platform audio focus manager and ducking integration ([1d1a3cd](https://github.com/JoestarLabs/Nekoyume/commit/1d1a3cd9b3fb4135ed1c1eee110a218ebf624ea7))
* dynamically calculate Android versionCode and versionName from version.txt in build.gradle.kts ([a1a4441](https://github.com/JoestarLabs/Nekoyume/commit/a1a44414454c79840569c5ace70abf3dbe147282))
* run /speckit-plan ([14fc83d](https://github.com/JoestarLabs/Nekoyume/commit/14fc83d416d33f7a4eea8303570a2433e763008e))
* run /speckit-tasks ([ca99269](https://github.com/JoestarLabs/Nekoyume/commit/ca992696ab528d0219043193b80712936093da2e))
* **spec:** initialize spec kit and establish nekoyume constitution and agent guidelines ([66f30b1](https://github.com/JoestarLabs/Nekoyume/commit/66f30b10d55821a7b904f4a12f508198a1b652d4))
* **ui:** connect UI transport controls and notification permissions ([e151f19](https://github.com/JoestarLabs/Nekoyume/commit/e151f19b1fbcb6c5f4037b9c11509b2d12bb4d91))


### Bug Fixes

* **ci:** remove invalid pixel_9 device profile from emulator runner ([afef837](https://github.com/JoestarLabs/Nekoyume/commit/afef83763bb55ebfdeb85b4391a50ef3632c363b))
* **deps:** update androidx & compose ([6d628b3](https://github.com/JoestarLabs/Nekoyume/commit/6d628b3167081c548cd73d62977c6901b167c9ae))
* **deps:** update androidx & compose ([337c3c6](https://github.com/JoestarLabs/Nekoyume/commit/337c3c6f9260a365f6d9b9ddb205840bc46cab83))
* **deps:** update dependency androidx.compose:compose-bom to v2026 ([f467122](https://github.com/JoestarLabs/Nekoyume/commit/f46712230e608c9f11d41adc33f2b4de23fb88df))
* **deps:** update dependency androidx.compose:compose-bom to v2026 ([a0bac30](https://github.com/JoestarLabs/Nekoyume/commit/a0bac30a375702cada1eafbd7e2888d407c6f838))
* **deps:** update testing dependencies ([9d63f44](https://github.com/JoestarLabs/Nekoyume/commit/9d63f447e558c3791a06750110c5a89e810d9854))
* **deps:** update testing dependencies ([5f787e4](https://github.com/JoestarLabs/Nekoyume/commit/5f787e4db1164af53bf99d3eb6061dd404c9ef88))
* **test:** update instrumented tests for package prefix assertion and compose rule import ([6a8fac2](https://github.com/JoestarLabs/Nekoyume/commit/6a8fac2ad9dfee6548cabebe9e72f88b51b5cb76))


### Documentation

* **plan:** plan continuous background audio service and dependency injection ([7d26840](https://github.com/JoestarLabs/Nekoyume/commit/7d26840b20698dd43bb20af8054860ca5227be3e))
* **spec:** clarify background audio service lifecycle and edge cases ([e20ad2d](https://github.com/JoestarLabs/Nekoyume/commit/e20ad2d9e6e34142320726f79e421d20d07d1ef5))
* **spec:** specify continuous background audio service and dependency injection ([d657a43](https://github.com/JoestarLabs/Nekoyume/commit/d657a43ba5639364e93b0bd5c731bd4453bb7de6))
* **tasks:** define implementation tasks for continuous background audio service ([67b5631](https://github.com/JoestarLabs/Nekoyume/commit/67b5631820b35cb2a622f2d27a3c05ff53a536f3))
* **tasks:** refine MediaSession wiring and notification permission tasks ([57147d7](https://github.com/JoestarLabs/Nekoyume/commit/57147d709a757e6673a4563fdb85b61b9a7fa4ac))


### Miscellaneous Chores

* **audio:** resolve lint warnings, perform audit and complete verification gates ([714d33a](https://github.com/JoestarLabs/Nekoyume/commit/714d33a8cfdd1ca0c3834593900aaddc4c670b81))
* **build:** configure ProGuard keep rules and finalize verification gates ([a3009ad](https://github.com/JoestarLabs/Nekoyume/commit/a3009ad58cecce2870235cbaec9e487ac92f662c))

## [0.0.1](https://github.com/bl4ckswordsman/Nekoyume/compare/v0.0.1...v0.1.0) (2025-06-24)


### Features

* Add release-please workflow ([df41f3f](https://github.com/bl4ckswordsman/Nekoyume/commit/df41f3fb0af8b984bff4e3ab1f071aebdd40ffb8))
* Add VirusTotal scan workflow ([ca4d2de](https://github.com/bl4ckswordsman/Nekoyume/commit/ca4d2de0908f7abde18a765ff552e3c5ca2bcaaf))
* **ci:** Add VirusTotal scan as a separate job to release workflow ([f6e66ef](https://github.com/bl4ckswordsman/Nekoyume/commit/f6e66ef34d9c15bf61652e64f013ebd550b58338))
* **ci:** testing ([ebcfd67](https://github.com/bl4ckswordsman/Nekoyume/commit/ebcfd67a081d73b170cb066f852c0ab9b91f73a1))
* **ci:** testing ([6c19efe](https://github.com/bl4ckswordsman/Nekoyume/commit/6c19efe241034e897f6d3cb090d009299b33365b))
* **workflow:** test workflow ([17691e4](https://github.com/bl4ckswordsman/Nekoyume/commit/17691e4b8f34dedef1ee524875e173670699fbb0))


### Bug Fixes

* Add VirusTotal scan to release build workflow ([88522db](https://github.com/bl4ckswordsman/Nekoyume/commit/88522db1d1b39288e40191721c43862272ec366c))
* **ci:** Move VirusTotal scan job into release-please workflow ([49f4150](https://github.com/bl4ckswordsman/Nekoyume/commit/49f41504a80968701d362c892fce14147a1915da))
* **ci:** Move VirusTotal scan to separate workflow file ([10ee6b9](https://github.com/bl4ckswordsman/Nekoyume/commit/10ee6b9aae44821bcde23b3214271888e968f334))
* **ci:** Update virustotal job to depend on build ([07d5019](https://github.com/bl4ckswordsman/Nekoyume/commit/07d5019ed1abf8149a9adf0500784077b64e44f5))
* **ci:** Use PAT token for release workflow ([8ed14be](https://github.com/bl4ckswordsman/Nekoyume/commit/8ed14be7e4880db2cc4d3aa900091027e5c85c6f))
* Update release workflow branch to master ([a7ca80b](https://github.com/bl4ckswordsman/Nekoyume/commit/a7ca80ba22146119a0db63f2686842db9e9c7cac))

## [0.0.1](https://github.com/bl4ckswordsman/Nekoyume/compare/v0.0.1...v0.1.0) (2025-06-24)


### Features

* Add release-please workflow ([df41f3f](https://github.com/bl4ckswordsman/Nekoyume/commit/df41f3fb0af8b984bff4e3ab1f071aebdd40ffb8))
* Add VirusTotal scan workflow ([ca4d2de](https://github.com/bl4ckswordsman/Nekoyume/commit/ca4d2de0908f7abde18a765ff552e3c5ca2bcaaf))
* **ci:** Add VirusTotal scan as a separate job to release workflow ([f6e66ef](https://github.com/bl4ckswordsman/Nekoyume/commit/f6e66ef34d9c15bf61652e64f013ebd550b58338))
* **ci:** testing ([ebcfd67](https://github.com/bl4ckswordsman/Nekoyume/commit/ebcfd67a081d73b170cb066f852c0ab9b91f73a1))
* **ci:** testing ([6c19efe](https://github.com/bl4ckswordsman/Nekoyume/commit/6c19efe241034e897f6d3cb090d009299b33365b))
* **workflow:** test workflow ([17691e4](https://github.com/bl4ckswordsman/Nekoyume/commit/17691e4b8f34dedef1ee524875e173670699fbb0))


### Bug Fixes

* Add VirusTotal scan to release build workflow ([88522db](https://github.com/bl4ckswordsman/Nekoyume/commit/88522db1d1b39288e40191721c43862272ec366c))
* **ci:** Update virustotal job to depend on build ([07d5019](https://github.com/bl4ckswordsman/Nekoyume/commit/07d5019ed1abf8149a9adf0500784077b64e44f5))
* **ci:** Use PAT token for release workflow ([8ed14be](https://github.com/bl4ckswordsman/Nekoyume/commit/8ed14be7e4880db2cc4d3aa900091027e5c85c6f))
* Update release workflow branch to master ([a7ca80b](https://github.com/bl4ckswordsman/Nekoyume/commit/a7ca80ba22146119a0db63f2686842db9e9c7cac))

## [0.0.1](https://github.com/bl4ckswordsman/Nekoyume/compare/v0.0.1...v0.1.0) (2025-06-24)


### Features

* Add release-please workflow ([df41f3f](https://github.com/bl4ckswordsman/Nekoyume/commit/df41f3fb0af8b984bff4e3ab1f071aebdd40ffb8))
* Add VirusTotal scan workflow ([ca4d2de](https://github.com/bl4ckswordsman/Nekoyume/commit/ca4d2de0908f7abde18a765ff552e3c5ca2bcaaf))
* **ci:** Add VirusTotal scan as a separate job to release workflow ([f6e66ef](https://github.com/bl4ckswordsman/Nekoyume/commit/f6e66ef34d9c15bf61652e64f013ebd550b58338))
* **ci:** testing ([6c19efe](https://github.com/bl4ckswordsman/Nekoyume/commit/6c19efe241034e897f6d3cb090d009299b33365b))
* **workflow:** test workflow ([17691e4](https://github.com/bl4ckswordsman/Nekoyume/commit/17691e4b8f34dedef1ee524875e173670699fbb0))


### Bug Fixes

* Add VirusTotal scan to release build workflow ([88522db](https://github.com/bl4ckswordsman/Nekoyume/commit/88522db1d1b39288e40191721c43862272ec366c))
* **ci:** Update virustotal job to depend on build ([07d5019](https://github.com/bl4ckswordsman/Nekoyume/commit/07d5019ed1abf8149a9adf0500784077b64e44f5))
* **ci:** Use PAT token for release workflow ([8ed14be](https://github.com/bl4ckswordsman/Nekoyume/commit/8ed14be7e4880db2cc4d3aa900091027e5c85c6f))
* Update release workflow branch to master ([a7ca80b](https://github.com/bl4ckswordsman/Nekoyume/commit/a7ca80ba22146119a0db63f2686842db9e9c7cac))

## 0.0.1 (2025-06-24) (testing)


### Features

* **ci:** testing ([6c19efe](https://github.com/bl4ckswordsman/Nekoyume/commit/6c19efe241034e897f6d3cb090d009299b33365b))


### Bug Fixes

* **ci:** Use PAT token for release workflow ([8ed14be](https://github.com/bl4ckswordsman/Nekoyume/commit/8ed14be7e4880db2cc4d3aa900091027e5c85c6f))

## 0.0.1 (2025-06-24)


### Features

* Add release-please workflow ([df41f3f](https://github.com/bl4ckswordsman/Nekoyume/commit/df41f3fb0af8b984bff4e3ab1f071aebdd40ffb8))
* Add VirusTotal scan workflow ([ca4d2de](https://github.com/bl4ckswordsman/Nekoyume/commit/ca4d2de0908f7abde18a765ff552e3c5ca2bcaaf))
* **workflow:** test workflow ([17691e4](https://github.com/bl4ckswordsman/Nekoyume/commit/17691e4b8f34dedef1ee524875e173670699fbb0))


### Bug Fixes

* Add VirusTotal scan to release build workflow ([88522db](https://github.com/bl4ckswordsman/Nekoyume/commit/88522db1d1b39288e40191721c43862272ec366c))
* Update release workflow branch to master ([a7ca80b](https://github.com/bl4ckswordsman/Nekoyume/commit/a7ca80ba22146119a0db63f2686842db9e9c7cac))

## [0.0.0] - 2025-06-22

### Added
- Initial release of Nekoyume Android application
- Basic Android Kotlin project structure with Compose UI
- Core application prototype
