# KhalilXmodz.Port - Quake 1 for Android (QuakeSpasm + SDL2 + gl4es)

Native port skeleton: real QuakeSpasm engine, SDL2 window/audio, gl4es translating the engine's
desktop OpenGL calls to OpenGL ES 2, Java multi-touch overlay wired into the engine's input.

**Status: source complete, never compiled by the author.** See "Status" below.

## Build from your phone (no computer)
1. Create a GitHub account and a new repository (browser, "Desktop site" mode helps).
2. Upload `project.zip` to the repository root: **Add file -> Upload files**. Do not unzip it.
3. **Add file -> Create new file**, name it `.github/workflows/build.yml`, paste the contents of
   `build.yml` (provided next to this ZIP) and commit.
4. Open the **Actions** tab -> *Build KhalilXmodz.Port APK* -> **Run workflow**.
5. When the run is green, open it and download the artifact **KhalilXmodz-Port-apk** (a ZIP with the APK).
   If it is red, open the run, then download **build-logs** (or open the failing step) and send me the error.

## Install
Unzip the artifact, tap the `.apk`, allow "install unknown apps" for your browser/file manager, install.

## Game data (not included)
Quake data is copyrighted. Supply `pak0.pak` (shareware episode 1 works; retail also needs `pak1.pak`).
Put the file(s) anywhere on the phone, open the app, tap **IMPORT GAME DATA**, pick them.
They are copied to the app's private folder `.../files/quake/id1/` and validated (PAK header, and that the
start map exists inside). PLAY stays disabled until validation passes.

## Controls
Left stick = move (analog, dead zone, optional floating). Drag on the right side = look.
A jump, B fire (hold), C configurable (default swim down), Y next weapon, X previous weapon,
II pause, ESC engine menu, SET settings. All are real engine commands (`+jump`, `+attack`, `impulse 10`...).
Each finger has its own pointer id: move + aim + fire + switch weapon work simultaneously.
In menus a D-pad / OK / ESC pad appears. Settings -> *Edit HUD layout* moves/resizes elements
(SAVE / RESET / DONE).

## Settings (all backed by real cvars or Android behaviour)
fov, gamma, contrast, volume, bgmvolume, gl_texturemode, r_dynamic, r_particles, host_maxfps (30/45/60/unlimited),
scr_sbarscale/menuscale/conscale (HUD scale), crosshair + size, presets (Performance/Balanced/Quality),
render resolution scale (fixed Android surface size; applies on next launch), touch sensitivity X/Y, invert Y,
dead zone, opacity, start map. Settings persist in SharedPreferences.

Deliberately omitted (no real backend): VSync toggle, separate SFX volume (engine has one `volume`),
crosshair styles, GPU/temperature metrics. Music needs codec libraries that are not bundled.

## Adding maps
Set the start map in Settings (e.g. `e1m2`). Extra `.bsp` files can be imported (they go to `id1/maps/`).

## Layout
`app/src/main/cpp` CMake + JNI bridge | `java/com/khalilxmodz/port` UI | `scripts/` fetch, patch, verify |
`deps.lock` pinned refs | `.github/workflows/build.yml` CI.

## Status (honest)
| Item | State |
|---|---|
| Source files, workflow, scripts | written |
| Shell/Python script syntax, patcher against sample `in_sdl.c` | checked |
| Java / CMake / Gradle compile | NOT verified (no Android SDK or network where this was written) |
| gl4es + QuakeSpasm link and run on device | NOT verified |
| Splash, multi-touch, settings, FPS overlay behaviour | NOT verified |
| Dependency refs (tags) exist and are correct | NOT verified; `strict` ones fail the build loudly if wrong |
| Pinned by full SHA | not yet: after the first green build copy SHAs from `deps-resolved.txt` into `deps.lock` |

Likely first-build failures: tag names in `deps.lock`, gl4es CMake options on Android, a QuakeSpasm source
file that must be excluded/added in `CMakeLists.txt`, or JNI/Java API differences. Each shows clearly in `build.log`.
Infinix Smart 9 ABI is unverified; the APK contains both armeabi-v7a and arm64-v8a so either works.

License: GPL-2.0-or-later (see THIRD_PARTY.md).
