# Aurora Optimizer — Modrinth page (English)

> Ready-to-paste copy for the Modrinth project form. Replace every `<PLACEHOLDER>` before publishing.

## Form fields

| Field | Value |
|---|---|
| **Project name** | Aurora Optimizer |
| **Slug** | `aurora-optimizer` |
| **Summary** | Client-side memory and rendering optimization for Fabric 26.2: bounded caches, a leak guard, distance-based entity culling, cheap far LOD, a particle budget, and a 9-mod open-source bundle. Works with Sodium and Voxy. English and Spanish UI. |
| **Project type** | Mod |
| **Categories** | Optimization, Utility |
| **Loaders** | Fabric |
| **Game versions** | 26.2 |
| **Environment** | Client and Server (client-focused; all mixins are client-only) |
| **License** | MIT |
| **Source** | `<ADD_REPOSITORY_URL>` |
| **Issues** | `<ADD_ISSUES_URL>` |
| **Wiki / Docs** | `<ADD_DOCS_URL>` (can point to the repository `docs/` folder) |
| **Discord** | `<ADD_DISCORD_URL>` (optional) |

## Gallery

Upload these existing files from `bench/screenshots/` in this order:

| # | File | Title | Description |
|---|---|---|---|
| 1 | `lang-en_us-status.png` | Aurora status (English) | Heap, direct memory, LOD, counters, hooks and compatibility in one screen. |
| 2 | `lang-es_es-status.png` | Aurora status (Spanish) | The same UI follows Minecraft's language setting. |
| 3 | `bundle-status.png` | Nine open-source mods detected | The unified jar loads Lithium, FerriteCore, MoreCulling, Cloth Config, Rail Optimization, ImmediatelyFast, BadOptimizations, Clumps and Cubes Without Borders as nested mods. |
| 4 | `entity-bench-heavy-cull-on.png` | Entity culling in a heavy scene | 1,350 pigs, Sodium in both runs: culling raised 756.9 to 923.6 FPS (+22.0%). |
| 5 | `bench-ab-aurora-on.png` | Far LOD and terrain benchmark | Aurora's cheap geometric LOD beyond render distance. |
| 6 | `bench-ab-sodium-lod-worldgen-conservative.png` | Optional worldgen LOD | Singleplayer-only ahead-of-player LOD. Opt-in because it costs FPS. |

## Version

| Field | Value |
|---|---|
| **Version name** | Aurora Optimizer 0.1.0 |
| **Version number** | `0.1.0` |
| **Release channel** | Beta (recommended for the first public build) |
| **Loaders** | Fabric |
| **Game versions** | 26.2 |
| **Dependencies** | Fabric Loader `>=0.20.1`, Minecraft `>=26.2 <26.4`, Java `>=25`, Fabric API `*` |
| **Optional** | Sodium, Sodium Extra, Iris, Voxy |

### Changelog

```markdown
First public release.

Added
- Bounded LRU far-LOD cache with configurable size (`lodMaxChunks`).
- Periodic cleanup and immediate cleanup on disconnect/shutdown.
- Leak guard on world exit: cached chunks, tint caches, global block entities, destruction progress and level entities.
- Distance-based entity culling with boss/named-entity options.
- Aurora far LOD (captured chunks) and optional singleplayer worldgen LOD.
- Global particle limit and tracking-emitter cap.
- Cloud height, disable-clouds and fog extras.
- `/aurora` commands: status, lod, particles, entities, trim, reload, compat, bench, help.
- English (`en_us`) and Spanish (`es_es`) UI in the same jar.
- Unified Jar-in-Jar build with nine open-source mods.
- `-PcoreOnly` build (Aurora only) and `-PsodiumProfile` build (client-only, Sodium required).

Verified
- 15/15 unit tests, 0 failures.
- Dedicated server starts with `Done`.
- Integrated client verified with `entities`, `particles`, `level` and `render` hooks.
- Leak guard on disconnect dropped 1,681 chunks and cleared tint caches.
- Sodium profile verified; Sodium is not bundled.

Known limits
- Aurora does not replace Sodium's terrain pipeline.
- Worldgen LOD is singleplayer-only, opt-in and expensive.
- First release: report rough edges on the issue tracker.
```

---

## Description (paste into Modrinth)

# Aurora Optimizer

**Client-focused memory and rendering optimization for Minecraft 26.2 (Fabric).**

Aurora targets three areas that are easy to measure and easy to bound:

- **Memory:** bounded caches, periodic cleanup, a defensive leak guard when leaving a world, a global particle budget and a tracking-emitter cap.
- **Rendering:** distance-based entity culling, Aurora's own cheap far LOD, and optional cloud/fog extras.
- **One package:** a Jar-in-Jar bundle of nine open-source mods that complement Aurora.

Aurora works alone or alongside **Sodium, Sodium Extra, Iris and Voxy**. It is **not** a replacement for Sodium's terrain renderer. The recommended setup is:

```text
Sodium (terrain) + Aurora (memory, culling, LOD, extras) + open-source bundle
```

## Performance honesty

Aurora does **not** beat Sodium on raw terrain throughput, and this page does not claim it does. In the 2026-10-30 A/B run (1920×1080, render distance 16, simulation 12, vsync off, 60 s warmup, 30 s measurement, one run per configuration):

| Configuration | Avg FPS | Min | Max | ms/frame |
|---|---:|---:|---:|---:|
| Aurora on (no Sodium) | 708.5 | 515 | 752 | 1.41 |
| Sodium off (Aurora off) | 753.2 | 608 | 769 | 1.33 |
| Sodium on (Aurora on) | 743.7 | 600 | 766 | 1.34 |
| Sodium + captured LOD | 727.4 | 553 | 744 | 1.37 |
| Sodium + conservative worldgen LOD | 596.8 | 369 | 667 | 1.68 |
| Sodium + aggressive worldgen LOD | 349.8 | 229 | 383 | 2.86 |
| Sodium on control | 754.8 | 591 | 773 | 1.32 |

Sodium is roughly **5–6.5% faster than Aurora-only on terrain** in this run. Sodium-on versus Sodium-off was **−1.3%**, inside the control drift (**+1.5%**), so Aurora's culling and particle work make no measurable difference in this terrain scene.

Where Aurora does show a measured gain is in **entity-heavy scenes**. With Sodium in both runs:

| Entities | Culling | Avg FPS | Difference |
|---|---:|---:|---:|
| 450 (225 pigs + 225 golems) | ON | 737.8 | +0.7% (noise) |
| 450 | OFF | 732.4 | — |
| 1,350 pigs | ON | 923.6 | **+22.0%** |
| 1,350 | OFF | 756.9 | — |

Captured LOD costs about **2–3.6%** versus Sodium-on. Conservative worldgen LOD costs about **19.8%**, and aggressive worldgen about **53%**, so worldgen LOD is **opt-in and disabled by default**.

> FPS values come from the game's internal counter. There is one run per configuration and the min/max spread is wide. Use these numbers to compare configurations on the same machine, not as absolute FPS.

## What's inside

### Memory and leak guard

- **Bounded LRU LOD cache** (`lodMaxChunks`, default 4096). Each entry is a height/color snapshot; it stores no chunks, entities or level references.
- **Periodic cleanup** (`trimIntervalTicks`, default 1200 ticks = 60 s).
- **Immediate cleanup** on disconnect and client shutdown.
- **Leak guard when leaving a world:** drops cached chunks, clears tint caches, global block entities, destruction progress and level entities. Verified: **1,681 chunks dropped** on disconnect.
- **Global particle limit** (`particleLimit`, default 4000) counting groups, pending queue and emitters.
- **Tracking-emitter cap** (`trackingEmitterLimit`, default 256).
- `/aurora trim` forces cleanup immediately.
- `/aurora status` reports heap, direct memory, freed entities/chunks and worldgen stats.

> The memory guard bounds Aurora's own caches and cleans particles. It does **not** claim to fix vanilla-internal leaks. MemoryLeakFix has been obsolete since 1.20.5 and has no 26.2 build.

### Rendering

- **Distance-based entity culling** (`entityCullDistance`, default 96 blocks), with options to keep bosses and named entities visible.
- **Aurora far LOD:** captures height and color per chunk cell and draws colored quads beyond render distance. Configurable quad budget (`lodMaxQuads`).
- **Worldgen LOD (singleplayer, opt-in):** generates LOD ahead of render distance using the integrated server's `ChunkGenerator`, without loading full chunks. In multiplayer only loaded chunks are captured. Disabled by default because the conservative measurement cost about 20% FPS.
- **Auto mode with Voxy/Sodium:** with `lodMode=auto`, Aurora yields LOD to Voxy or Sodium if present. `lodMode=aurora` forces it.
- **Extras:** cloud height, disable clouds and per-frame fog control.
- **Measurement:** `/aurora status` shows FPS and ms/frame; `/aurora bench` averages FPS, min, max and frame time.

### Open-source bundle (Jar-in-Jar)

The default build packages nine open-source mods **without merging their code**. Each keeps its id, mixins, config and license:

| Mod | Version | License |
|---|---|---|
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

**Sodium and Voxy are not included.** Sodium uses Polyform Shield 1.0.0 (not open source) and Voxy is All Rights Reserved. Install them separately.

## Installation

1. Install **Fabric Loader for 26.2** and **Fabric API 0.161.0+26.2**.
2. Copy `Aurora-0.1.0.jar` (unified build) into `.minecraft/mods/`.
3. Optional but recommended: install **Sodium** for terrain and **Voxy** for textured far LOD.
4. Launch the game. The config is created at `config/aurora.json`.

Available builds:

| Build | File | Contents |
|---|---|---|
| Unified | `Aurora-0.1.0.jar` | Aurora + nine nested open-source mods |
| Core | `Aurora-0.1.0-core.jar` | Aurora only, no third parties |
| Sodium profile | `Aurora-0.1.0-sodium.jar` | Client-only, requires Sodium `>=0.9.2`; Sodium is not bundled |

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

## Configuration highlights

File: `config/aurora.json`. Values are sanitized on load: out-of-range values are corrected instead of breaking the game.

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
| `lodMode` | `auto` | `auto`, `aurora`, `voxy` or `off` |
| `lodMaxChunks` | `4096` | Maximum LRU cache size |
| `lodCellSize` | `2` | Blocks per cell (1, 2, 4 or 8) |
| `lodStartDistanceChunks` | `8` | LOD start (raised to the vanilla render border) |
| `lodMaxDistanceChunks` | `96` | Maximum LOD range |
| `lodMaxQuads` | `12000` | Quad budget per frame |
| `lodFog` | `true` | Blend LOD with fog color |
| `lodDebug` | `false` | Detailed LOD/cleanup logging (max 1 line/s) |
| `lodWorldgen` | `false` | Generate ahead-of-player LOD in singleplayer (opt-in) |
| `lodWorldgenBatch` | `4` | Chunks generated per tick (1–8) |
| `lodWorldgenRadiusChunks` | `18` | Maximum worldgen radius, clamped to `lodMaxDistanceChunks` |
| `sodiumCompat` | `true` | Soft Sodium detection |
| `voxyCompat` | `true` | Soft Voxy detection |
| `logCompat` | `true` | Log compatibility on startup |

## Languages

English (`en_us`) and Spanish (`es_es`) are included in the same jar. The UI follows Minecraft's language setting.

## Compatibility

- **Fabric 26.2**, Java 25, Fabric Loader 0.20.1+.
- **Sodium, Sodium Extra, Iris, Voxy:** soft detection; no hard dependency.
- **Bundled:** Lithium, FerriteCore, MoreCulling, Cloth Config, Rail Optimization, ImmediatelyFast, BadOptimizations, Clumps and Cubes Without Borders.
- The unified jar is **client-focused**. On a dedicated server, use **Aurora Server Optimizer** instead. Do **not** install both unified jars on the same instance: they share nested mod ids (Lithium, FerriteCore, Rail Optimization).
- If a bundled mod is already installed separately, use the core build or remove the duplicate.

## Known limits

- Aurora **does not replace** Sodium's terrain pipeline. For maximum vanilla-chunk FPS, use Sodium; Aurora complements it.
- Aurora's LOD is geometric and cheap (colored quads), not textured like Voxy. Its goal is far context with very little RAM/CPU.
- Without worldgen, LOD is fed by chunks the client has already loaded. `lodWorldgen=true` only works in singleplayer and does not work in multiplayer.
- `lodDebug=true` disables frustum culling and uses `lodStartDistanceChunks` as-is. Diagnostic only; logging is limited to one line per second.
- Entity culling and the particle limit change what you see; both are configurable.
- Sodium and Voxy are not distributed in the jar due to their licenses.
- Figures are from one run per configuration; there is no multi-run confidence interval.
- This is the first public release. Expect rough edges and report them on the issue tracker.

## License and credits

Aurora's own code is **MIT**. Nested mods keep their licenses; see `THIRD_PARTY_NOTICES.md`, `LICENSES/` and `META-INF/licenses/` inside the jar. Sodium and Voxy are not included.

## Links

- **Source:** `<ADD_REPOSITORY_URL>`
- **Issues:** `<ADD_ISSUES_URL>`
- **Documentation:** `<ADD_DOCS_URL>`
- **Server companion:** Aurora Server Optimizer — `<ADD_SERVER_MODRINTH_URL>`

---

## Publishing checklist (not part of the description)

1. Replace every `<PLACEHOLDER>`: repository, issues, docs, Discord and the server companion URL.
2. Create the Modrinth project with the fields above. Select **Client** and **Server** as the environment, and **Fabric** as the loader.
3. Upload the six gallery images from `bench/screenshots/` in the listed order and set the English status screen as featured.
4. Create version `0.1.0`, choose **Beta**, paste the changelog, upload `Aurora-0.1.0.jar` and add Fabric API as a required dependency. Mark Sodium and Voxy as optional.
5. Confirm the description renders correctly (tables and code blocks) and that it contains no external download links.
6. Publish, then cross-link the server page in both directions.
7. Modrinth expects a source URL for open-source projects. This project currently has no git repository; create one and use its URL before publishing.
