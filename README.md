# Hotbar Cycling — coordinated development package

This archive contains two independent Gradle projects:

- `HotbarCycling-Paper`: server plugin, data storage and authoritative inventory rotation.
- `HotbarCycling-Fabric`: client mod, configurable keybind and plugin-message request.

## Environment
- Minecraft 26.2
- Paper 26.2
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.2
- Java 25
- Gradle 9.6.1 or newer

## Build both projects (PowerShell)
Open PowerShell in this folder and run:

```powershell
Push-Location .\HotbarCycling-Paper
gradle build
Pop-Location

Push-Location .\HotbarCycling-Fabric
gradle build
Pop-Location
```

The Paper JAR and Fabric JAR are generated in their respective `build\libs` folders.

## Current scope
This is a source-code development milestone, not precompiled JARs. The keybind-to-Paper rotation request is implemented. The 3x3 editor and item/group synchronization are not included yet.

Use a test server and disposable items. Do not deploy to the production Server404 instance until the rotation, reconnect, restart, and inventory-integrity tests have passed.
