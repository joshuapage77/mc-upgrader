# mc-client-upgrader

**mc-client-upgrader** is a cross-platform tool for automating the setup and maintenance of Minecraft client installations, focused on managing version-locked mods and ensuring compatibility across custom game directories.

---
## 🔧 Core Functions
Upgrades or downgrades:
* fabric loader version
* mod versions
* shader versions
Determines (based on mod optionality and command line parameters) the highest version supported by required mods

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
mvn clean package     # Builds the project
```
Distributable artifact will be found in: `target/mc-upgrader.zip`
This can be used for global minecraft upgrading or specific game upgrading. The ladder is recommended.

## Install

#### Recommended setup
```
minecraft
└── games
    ├── mc-updater
    └── <game folder>
        └── conf
            └── mods.json
```
* create a folder called games under the minecraft folder (whatever that is on your system)
* unzip mc-updater.zip so it creates a folder under games
* create a folder for your game
  * you can copy things you want to reuse from the global minecraft
    * config (folder)
    * options.txt

Configure game instance:
* create a mods.json in the games `config` folder with this structure:
```
[
  {
    "slug": "MOD NAME",
    "loader": "MOD LOADER",
    "optional": boolean,
    "useSnapshot": boolean,
    "type": "[mod|shader]",
    "releaseArtifacts": "[modrinth|enginehub]",
    "snapshotArtifacts": "[modrinth|enginehub]"
  },
  ...
]
```
example:
```json
[
   {
      "slug": "fabric-api",
      "loader": "fabric",
      "type": "mod",
      "releaseArtifacts": "modrinth",
      "snapshotArtifacts": "modrinth"
   },
   {
      "slug": "worldedit-cui",
      "loader": "fabric",
      "optional": true,
      "useSnapshot": true,
      "type": "mod",
      "releaseArtifacts": "modrinth",
      "snapshotArtifacts": "enginehub"
   }
]
```
Configure mc-updater:
* copy `properties.json.SAMPLE` to `properties.json`
* minecraft is the path to minecraft
  * can be absolute, relative, or use ~ for mac/unix
  * sample assumes the recommended structure
* Multiple games may be managed, each should have an object defined in the games array
* name - only used as an identifier if specifying a specific game to upgrade
* path - path to game folder

---

## 🌐 Cross-Platform Support

All upgrade logic is wrapped with:
- `upgrade.sh` / `find_latest_java.sh` for Linux/macOS
- `upgrade.bat` / `find_latest_java.bat` for Windows

No global Java installation needed.