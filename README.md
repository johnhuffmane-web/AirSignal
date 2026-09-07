# UniversalIR (v1.1)

UniversalIR is an advanced Android application designed to leverage built-in phone infrared (IR) blaster hardware, turning your mobile device into a versatile universal remote control for home appliances like televisions, air conditioners, and smart lamps.

---

## Features

* **Hardware IR Emulation**: Directly interfaces with Android's `ConsumerIrManager` service to transmit raw infrared carrier signals (optimized at 38kHz).
* **Hardware Detection Check**: Automatically verifies if the physical device contains an active IR emitter before allowing scans or transmissions.
* **Dynamic Appliance Control Center (`ControlActivity`)**:
    * Automatically adapts layout controls based on selected appliance types (`LAMP`, `AC`, `TV`, `SOUNDBAR`, etc.).
    * Features specialized sliders for dimmable lighting, temperature state controls for air conditioners, and standard power toggles.
* **Interactive Scan-and-Match Flow (`MainActivity`)**: Transmits test sequences, allows real-time device response verification, and saves custom appliance profiles securely using Gson serialization and local device storage.
* **Modern Project Architecture**: Built using Kotlin DSL (`.kts`), AndroidX support, and a clean separation of concerns between hardware repositories and activity UI controllers.

---

## Technical Stack & Requirements

* **Android Gradle Plugin (AGP)**: `8.5.2`
* **Kotlin Version**: `2.0.21`
* **Target Hardware**: Physical Android device equipped with an internal IR blaster (e.g., Honor 400).
* **Permissions Required**: `android.permission.TRANSMIT_IR`
* **Hardware Feature Flag**: `android.hardware.consumerir` (Required)

---

## Project Structure

```text
UniversalIR/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/universalir/
│   │       │   ├── MainActivity.kt
│   │       │   ├── ControlActivity.kt
│   │       │   ├── hardware/IrRepository.kt
│   │       │   └── model/...
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle
└── gradle.properties