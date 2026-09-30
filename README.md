# ⚡ Shoka Launcher — Modular Life Tracker Engine

<div align="center">

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Lua](https://img.shields.io/badge/Lua-Scriptable_Engine-000080?style=for-the-badge&logo=lua&logoColor=white)
![Vim](https://img.shields.io/badge/Vim-CLI_Driven-019733?style=for-the-badge&logo=vim&logoColor=white)

**A Neovim-inspired, CLI-driven Android Launcher & Quantified Self Engine.**  
*Minimalist, keyboard-first, hot-reloadable Lua plugins, and 100% AMOLED true black optimization.*

</div>

---

## 🌟 Overview

**Shoka Launcher** (also known as the *Modular Life Tracker Engine*) replaces the conventional icon-grid Android Home screen with a keyboard-driven, modular terminal interface. Inspired by the **UNIX philosophy** and **Neovim’s extensible plugin architecture**, Shoka turns your home screen into a reactive feed of customizable cards driven by lightning-fast CLI commands.

Engineered specifically to run flawlessly on modest or older hardware (such as MediaTek Helio P35 devices) without battery drain or background bloat.

---

## 🏗️ Architecture & Interface Layout

The screen is split into three main reactive zones driven by Jetpack Compose and Kotlin `StateFlow`:

```
┌─────────────────────────────────────────────────────────────────┐
│ 🕒 ZONE 1: HEADER & CONTEXT                                    │
│ [22:45] ⚡ 85% Battery | RAM: 1.8GB free | Mode: Normal         │
├─────────────────────────────────────────────────────────────────┤
│ 📊 ZONE 2: DYNAMIC LAZYFEED (Reactive UI Blocks)                │
│                                                                 │
│  💧 [Hydration]       | ████████░░ 1,750 / 2,500 ml             │
│  🏋️ [Workout]         | Bench Press: 80kg x 5 reps [SET 3]     │
│  🎵 [Music Player]    | ♫ Synthwave Drive — Lazerhawk           │
│  📜 [Lua Custom Block]| Custom User Metric / API Feed           │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ ⌨️ ZONE 3: CLI TERMINAL & PROMPT                               │
│ > a 250                                                         │
│ └─ [Command Output: Added 250ml water (Total: 2,000ml)]        │
└─────────────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────────────┐
│                     SYSTEM ARCHITECTURE                         │
├─────────────────────────────────────────────────────────────────┤
│                        MainActivity                             │
│                             │                                   │
│                     ModuleRegistry                              │
│         (Merges UI Blocks via combine + flatMapLatest)          │
│                             │                                   │
│   ┌─────────────────────────┼─────────────────────────┐         │
│   ▼                         ▼                         ▼         │
│ Kotlin Native Modules   LuaModuleManager          Theme & System │
│ - AppLauncherModule     (Loads Lua scripts)      - SysCtlModule │
│ - HydrationModule       - LuaEngineBridge        - ThemeModule  │
│ - WorkoutModule         (Audio, Files, Storage)                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✨ Key Features

- **⌨️ CLI-First Navigation**: Access everything via instant hotkeys and concise terminal commands.
- **📜 Live Lua Plugin Engine**: Extend your home screen on the fly by placing `.lua` scripts in `/sdcard/Documents/ModularLife/modules/` — no recompilation needed.
- **⚡ Ultra-Low Footprint**: Optimized event-driven architecture (5s clock tick, zero polling receivers, minimal RAM footprint).
- **🌑 100% True AMOLED Black (`#000000`)**: Zero power wasted on screen illumination for OLED/AMOLED displays.
- **📱 Built-In Fuzzy App Launcher**: Quickly filter and launch installed Android apps straight from the command line (`o <app_name>`).
- **🎵 Standalone Media Engine**: Full-featured music control (playlists, shuffle, queue management, album artwork) accessible via command syntax or Lua bridge.
- **🔒 Dedicated Kiosk / Lock Task Mode**: Integrated administrative system controls (`sys pin`, `sys unpin`, `sys reboot`).

---

## 💻 CLI Command Reference

| Command Prefix | Syntax / Usage | Description |
| :--- | :--- | :--- |
| **`o`** | `o <app_name>` | Open/launch an installed app with fuzzy name search |
| **`a`** | `a <ml>` \| `a clear` | Log water intake in milliliters or reset daily counter |
| **`w`** | `w <exercise> <weight> <reps>` | Log workout sets (e.g. `w bench 80 5`) |
| **`m`** | `m play` \| `m pause` \| `m next` \| `m prev` | Control audio playback |
| | `m list` \| `m shuf` | View music queue or toggle shuffle mode |
| **`mod`** | `mod list` \| `mod reload` | List loaded modules or hot-reload Lua plugins |
| **`sys`** | `sys pin` \| `sys unpin` \| `sys status` | Manage Lock Task (Kiosk) mode and device state |
| **`help`** | `help` | Display list of all active registered commands |

---

## 🔌 Writing Custom Lua Plugins

Creating a new home screen widget is as simple as dropping a `.lua` file into your phone's storage.

### Example: `habit_tracker.lua`

```lua
local module = {
    id = "habit",
    name = "Habit Tracker",
    commandPrefix = "h",
    helpText = "h <check|reset> - Track daily habits"
}

local completed = false

function module.render()
    return {
        title = "Daily Habit",
        description = completed and "✅ Completed for today!" or "❌ Pending execution",
        tag = completed and "DONE" or "PENDING"
    }
end

function module.executeCommand(args)
    if args == "check" then
        completed = true
        return "Habit marked as complete!"
    elseif args == "reset" then
        completed = false
        return "Habit counter reset."
    end
    return "Unknown argument. Use 'check' or 'reset'."
end

Engine.register_module(module)
```

The system automatically detects the file, renders the UI block in Zone 2, and exposes the `h` command in the CLI prompt!

---

## 🛠️ Technology Stack

- **Framework**: Native Android
- **Language**: Kotlin 2.0
- **UI Engine**: Jetpack Compose with Material 3 Dark Theme
- **Scripting Engine**: LuaJ (Java-Lua Bridge)
- **Architecture**: Unidirectional Data Flow (UDF) with Kotlin `StateFlow` & Coroutines
- **Target SDK**: Android 14+ (API 34), Minimum SDK: API 26

---

## 🚀 Getting Started

### Requirements
- Android Studio Ladybug / Hedgehog (or newer)
- JDK 17
- Android Device running Android 8.0+ (API 26+)

### Installation & Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/eng-jomanoel/shoka-launcher.git
   cd shoka-launcher
   ```

2. **Build and Install**:
   Connect your Android device with USB Debugging enabled, then run:
   ```bash
   ./gradlew installDebug
   ```

3. **Set as Default Launcher**:
   - Press the **Home Button** on your Android device.
   - Select **Shoka Launcher** (or *Modular Life Tracker Engine*) and tap **Always**.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
