# Changelog

All notable changes to the UniversalIR project will be documented in this file.

## [1.1] - 2026-09-07

### Added
* **Root Build Setup (`build.gradle.kts`)**: Introduced Kotlin DSL project-level configuration with Android Application plugin (`v8.5.2`) and Kotlin Android plugin (`v2.0.21`).
* **Gradle Settings (`settings.gradle`)**: Configured `pluginManagement` repositories (`google`, `mavenCentral`, `gradlePluginPortal`), Foojay toolchain resolver convention (`v1.0.0`), and strict `dependencyResolutionManagement` for the `:app` module.
* **Project Properties (`gradle.properties`)**: Enabled AndroidX support (`android.useAndroidX=true`) and official Kotlin code styling.
* **Android Manifest (`AndroidManifest.xml`)**: Added `TRANSMIT_IR` permissions, `android.hardware.consumerir` hardware feature requirements, Material3 DayNight theme bindings, and declarations for both `MainActivity` and `ControlActivity`.
* **Hardware Management (`IrRepository.kt`)**: Implemented system `ConsumerIrManager` service integration, hardware emitter detection (`hasEmitter`), and raw 38kHz IR signal pattern transmission handlers with logging.
* **Dynamic Control Activity (`ControlActivity.kt`)**: Built dynamic layout rendering engine supporting specialized controls for Lamps (brightness sliders), Air Conditioners (temperature states and controls), and standard media/TV power toggles via Gson appliance serialization.
* **Main Dashboard (`MainActivity.kt`)**: Implemented hardware readiness verification, persistent device storage loading, interactive scan-and-match test transmission dialog flows, and custom device profile creation workflows.