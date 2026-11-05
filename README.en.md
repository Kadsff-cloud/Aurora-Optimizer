# Aurora Optimizer

Optimization mod for **Minecraft 26.2 (Fabric)** focused on three areas:

1. **Memory**: bounded caches, periodic cleanup, defensive cleanup when leaving a world, a particle budget and a tracking-emitter cap.
2. **Rendering**: distance-based entity culling, Aurora's own far LOD (captured and singleplayer-generated) and cloud/fog extras.
3. **Single package**: a Jar-in-Jar bundle of open-source mods that complement Aurora.

Aurora works **alone or together with Sodium, Sodium Extra, Iris and Voxy**. It is not a replacement for Sodium's terrain pipeline. The recommended combination is:

```
Sodium (terrain) + Aurora (memory, culling, LOD, extras) + open-source bundle
```

> **Performance honesty**: in the 2026-10-30 A/B run, Sodium-on scored **743.7 FPS** (final control 754.8) and Aurora-only **708.5 FPS**; Sodium remains **~5–6.5% faster on terrain**. Aurora **does not beat Sodium** in raw terrain throughput and makes no such claim. Where Aurora does provide a measured gain is in scenes with many entities: with 1,350 pigs, culling raised 756.9 to 923.6 FPS (**+22.0%**) with Sodium in both runs (earlier measurement). Captured LOD costs ~2–3.6%; conservative worldgen LOD costs ~20% and aggressive worldgen ~53%, so worldgen is opt-in. Details and limits: [`docs/RENDIMIENTO.md`](docs/RENDIMIENTO.md) (Spanish).

---

## Status

- Version: `0.1.0`
- Minecraft: `26.2` (declared `>=26.2 <26.4`)
- Java: `25`
- Fabric Loader: `0.20.1+`
- Fabric API: `0.161.0+26.2`
- Environment: client and server (`environment: "*"`); mixins are client-only.
- Build verified: `BUILD SUCCESSFUL` for common, core and Sodium profiles.
- Tests: **15/15, 0 failures** (`AuroraConfigTest` 7, `ParticleBudgetTest` 3, `LodCacheTest` 5).
- Dedicated server verified: `Done`.
- Integrated-world client verified; `entities`, `particles`, `level`, `render` hooks.
- 9-mod bundle verified in the client:
  `Lithium=true | FerriteCore=true | MoreCulling=true | ClothConfig=true | RailOptimization=true | ImmediatelyFast=true | BadOptimizations=true | Clumps=true | CubesWithoutBorders=true`.
- Auto-LOD verified with Sodium: `lodMode=auto` reports `LOD=0 chunks/0 B` and logs the handoff to Sodium.
- Singleplayer worldgen LOD verified: **144 chunks generated, 9,216 samples, 141 cached chunks / 61.7 KB, 14,184 quads**; first chunk at `(-55, -17)`.
- Leak guard verified on disconnect: **1,681 chunks dropped**, tint caches cleared.
- Sodium profile verified: `Aurora-0.1.0-sodium.jar` is client-only, requires `sodium >= 0.9.2` and **does not bundle Sodium**.
- Scene cleanup verified: `#pigs has 0`, `#golems has 0`.
- **Languages**: English (`en_us`) and Spanish (`es_es`) in the same jar. The UI follows Minecraft's language setting.

## Installation

1. Install **Fabric Loader for 26.2** and **Fabric API 0.161.0+26.2**.
2. Copy `Aurora-0.1.0.jar` (unified build) into `.minecraft/mods/`.
3. (Optional, recommended) install **Sodium** for terrain and **Voxy** for textured far LOD. If you want Sodium to be a hard dependency of the jar itself, use `Aurora-0.1.0-sodium.jar` (`-PsodiumProfile`); Sodium is still installed separately.
4. Launch the game. The config is created at `config/aurora.json`.

The unified jar already includes, as Fabric Jar-in-Jar nested mods:

| Mod | Version | License |
| --- | --- | --- |
| Lithium | 0.25.3+mc26.2 | LGPL-3.0-only |
| FerriteCore | 9.0.0-fabric | MIT |
| MoreCulling | 1.8.1 | GPL-3.0-only |
| Cloth Config API | 26.2.155+fabric | LGPL-3.0-only |
| Rail Optimization | 2.0+mc26.1.2 | GPL-3.0-only |
| ImmediatelyFast | 1.16.5+26.2 | LGPL-3.0-or-later |
| BadOptimizations | 2.4.1 | MIT |
| Clumps | 26.2.1 | MIT |
| Cubes Without Borders | 5.0.0+26.2 | MIT |

**Do not install those nine mods separately** if you use the unified jar: Fabric Loader would detect duplicate ids. If you prefer to install them yourself, use the `-PcoreOnly` build, which contains only Aurora.

Sodium and Voxy are **not included**: Sodium uses Polyform Shield 1.0.0 (not open source) and Voxy is All Rights Reserved. Install them separately.

## Features

### Memory and leaks

- **Bounded LRU LOD cache** (`lodMaxChunks`, default 4096). Each entry is a height/color snapshot; it stores no chunks, entities or level references.
- Configurable periodic cleanup (`trimIntervalTicks`, default 1200 ticks = 60 s).
- Immediate cleanup on disconnect and client shutdown.
- **Leak guard** when leaving a world: drops cached chunks, clears tint caches, global block entities, destruction progress and level entities.
- Global particle limit (`particleLimit`, default 4000) counting groups, pending queue and emitters.
- Tracking-emitter cap (`trackingEmitterLimit`, default 256).
- `/aurora trim` forces cleanup immediately.
- Heap, direct memory, freed entities/chunks and worldgen stats in `/aurora status`.

> The memory guard bounds Aurora's caches and cleans particles; it does not claim to fix vanilla-internal leaks. MemoryLeakFix has been obsolete since 1.20.5 and has no 26.2 build.

### Rendering

- **Distance-based entity culling** (`entityCullDistance`, default 96 blocks), with options to keep bosses and named entities visible.
- **Aurora's own far LOD**: captures height and color per chunk cell and draws colored quads beyond render distance. Configurable quad budget (`lodMaxQuads`).
- **Worldgen LOD (singleplayer, opt-in)**: generates LOD ahead of render distance using the integrated server's `ChunkGenerator`, without loading full chunks. In multiplayer only loaded chunks are captured. Disabled by default because the conservative measurement cost ~20% FPS.
- **Auto mode with Voxy/Sodium**: if Voxy or Sodium is installed and `lodMode=auto`, Aurora does not draw its own LOD. With Voxy this avoids duplicate geometry; with Sodium it is a cost decision (Aurora LOD does render with Sodium, but adds a few FPS of cost). `lodMode=aurora` forces it.
- **Extras**: cloud height, disable clouds and per-frame fog control.
- **Measurement**: `/aurora status` shows FPS and ms/frame; `/aurora bench` averages FPS, min, max and frame time.

### Open-source bundle

The default build packages the nine mods above without merging their code. Each keeps its id, mixins, config and license. Aurora detects them and avoids duplicate work:

- **Lithium**: ticking, AI, physics and allocations on the integrated server.
- **FerriteCore**: blockstate, model and block-data memory.
- **MoreCulling**: block and item-frame culling.
- **Cloth Config**: configuration dependency for MoreCulling.
- **Rail Optimization**: powered/activator rail updates.
- **ImmediatelyFast**: HUD/text/entity buffer submission.
- **BadOptimizations**: client micro-optimizations.
- **Clumps**: groups XP orbs on the integrated server, reducing entities.
- **Cubes Without Borders**: configurable borderless/windowed fullscreen. It is a window utility, not an FPS feature; it is included because it is open source, client-side and compatible with 26.2/Sodium.

Details, hashes and verification: [`docs/BUNDLE.md`](docs/BUNDLE.md) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

### Addon API

Other mods can declare the `aurora` and `aurora:client` entrypoints to receive initialization, tick, cleanup and render submissions. See [`docs/ADDONS.md`](docs/ADDONS.md).

## Commands

| Command | Effect |
|---|---|
| `/aurora` or `/aurora status` | Memory, LOD, counters, hooks and compatibility |
| `/aurora lod on` | Enable Aurora LOD |
| `/aurora lod off` | Disable LOD |
| `/aurora lod auto` | Yield LOD to Voxy or Sodium if present |
| `/aurora lod voxy` | Force Voxy mode (Aurora does not draw LOD) |
| `/aurora lod worldgen on\|off` | Enable/disable ahead-of-player LOD generation (singleplayer only) |
| `/aurora particles <0-20000>` | Particle limit (`0` disables the limit) |
| `/aurora entities <16-512>` | Entity culling distance |
| `/aurora trim` | Clean caches and particles now |
| `/aurora reload` | Reload `config/aurora.json` |
| `/aurora compat` | Show detected performance mods |
| `/aurora bench [1-60]` | Measure FPS and ms/frame for N seconds (10 by default) |
| `/aurora help` | Help |

## Configuration

File: `config/aurora.json`. It is sanitized on load: out-of-range values are corrected instead of breaking the game.

| Field | Default | Description |
|---|---:|---|
| `leakGuard` | `true` | Enable periodic cleanup |
| `trimIntervalTicks` | `1200` | Cleanup interval (100–72000) |
| `clearCachesOnDisconnect` | `true` | Clear LOD/particles when leaving a world |
| `entityCulling` | `true` | Entity culling |
| `entityCullDistance` | `96` | Distance in blocks (16–512) |
| `cullBosses` | `false` | Hide bosses at distance |
| `cullNamedEntities` | `false` | Hide named entities |
| `particleLimit` | `4000` | Maximum particles (0–20000) |
| `reduceParticles` | `true` | Enable the limit |
| `trackingEmitterLimit` | `256` | Maximum tracking emitters (0–4096) |
| `cloudHeightOverride` | `false` | Force cloud height |
| `cloudHeight` | `192.0` | Height used when the override is active |
| `disableClouds` | `false` | Hide clouds |
| `fogOverride` | `false` | Force fog |
| `fogStart` / `fogEnd` | `0.75` / `1.0` | Fractions of render distance |
| `lodEnabled` | `true` | Enable LOD |
| `lodMode` | `auto` | `auto` (yield to Voxy/Sodium), `aurora` (force), `voxy` (no LOD), `off` |
| `lodMaxChunks` | `4096` | Maximum LRU cache size |
| `lodCellSize` | `2` | Blocks per cell (1, 2, 4 or 8) |
| `lodStartDistanceChunks` | `8` | LOD start (raised to the vanilla render border) |
| `lodMaxDistanceChunks` | `96` | Maximum LOD range |
| `lodMaxQuads` | `12000` | Quad budget per frame |
| `lodFog` | `true` | Blend LOD with fog color |
| `lodDebug` | `false` | Detailed LOD/cleanup logging (max 1 line/s) |
| `lodWorldgen` | `false` | Generate ahead-of-player LOD in singleplayer (opt-in) |
| `lodWorldgenBatch` | `4` | Chunks generated per tick (1–8); 8 fills the horizon sooner |
| `lodWorldgenRadiusChunks` | `18` | Maximum worldgen radius, clamped to `lodMaxDistanceChunks` |
| `sodiumCompat` | `true` | Soft Sodium detection |
| `voxyCompat` | `true` | Soft Voxy detection |
| `logCompat` | `true` | Log compatibility on startup |

## Measured performance

Terrain scene: `AuroraBench` world, `x=-600,z=0`, yaw 270, pitch 25, 1920x1080, render distance 16, simulation 12, vsync off, 60 s warmup and 30 s measurement. One run per configuration.

| Configuration | Avg FPS | Min | Max | ms/frame (1000/FPS) | Heap |
|---|---:|---:|---:|---:|---:|
| Aurora on (no Sodium) | 708.5 | 515 | 752 | 1.41 | 726.2 MB |
| Sodium off (Aurora off) | 753.2 | 608 | 769 | 1.33 | 591.6 MB |
| Sodium on (Aurora on) | 743.7 | 600 | 766 | 1.34 | 1.3 GB |
| Sodium + captured LOD | 727.4 | 553 | 744 | 1.37 | 914.2 MB |
| Sodium + aggressive worldgen LOD (radius 20, batch 8) | 349.8 | 229 | 383 | 2.86 | 538.6 MB |
| Sodium + conservative worldgen LOD (radius 18, batch 4) | 596.8 | 369 | 667 | 1.68 | 761.1 MB |
| Sodium on control | 754.8 | 591 | 773 | 1.32 | 764.6 MB |

Honest reading: **Sodium is ~5–6.5% faster than Aurora-only on terrain** in this run. Sodium-on vs Sodium-off is **-1.3%**, inside the control drift (**+1.5%**), so Aurora's culling/particles make no measurable difference in this terrain scene. Captured LOD costs **-2.2%** vs Sodium-on (**-3.6%** vs control). Conservative worldgen LOD costs **-19.8%** vs Sodium-on and draws 11,031 quads; aggressive costs **-53%** with 23,779 quads. Both fill the horizon but are too expensive as defaults: `lodWorldgen` stays **`false`** (opt-in).

Entity scene (Sodium in both runs, same bundle):

| Entities | Culling | Avg FPS | Difference |
|---|---:|---:|---:|
| 450 (225 pigs + 225 golems) | ON | 737.8 | +0.7% (noise) |
| 450 | OFF | 732.4 | — |
| 1,350 pigs | ON | 923.6 | **+22.0%** |
| 1,350 | OFF | 756.9 | — |

The 1,350-pig scene was measured with the camera at `y=140`, pitch 0, so entities stood against the sky.

> FPS values come from the game's internal counter. `options.txt` had `maxFps:260`, but the numbers exceeded it; use them to compare configurations on the same machine, not as absolute FPS. There is one run per configuration and the min/max spread is wide.

## Build and test

```bat
cd D:\Cosas\AI\opencode\projects\aurora-optimizer
.\gradlew.bat build --no-daemon --console=plain
.\gradlew.bat test --no-daemon --console=plain
```

- Default build: `build/libs/Aurora-0.1.0.jar` (Aurora + 9 nested mods).
- Aurora only: `.\gradlew.bat build -PcoreOnly` → `Aurora-0.1.0-core.jar` (no third parties).
- Sodium profile: `.\gradlew.bat build -PsodiumProfile` → `Aurora-0.1.0-sodium.jar` (client-only, requires Sodium; does not bundle it).

Client test with a specific world:

```bat
.\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraTest
```

## Architecture

```
Aurora (common)                 AuroraClient (client)
├─ AuroraConfig                 ├─ MemoryGuard ── LevelLeakGuard
├─ AuroraCompat                 ├─ LodManager ── LodCache ── LodChunk
├─ AuroraAddons                 ├─ WorldgenLodGenerator (singleplayer)
└─ addon API                    ├─ LodRenderer
                                ├─ ParticleBudget
                                ├─ Extras
                                ├─ AuroraCommands
                                └─ HookProbe

Client mixins: ClientLevel, ParticleEngine, EntityRenderDispatcher
```

More detail in [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md), [`docs/MEMORIA.md`](docs/MEMORIA.md), [`docs/LOD.md`](docs/LOD.md), [`docs/COMPATIBILIDAD.md`](docs/COMPATIBILIDAD.md), [`docs/BUNDLE.md`](docs/BUNDLE.md) and [`docs/RENDIMIENTO.md`](docs/RENDIMIENTO.md).

## Known limits

- Aurora **does not replace** Sodium's terrain pipeline. For maximum vanilla-chunk FPS, use Sodium; Aurora complements it.
- Aurora's LOD is geometric and cheap (colored quads), not textured like Voxy. Its goal is far context with very little RAM/CPU.
- Without worldgen, LOD is fed by chunks the client has already loaded: memory of visited terrain. With `lodWorldgen=true` in singleplayer it generates a ring ahead, but still without textures or lighting and does not work in multiplayer.
- `lodDebug=true` disables frustum culling and uses `lodStartDistanceChunks` as-is. Diagnostic only; logging is limited to one line per second.
- Entity culling and the particle limit change what you see; they are configurable.
- Sodium and Voxy are not distributed in the jar due to their licenses. The bundle only contains open-source mods. The Sodium profile requires Sodium but does not bundle it either.
- With Sodium, Aurora's LOD does render (verified), but `lodMode=auto` yields it to avoid adding cost for users who want maximum terrain FPS. Use `lodMode=aurora` to enable it.
- There is no multi-run confidence interval; figures are from one run per configuration.

## License

MIT. See `LICENSE`. Nested mods keep their licenses; see `THIRD_PARTY_NOTICES.md`, `LICENSES/` and `META-INF/licenses/` inside the jar.
