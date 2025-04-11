# mc-client-upgrader

**mc-client-upgrader** is a cross-platform tool for automating the setup and maintenance of Minecraft client installations, focused on managing version-locked mods and ensuring compatibility across custom game directories.

---

## ✨ Features

- ✅ Resolves compatible Minecraft versions based on required and optional mods
- ✅ Supports Fabric-based mods via [Modrinth API](https://docs.modrinth.com/)
- ✅ Automatically locates and uses Minecraft’s bundled Java runtime
- ✅ Supports Linux, macOS, and Windows with portable shell/batch scripts
- ✅ Isolated configuration via `properties.json` and per-client `mods.json`
- ✅ Automates the process of upgrading mods for specific game directories
- ✅ Sandbox-friendly for testing mod upgrades and configurations
- ✅ No external dependencies required on the target machine

---

## 🧱 Project Structure

```text
mc-client-upgrader/
├── build.gradle                  # Gradle project definition
├── settings.gradle               # Gradle settings
├── sandbox/                      # Dev sandbox for testing upgrades
│   └── config/
│       └── mods.json             # List of mods and optional flags
├── scripts/                      # Bootstrap + upgrade scripts
│   ├── find_latest_java.[sh|bat]
│   └── upgrade.[sh|bat]
├── src/main/java/                # Java sources
│   └── ModCompatibilityResolver.java
└── build/mcc-upgrader/           # Build output (distributable)
    ├── mc-upgrader.jar
    ├── properties.json
    ├── upgrade.sh / upgrade.bat
    └── find_latest_java.sh / .bat
```

---

## 🔧 Core Functions

### 1. Mod Compatibility Resolution
Reads a list of mods (`mods.json`) with required/optional flags and determines:
- ✅ Highest Minecraft version compatible with **all** mods
- ✅ Highest Minecraft version compatible with **required** mods only

### 2. Java Runtime Discovery
Auto-detects the newest available Minecraft-bundled Java runtime (cross-platform) to launch the upgrader.

### 3. Mod Upgrade Automation
Automates the upgrade process by fetching compatible mod versions and updating client `mods/` folders, according to a central configuration (`properties.json`).

---

## 🚀 Getting Started

```bash
./gradlew build     # Builds the project
```

Distributable artifacts will be found in:
```
build/mcc-upgrader/
```

Copy `properties.json.example` to `properties.json` and edit to point to your game directory root after deployment.

---

## 🌐 Cross-Platform Support

All upgrade logic is wrapped with:
- `upgrade.sh` / `find_latest_java.sh` for Linux/macOS
- `upgrade.bat` / `find_latest_java.bat` for Windows

No global Java installation needed.