# Universal IR Controller (v1.0)

A high-performance, native Android application built in Kotlin that transforms an IR-equipped smartphone into a universal remote control for household appliances (Televisions, Air Conditioners, Dimmable Lamps, Media Players, and Soundbars). 

Designed with a focus on low-level hardware integration, clean architecture, and dynamic UI rendering.

---

## Key Engineering Features

*   **Low-Level Hardware Abstraction (`ConsumerIrManager`):** Interacts directly with the device's physical infrared emitter to transmit customized microsecond timing pulses and carrier frequencies (e.g., 38kHz).
*   **Dynamic UI Composition:** Inspects appliance types at runtime to render context-aware control layouts—such as dimming/Kelvin sliders for lamps or mode and temperature selector panels for air conditioners.
*   **Persistent Local Storage:** Implements local state serialization via `Gson` and Android `SharedPreferences` to ensure user-defined appliances and custom configurations persist reliably across application sessions.
*   **Interactive Scanning Loop:** Features a built-in interactive scanning and matching workflow, allowing users to test raw code sequences, identify responding home appliances, and assign custom names and categories on the fly.

---

## Tech Stack & Architecture

*   **Language:** Kotlin (1.9+)
*   **IDE:** Visual Studio Code / Android SDK Build-Tools
*   **Framework/APIs:** Android Jetpack, `ConsumerIrManager`, Material Components
*   **Data Serialization:** Google Gson
*   **Architecture Pattern:** Repository Pattern & Clean Separation of Concerns (Hardware layer, Domain models, Local data storage, and UI controllers)

---

## Project Structure

```text
com.example.universalir/
│
├── hardware/
│   └── IrRepository.kt       # Low-level hardware IR transmitter interface
├── model/
│   ├── Appliance.kt          # Domain data model & appliance type definitions
│   └── DeviceStorage.kt      # Local JSON serialization and state persistence
├── ui/                       # Layout views & dialog managers
├── MainActivity.kt           # Main dashboard & device scanning engine
└── ControlActivity.kt        # Dynamic view renderer for specific appliance types

# Universal IR Controller (v1.0)

A high-performance, native Android application built in Kotlin that transforms an IR-equipped smartphone into a universal remote control for household appliances (Televisions, Air Conditioners, Dimmable Lamps, Media Players, and Soundbars). 

Designed with a focus on low-level hardware integration, clean architecture, and dynamic UI rendering.

---

## Key Engineering Features

*   **Low-Level Hardware Abstraction (`ConsumerIrManager`):** Interacts directly with the device's physical infrared emitter to transmit customized microsecond timing pulses and carrier frequencies (e.g., 38kHz).
*   **Dynamic UI Composition:** Inspects appliance types at runtime to render context-aware control layouts—such as dimming/Kelvin sliders for lamps or mode and temperature selector panels for air conditioners.
*   **Persistent Local Storage:** Implements local state serialization via `Gson` and Android `SharedPreferences` to ensure user-defined appliances and custom configurations persist reliably across application sessions.
*   **Interactive Scanning Loop:** Features a built-in interactive scanning and matching workflow, allowing users to test raw code sequences, identify responding home appliances, and assign custom names and categories on the fly.

---

## Tech Stack & Architecture

*   **Language:** Kotlin (1.9+)
*   **IDE:** Visual Studio Code / Android SDK Build-Tools
*   **Framework/APIs:** Android Jetpack, `ConsumerIrManager`, Material Components
*   **Data Serialization:** Google Gson
*   **Architecture Pattern:** Repository Pattern & Clean Separation of Concerns (Hardware layer, Domain models, Local data storage, and UI controllers)

---

## Project Structure

```text
com.example.universalir/
│
├── hardware/
│   └── IrRepository.kt       # Low-level hardware IR transmitter interface
├── model/
│   ├── Appliance.kt          # Domain data model & appliance type definitions
│   └── DeviceStorage.kt      # Local JSON serialization and state persistence
├── ui/                       # Layout views & dialog managers
├── MainActivity.kt           # Main dashboard & device scanning engine
└── ControlActivity.kt        # Dynamic view renderer for specific appliance types

## Roadmap & Future Development (V2.0)

*   [ ] **IoT / Network Bridging:** Integration with local Wi-Fi smart blasters (e.g., ESP32 microcontrollers or Broadlink hubs) to allow cross-platform control from iOS and non-IR devices.
*   [ ] **Expanded Raw Timing Database:** Built-in repository scaling to support hundreds of global manufacturer profiles for ACs, TVs, and sound systems.
*   [ ] **Macro Automation & Profiles:** Introduction of room-based grouping and custom macro triggers (e.g., a "Movie Night" profile that turns on the TV, dims the lamps, and sets the AC simultaneously).
*   [ ] **Cloud Sync:** Secure cloud backup via Firebase for user-defined custom appliance mappings and layouts.

Developed as a demonstration of low-level hardware control, local state management, and modern native Android development practices.