# Hotbar Cycling — Fabric Client 0.1.0

**Target client**
- Minecraft 26.2
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.2
- Java 25
- Fabric Loom 1.17.21
- Gradle 9.6.1 or newer

## Implemented in this slice
- Client-only Fabric mod.
- Configurable key mapping under Options > Controls > Hotbar Cycling.
- Default key: G.
- Sends the two-byte rotate request over `hotbarcycling:main`.
- Displays the Paper acknowledgement or permission rejection.
- Does not modify inventory locally; Paper remains authoritative.

## Build
Use JDK 25 and Gradle 9.6.1 or newer:

```powershell
gradle build
```

Output: `build/libs/HotbarCycling-Fabric-0.1.0.jar`

## Install
Copy the JAR into the Fabric client's `mods` folder. Keep Fabric API 0.161.0+26.2 installed.
The matching Paper plugin 0.2.0 must be installed on the server.

## Not implemented yet
The 3x3 editor, group/item synchronization, and GUI-based per-group settings are the next stage.
