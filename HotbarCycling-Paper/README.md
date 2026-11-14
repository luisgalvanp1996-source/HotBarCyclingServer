# Hotbar Cycling — Paper 0.2.0

**Target:** Paper 26.2 build 133, Java 25.

## Included
- Nine groups per player, one group for each hotbar position.
- Nine virtual cells per group.
- UUID-based YAML persistence.
- Rotation core with the active cell represented by the real selected hotbar slot.
- Plugin messaging channel `hotbarcycling:main`.
- Incoming packet `[1, 0]` rotates the group for the server-selected hotbar slot.
- Reply `[2, activeIndex]` confirms rotation; `[3, 0]` means permission denied.
- Server-side permission check and request throttling.
- `/hc status` and `/hc rotate` for manual testing.

## Build
Use JDK 25 and Gradle 9.6.1 or newer:

```powershell
gradle build
```

Output: `build/libs/HotbarCycling-0.2.0.jar`

## Install on a TEST Paper server
1. Back up the test server.
2. Stop Paper.
3. Copy the JAR into `plugins/`.
4. Start Paper and check the console for `HotbarCycling 0.2.0 enabled`.
5. Install the matching Fabric client mod.
6. Test with disposable items first.

Do not install this prototype on the production server until the inventory and reconnect tests pass.
