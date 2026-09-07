# AirSignal (v1.3 Dev Build)

**AirSignal** is an Android mobile remote control platform built to bridge direct consumer infrared (IR) hardware emissions with Radio Frequency (RF) smart home protocol management. AirSignal turns mobile devices into universal control hubs for Televisions, Air Conditioners, Smart Lamps, Fans, Soundbars, Media Players, and Projectors.

Built with modern Android architectural patterns, AirSignal incorporates hardware abstraction layers, non-blocking asynchronous transmission loops, dynamic runtime UI generation, targeted category signal scanning, and intelligent protocol guidance for both optical IR and wireless RF devices.

> **Development Status**: AirSignal is currently in **Active Protocol Refinement & Dev Mode**, focusing on deep signal state machine troubleshooting for complex CCT LED lamp drivers (such as BAIERDI floor lamps, SKU `4766740590_PH-31007347311`) while providing a fully customizable **Universal Remote Canvas** and **Remote Backup Vault**.

---

## Technical Highlights & Core Architecture

* **Official LSB-First NEC Protocol Engine (`IrCodeDatabase`)**:
  - Implements true official LSB-first (Least Significant Bit first) 32-bit NEC framing (`necLsbToRawPattern`) required by Chinese CCT LED lamp microcontrollers.
  - Implements microsecond burst repeat sequences (`necLsbRepeatPattern`) operating at 110ms frame intervals to simulate continuous smooth PWM slider dimming.

* **Macro Sequence Engine (`sendMacroSequence`)**:
  - Automatically resolves state-machine deadlocks in CCT drivers (such as switching from 4000K Natural Yellow to 3000K Warm Yellow) by sending automated reset and double-lock sequences (`0x09` Cool White Reset -> `0x07` Warm Yellow -> `0x07` Lock) spaced by 200ms gaps.

* **Single-Click Auto-Scan Engine with Pause & Resume (`ControlActivity`)**:
  - Systematically tests all 256 candidate command bytes across Address `0x00FF` LSB.
  - Features a live **Pause / Resume** hook (`pauseScanButton`) allowing developers to freeze the scan timer, inspect active HEX payload details, take notes, and resume without restarting.

* **Custom Universal Remote Canvas & Dynamic Renaming**:
  - Every remote control profile is rendered as an interactive, customizable canvas.
  - Users can rename any button label anytime (✎) or delete unused signal keys (🗑️).
  - Custom signal generator (`+ Add Custom Code`) converts any raw hex byte (e.g. `0x07`, `0x09`, `0x0C`, `0x18`) into a functional button instantly.

* **JSON Remote Backup Vault (`MainActivity`)**:
  - Full export and import capability (`AirSignal_Vault_Backup`) using Gson serialization.
  - Allows users to copy their entire remote database to the clipboard or back it up to cloud/email, ensuring remotes are never lost if the physical remote or phone is replaced.

---

## Technical Stack & Specifications

| Component | Specification |
| :--- | :--- |
| **Project Name** | AirSignal |
| **Current Build** | v1.3 Development Build |
| **Language** | Kotlin 2.0.21 |
| **Build System** | Gradle 8.5.2 (Kotlin DSL `.kts`) |
| **Compatibility** | Android 5.0 (API 21) to Android 14 (API 34) |
| **Hardware Integration** | Consumer IR Emitter (`android.hardware.consumerir`) & RF Smart Gateway Protocols |
| **Permissions** | `android.permission.TRANSMIT_IR` |
| **Design System** | Google Material Design 3 (`Theme.Material3.DayNight`) |
| **Serialization** | Google Gson 2.10.1 |

---

## Verified Command Mapping for BAIERDI CCT Lamp (Address 0x00FF LSB)

| Command Byte | Function / Color State | Verification Status |
| :--- | :--- | :--- |
| **`0x07`** | Warm Yellow Light (3000K) | **Verified** *(Double-Lock Macro: 0x09 -> 0x07 -> 0x07)* |
| **`0x09`** | Blue / Cool White Light (6500K) | **Verified** |
| **`0x0C`** | Bright Natural Yellow (4000K) | **Verified** |
| **`0x18`** | 100% Max Brightness | **Verified** *(3-Burst Hold)* |
| **`0x1C`** | 50% Medium Brightness | **Verified** *(3-Burst Hold)* |
| **`0x15`** | 5% Low Eco / Night Light | **Verified** |
| **`0x08` / `0x88`** | Power OFF / Power ON candidates | *Active Debugging / In Progress* |

---

## Roadmap & Next Steps

1. **Continuous Dimmer PWM Hold Simulation**:
   - Refine repeat pulse timing intervals to match physical remote slider behavior.
2. **Power ON/OFF Sweep**:
   - Sweep remaining single-byte toggles on Address `0x00FF` (`0x02`, `0x12`, `0x0A`, `0x04`, `0x01`, `0x03`, `0x00`, `0x14`, `0x1A`) to lock in power toggle.
3. **Developer Mode / Clean User Mode Toggle**:
   - Introduce a mode toggle to hide developer auto-scanners and diagnostic matrix panels for production release.

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
   Deploy to a physical Android device equipped with an IR blaster hardware emitter.
