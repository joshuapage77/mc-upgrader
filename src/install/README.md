# Install Folder
This folder holds game specific install configurations and artifacts.
A `game` refers to a particular server using a set of mods which you want a
dedicated installation and game folder for.

## Folder Name
This name will be referenced in the pom file for property `game`.

## Folder Content
The following are expected to be in this folder:
* game.json
* game-icon.png
* copy (folder)

### game.json
Defines default settings to be used in the installer for the game
* gameName
  * The name of the game to be used in the installed
  * Also determines the folder name that will be used

** Example **

```json
{
  "gameName": "Domain Of Pages"
}
```

### game-icon.png
This will be used as the icon for the installer as well as the installation profile when it is created in minecraft

### copy (folder)
Any files that are in the `copy` folder will be copied into the installed game folder.
A recursive copy will be used, so if you wanted a file to be copied to the `config` folder 
of the game folder, you could create a `config` folder in the `copy` folder.

A common use for this will be to provide:
* `server.dat`
  * defines the minecraft server that this game is designed to connect to
* `config/mods.json`
  * Defines the mods (and shaders) used for the game, where they update from, and whether they are optional

** Example **
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