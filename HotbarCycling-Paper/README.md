# Hotbar Cycling — Paper 0.4.0

**Target:** Paper 26.2 build 133, Java 25.

## Included
- Nine persistent virtual groups per player, one for each hotbar slot.
- Nine cells per group, with a server-side 3x3 editor.
- UUID-based YAML persistence for groups and server-side preference mirror.
- Plugin messaging channel `hotbarcycling:main`.
- C2S `[1,0]` rotates; C2S `[5,0]` opens editor; C2S `[6,flags]` synchronizes client preferences.
- Settings bit 0 = skip empty cells; bit 1 = reverse direction.
- `/hc status`, `/hc rotate`, and `/hc settings` remain available.

## Build
```powershell
gradle clean build
```
Output: `build/libs/HotbarCycling-Paper-0.4.0.jar`

Test with a backup and disposable items before production deployment.
