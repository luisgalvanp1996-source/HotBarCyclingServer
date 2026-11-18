# Hotbar Cycling — Fabric Client 0.4.0

**Target client**
- Minecraft 26.2
- Fabric Loader 0.19.5+
- Fabric API 0.161.0+26.2
- Java 25
- Mod Menu 20.0.3 (optional, recommended)

## Features
- `G`: rotate the selected virtual group.
- `Shift + G`: open the selected group's 3x3 editor.
- Config screen exposed through Mod Menu's gear/config button.
- Local settings saved in `.minecraft/config/hotbarcycling.properties`.
- Key mapping `G` is saved by Minecraft in `options.txt`; Shift remains the fixed modifier.
- Client preferences are synchronized to Paper when joining and when changed in the config screen.

## Build
From this folder with JDK 25 and Gradle 9.6.1+:

```powershell
gradle clean build
```

Output: `build/libs/HotbarCycling-Fabric-0.4.0.jar`

Mod Menu is a compile-only integration and is not a hard dependency. Without Mod Menu, the keybind and local config still work; settings can be edited through Mod Menu when installed.
