# Changelog

All notable changes to the AirSignal project are documented in this file.

## [1.3] - 2026-09-07 (Dev Build)

### Added
* **Official LSB-First NEC Framing (`IrCodeDatabase.kt`)**: Implemented `necLsbToRawPattern(address, command)` for 32-bit LSB-first bit ordering required by Chinese CCT LED lamp microcontrollers (e.g. BAIERDI CCT floor lamp, SKU `4766740590_PH-31007347311`).
* **PWM Repeat Pulse Burst Generator (`IrCodeDatabase.kt`)**: Added `necLsbRepeatPattern(address, command, repeatCount)` to generate 110ms NEC repeat frame bursts, simulating continuous button holds for smooth dimming.
* **Macro Sequence Engine (`ControlActivity.kt`)**: Added `sendMacroSequence()` to resolve PWM state-machine deadlocks in CCT lamp controllers. Automatically executes `0x09 (Cool White Reset)` -> `0x07` -> `0x07` double-lock sequence when triggering Warm Yellow (`0x07`).
* **Auto-Scan Pause & Resume (`dialog_scanning_progress.xml` & `ControlActivity.kt`)**: Added a live `Pause / Resume` button to the scanning progress dialog, allowing developers to freeze the scan timer, inspect active HEX payloads, take notes, and resume without losing state.
* **Dynamic Universal Remote Canvas (`ControlActivity.kt`)**: Transformed `ControlActivity` into an interactive, customizable button list where every saved signal key features **Rename (✎)** and **Delete (🗑️)** actions.
* **JSON Remote Backup Vault (`MainActivity.kt`)**: Added **Export Vault to Clipboard** and **Import Vault from Clipboard** dialogs using Gson serialization, allowing users to backup, restore, and transfer all remote profiles seamlessly across devices.
* **Custom HEX Signal Generator (`MainActivity.kt` & `ControlActivity.kt`)**: Added **"+ Custom HEX Signal"** and **"+ Add Custom Code"** dialogs to create functional remote buttons directly from raw hex bytes or timing arrays.
* **BAIERDI Verified CCT Color & Dimmer Palette**: Successfully identified and verified:
  - `0x07`: Warm Yellow Light (3000K)
  - `0x09`: Blue / Cool White Light (6500K)
  - `0x0C`: Bright Natural Yellow Light (4000K)
  - `0x18`: 100% Max Brightness
  - `0x1C`: 50% Medium Brightness
  - `0x15`: 5% Low Eco / Night Light

### Changed
* **Active Hardware Debugging Status**: Updated project documentation (`README.md` and `CHANGELOG.md`) noting active troubleshooting for BAIERDI Power ON/OFF mapping and PWM repeat pulse slider timing.

---

## [1.2] - 2026-09-07

### Added
* **Project Rebrand to AirSignal**: Rebranded application and project architecture from UniversalIR to **AirSignal**, expanding scope from IR-only transmissions to dual Infrared (IR) and Radio Frequency (RF) smart signal management.
* **Targeted Category Scanning (`IrCodeDatabase.kt`)**: Mapped IR/RF candidate registry with multi-frequency support (36.0 kHz, 36.7 kHz, 38.0 kHz, and 40.0 kHz) categorized by `ApplianceType` (`LAMP`, `TV`, `AC`, `FAN`, `SOUNDBAR`, `MEDIA_PLAYER`, `PROJECTOR`).
* **Live Scan Feedback & Progress UI (`dialog_scanning_progress.xml`)**: Added custom dialog featuring a horizontal `ProgressBar`, real-time test index counter, active carrier frequency label, and instruction guidance.
* **Asynchronous Non-Blocking Scanner (`MainActivity.kt`)**: Implemented main-thread-safe `Handler`/`Looper` scheduler transmitting candidate codes at a 2.5s interval with real-time UI updates.
* **Scan Cancellation & Match Capture**: Added "Stop Scanning" cancellation hook and "Device Reacted!" handler.

### Changed
* **Refactored `IrRepository`**: Standardized hardware presence check using native `ConsumerIrManager.hasIrEmitter()` API.
* **Repository Alignment**: Updated project documentation to link to `https://github.com/johnhuffmane-web/AirSignal.git`.

---

## [1.1] - 2026-09-07

### Added
* **Root Build Setup (`build.gradle.kts`)**: Introduced Kotlin DSL project-level configuration with AGP `v8.5.2` and Kotlin `v2.0.21`.
* **Android Manifest (`AndroidManifest.xml`)**: Added `TRANSMIT_IR` permissions and Material3 theme bindings.
* **Hardware Management (`IrRepository.kt`)**: Implemented system `ConsumerIrManager` service integration.
* **Dynamic Control Activity (`ControlActivity.kt`)**: Built dynamic layout rendering engine.
