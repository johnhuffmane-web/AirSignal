# AirSignal (v1.2)

**AirSignal** is an Android mobile remote control platform built to bridge direct consumer infrared (IR) hardware emissions with Radio Frequency (RF) smart home protocol management. AirSignal turns mobile devices into universal control hubs for Televisions, Air Conditioners, Smart Lamps, Fans, Soundbars, Media Players, and Projectors.

Built with modern Android architectural patterns, AirSignal incorporates hardware abstraction layers, non-blocking asynchronous transmission loops, dynamic runtime UI generation, targeted category signal scanning, and intelligent protocol guidance for both optical IR and wireless RF devices.

---

## Technical Highlights & Core Architecture

* **Dual Signal Management (IR & RF Protocols)**:
  - **Native Optical IR Engine**: Interfacing directly with Android's `ConsumerIrManager` service to generate precision microsecond pulse mark/space arrays across 36.0 kHz, 36.7 kHz, 38.0 kHz, and 40.0 kHz carrier frequencies.
  - **RF Remote & Smart Gateway Integration**: Built-in signal classification guidance that distinguishes line-of-sight optical IR signals from 433MHz / 315MHz / 2.4GHz Radio Frequency and Bluetooth devices (e.g., Lazada/Shopee Nordic LED floor lamps and RF fans), providing smart hub integration pathways.

* **Targeted Category Scanning & Frequency Matrix (`IrCodeDatabase`)**:
  - Indexed candidate registry organized by `ApplianceType` (`LAMP`, `TV`, `AC`, `FAN`, `SOUNDBAR`, `MEDIA_PLAYER`, `PROJECTOR`).
  - Pre-scan category filtering narrows search space and minimizes scan times by eliminating redundant frequency brute-forcing.
  - Comprehensive protocol coverage (NEC, Sony SIRC, RC5/RC6, Panasonic/Kaseikyo, and popular Chinese Nordic lamp controller profiles).

* **Asynchronous Non-Blocking Scan Engine (`MainActivity`)**:
  - Main-thread-safe asynchronous execution powered by `Handler` / `Looper` scheduling with an automated 2.5-second cadence per test code.
  - Real-time progress feedback via horizontal `ProgressBar` displaying current candidate index, active carrier frequency, and protocol payload details.
  - Lifecycle-aware cancellation ("Stop Scanning") preventing memory leaks and background transmissions (`onDestroy` teardown).
  - One-click signal capture ("Device Reacted!") that preserves matched carrier frequencies and pulse-length arrays (`IntArray`) into persistent appliance profiles.

* **Hardware Abstraction Layer (`IrRepository`)**:
  - Encapsulates system services with null-safe hardware presence verification (`hasIrEmitter`) to handle devices with or without physical IR hardware gracefully.

* **Dynamic Runtime Control Surface (`ControlActivity`)**:
  - Context-aware UI renderer that dynamically generates specialized controls based on appliance classification (e.g., Kelvin/dimmer controls for `LAMP`, temperature state panels for `AC`, and standard power toggles for `TV`/`SOUNDBAR`).

* **Local Profile Persistence (`DeviceStorage`)**:
  - Persistent profile management using `SharedPreferences` paired with Gson serialization for multi-device profile storage and command key mapping.

---

## Technical Stack & Specifications

| Component | Specification |
| :--- | :--- |
| **Project Name** | AirSignal |
| **Language** | Kotlin 2.0.21 |
| **Build System** | Gradle 8.5.2 (Kotlin DSL `.kts`) |
| **Compatibility** | Android 5.0 (API 21) to Android 14 (API 34) |
| **Hardware Integration** | Consumer IR Emitter (`android.hardware.consumerir`) & RF Smart Gateway Protocols |
| **Permissions** | `android.permission.TRANSMIT_IR` |
| **Design System** | Google Material Design 3 (`Theme.Material3.DayNight`) |
| **Serialization** | Google Gson 2.10.1 |

---

## Project Structure

```text
AirSignal/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/universalir/
│   │       │   ├── MainActivity.kt               # Main dashboard & asynchronous scanner
│   │       │   ├── ControlActivity.kt            # Dynamic control panel renderer
│   │       │   ├── DeviceStorage.kt              # Local JSON/SharedPreferences persistence
│   │       │   ├── hardware/
│   │       │   │   ├── IrRepository.kt           # ConsumerIrManager API wrapper
│   │       │   │   └── IrCodeDatabase.kt         # Mapped IR/RF carrier frequencies & signal protocols
│   │       │   └── model/
│   │       │       ├── Appliance.kt              # Appliance domain model & command map
│   │       │       └── ApplianceType.kt          # Categorized appliance enum
│   │       └── res/
│   │           └── layout/
│   │               ├── activity_main.xml
│   │               ├── activity_control.xml
│   │               ├── dialog_scanning_progress.xml  # Live scan progress & frequency UI
│   │               └── dailog_save_device.xml
│   └── build.gradle.kts                          # Module build script
├── build.gradle.kts                              # Root build script
├── settings.gradle                               # Plugin & dependency resolution management
├── gradle.properties                             # AndroidX & build properties
└── .gitignore                                    # Git exclusion rules
```

---

## Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/johnhuffmane-web/AirSignal.git
   cd AirSignal
   ```

2. **Build the Project**:
   Open in **Android Studio Jellyfish or newer** and execute:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Deploy to Hardware**:
   Deploy to a physical Android device equipped with an IR blaster hardware module (e.g., Xiaomi, Honor, or Huawei devices) or pair via smart hub bridge for RF devices.
