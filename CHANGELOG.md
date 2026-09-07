# Changelog

All notable changes to the AirSignal project are documented in this file.

## [1.2] - 2026-09-07

### Added
* **Project Rebrand to AirSignal**: Rebranded application and project architecture from UniversalIR to **AirSignal**, expanding scope from IR-only transmissions to dual Infrared (IR) and Radio Frequency (RF) smart signal management.
* **Targeted Category Scanning (`IrCodeDatabase.kt`)**: Mapped IR/RF candidate registry with multi-frequency support (36.0 kHz, 36.7 kHz, 38.0 kHz, and 40.0 kHz) categorized by `ApplianceType` (`LAMP`, `TV`, `AC`, `FAN`, `SOUNDBAR`, `MEDIA_PLAYER`, `PROJECTOR`) to minimize scan times. Added specific NEC hex codes for popular Nordic/Lazada standing lamps (`0x00FF02FD`, `0x00FF00FF`, `0x00FFB04F`, `0x00FF629D`, `0x00FFA25D`).
* **Live Scan Feedback & Progress UI (`dialog_scanning_progress.xml`)**: Added custom dialog featuring a horizontal `ProgressBar`, real-time test index counter, active carrier frequency label, and instruction guidance.
* **Asynchronous Non-Blocking Scanner (`MainActivity.kt`)**: Implemented main-thread-safe `Handler`/`Looper` scheduler transmitting candidate codes at a 2.5s interval with real-time UI updates.
* **Scan Cancellation & Match Capture**: Added "Stop Scanning" lifecycle-safe cancellation hook to halt transmissions and "Device Reacted!" handler to capture working IR payloads and pre-fill save profiles.
* **RF Compatibility Notice & Guidance (`activity_main.xml`)**: Added clear dashboard notification regarding IR vs. 433MHz/2.4GHz RF remote differences to guide users with RF lamps or smart hub integrations.
* **Version Control Ignore Configuration (`.gitignore`)**: Added project-level `.gitignore` rules covering build outputs, `.gradle/` caches, and local property files.

### Changed
* **Refactored `IrRepository`**: Standardized hardware presence check using native `ConsumerIrManager.hasIrEmitter()` API with safe-call null handling.
* **Refactored `MainActivity`**: Replaced static single-code test dialog with step-by-step type selection and progress-driven scan-and-match workflow.
* **Repository Alignment**: Updated project documentation to link to `https://github.com/johnhuffmane-web/AirSignal.git`.

---

## [1.1] - 2026-09-07

### Added
* **Root Build Setup (`build.gradle.kts`)**: Introduced Kotlin DSL project-level configuration with Android Application plugin (`v8.5.2`) and Kotlin Android plugin (`v2.0.21`).
* **Gradle Settings (`settings.gradle`)**: Configured `pluginManagement` repositories (`google`, `mavenCentral`, `gradlePluginPortal`), Foojay toolchain resolver convention (`v1.0.0`), and strict `dependencyResolutionManagement` for the `:app` module.
* **Project Properties (`gradle.properties`)**: Enabled AndroidX support (`android.useAndroidX=true`) and official Kotlin code styling.
* **Android Manifest (`AndroidManifest.xml`)**: Added `TRANSMIT_IR` permissions, `android.hardware.consumerir` hardware feature requirements, Material3 DayNight theme bindings, and declarations for both `MainActivity` and `ControlActivity`.
* **Hardware Management (`IrRepository.kt`)**: Implemented system `ConsumerIrManager` service integration, hardware emitter detection (`hasIrEmitter`), and raw 38kHz IR signal pattern transmission handlers with logging.
* **Dynamic Control Activity (`ControlActivity.kt`)**: Built dynamic layout rendering engine supporting specialized controls for Lamps (brightness sliders), Air Conditioners (temperature states and controls), and standard media/TV power toggles via Gson appliance serialization.
* **Main Dashboard (`MainActivity.kt`)**: Implemented hardware readiness verification, persistent device storage loading, interactive scan-and-match test transmission dialog flows, and custom device profile creation workflows.
