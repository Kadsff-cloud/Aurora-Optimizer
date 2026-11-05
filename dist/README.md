# Aurora Optimizer

> **Idiomas / Languages**: este README está en español. English version: [README.en.md](README.en.md). El mod incluye la interfaz en **español e inglés**; sigue el idioma configurado en Minecraft.

Mod de optimización para **Minecraft 26.2 (Fabric)** centrado en tres frentes:

1. **Memoria**: caches acotadas, limpieza periódica, limpieza defensiva al salir del mundo, límite de partículas y tope de emisores de seguimiento.
2. **Renderizado**: culling de entidades por distancia, LOD lejano propio (capturado y generado en singleplayer) y extras de nubes/niebla.
3. **Paquete único**: bundle Jar-in-Jar con mods open source que complementan a Aurora.

Aurora funciona **solo o junto a Sodium, Sodium Extra, Iris y Voxy**. No es un reemplazo del pipeline de terreno de Sodium. La combinación recomendada es:

```
Sodium (terreno) + Aurora (memoria, culling, LOD, extras) + bundle open source
```

> **Honestidad sobre rendimiento**: en la tanda A/B del 2026-10-30, Sodium-on dio **743.7 FPS** (control final 754.8) y Aurora-only **708.5 FPS**; Sodium sigue siendo **~5–6.5% más rápido en terreno**. Aurora **no supera a Sodium en bruto** y no se afirma lo contrario. Donde Aurora sí aporta una mejora medida es en escenas con muchas entidades: con 1.350 cerdos, el culling subió de 756.9 a 923.6 FPS (**+22.0%**) usando Sodium en ambas corridas (medición anterior). El LOD capturado cuesta ~2–3.6%; el LOD worldgen conservador cuesta ~20% y el agresivo ~53%, así que es una función opt-in para activar a conciencia. Los detalles y límites están en [`docs/RENDIMIENTO.md`](docs/RENDIMIENTO.md).

---

## Estado

- Versión: `0.1.0`
- Minecraft: `26.2` (declarado `>=26.2 <26.4`)
- Java: `25`
- Fabric Loader: `0.20.1+`
- Fabric API: `0.161.0+26.2`
- Entorno: cliente y servidor (`environment: "*"`); los mixins son solo de cliente.
- Build verificado: `BUILD SUCCESSFUL` en común, core y perfil Sodium.
- Tests: **15/15, 0 fallos** (`AuroraConfigTest` 7, `ParticleBudgetTest` 3, `LodCacheTest` 5).
- Servidor dedicado verificado: `Done`.
- Cliente con mundo integrado verificado; mixins `entidades`, `particulas`, `nivel`, `render`.
- Bundle de 9 mods verificado en cliente:
  `Lithium=true | FerriteCore=true | MoreCulling=true | ClothConfig=true | RailOptimization=true | ImmediatelyFast=true | BadOptimizations=true | Clumps=true | CubesWithoutBorders=true`.
- Auto-LOD verificado con Sodium: `lodMode=auto` reporta `LOD=0 chunks/0 B` y registra la cesión a Sodium.
- Worldgen LOD verificado en singleplayer: **144 chunks generados, 9.216 muestras, 141 chunks en cache / 61,7 KB, 14.184 quads**; primer chunk en `(-55, -17)`.
- Leak guard verificado al desconectar: **1.681 chunks soltados**, tint caches limpiadas.
- Perfil Sodium verificado: `Aurora-0.1.0-sodium.jar` es cliente-only, exige `sodium >= 0.9.2` y **no empaqueta Sodium**.
- Limpieza de escena verificada: `#pigs has 0`, `#golems has 0`.
- **Idiomas**: interfaz en español (`es_es`) e inglés (`en_us`) en el mismo jar; sigue el idioma de Minecraft.

## Instalación

1. Instala **Fabric Loader 26.2** y **Fabric API 0.161.0+26.2**.
2. Copia `Aurora-0.1.0.jar` (build unificado) en `.minecraft/mods/`.
3. (Opcional, recomendado) instala **Sodium** para el terreno y **Voxy** si quieres LOD lejano texturizado. Si quieres que Sodium sea obligatorio en el propio jar, usa `Aurora-0.1.0-sodium.jar` (perfil `-PsodiumProfile`); Sodium sigue instalándose aparte.
4. Inicia el juego. La configuración se crea en `config/aurora.json`.

El jar unificado ya incluye, como mods anidados de Fabric Jar-in-Jar:

| Mod | Versión | Licencia |
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

**No instales esos nueve mods por separado** si usas el jar unificado: Fabric Loader detectaría ids duplicados. Si prefieres instalarlos tú, usa el build `-PcoreOnly`, que solo contiene Aurora.

Sodium y Voxy **no se incluyen**: Sodium usa Polyform Shield 1.0.0 (no open source) y Voxy es All Rights Reserved. Se instalan aparte.

## Funciones

### Memoria y fugas

- Cache LOD **LRU acotada** (`lodMaxChunks`, por defecto 4096). Cada entrada es una instantánea de alturas/colores; no guarda chunks, entidades ni referencias al nivel.
- Limpieza periódica configurable (`trimIntervalTicks`, por defecto 1200 ticks = 60 s).
- Limpieza inmediata al desconectar y al cerrar el cliente.
- **Leak guard** al salir del mundo: suelta chunks de la cache, limpia tint caches, block entities globales, progreso de destrucción y entidades del nivel.
- Límite global de partículas (`particleLimit`, por defecto 4000) que cuenta grupos, cola pendiente y emisores.
- Tope de emisores de seguimiento (`trackingEmitterLimit`, 256 por defecto).
- `/aurora trim` fuerza la limpieza sin esperar.
- Estadísticas de heap, memoria directa, entidades/chunks liberados y worldgen en `/aurora status`.

> La guardia de memoria acota las caches de Aurora y limpia partículas; no pretende arreglar leaks internos de vanilla. MemoryLeakFix quedó obsoleto desde 1.20.5 y no tiene build 26.2.

### Renderizado

- **Culling de entidades por distancia** (`entityCullDistance`, por defecto 96 bloques), con opciones para no ocultar jefes ni entidades con nombre.
- **LOD lejano propio**: captura altura y color por celda de chunk y dibuja quads de color más allá de la distancia de render. Presupuesto de quads configurable (`lodMaxQuads`).
- **Worldgen LOD (singleplayer, opt-in)**: genera el LOD por delante del render distance usando el `ChunkGenerator` del servidor integrado, sin cargar chunks completos. En multijugador se mantiene solo la captura de chunks cargados. Está desactivado por defecto porque la medición conservadora costó ~20% de FPS.
- **Modo auto con Voxy/Sodium**: si Voxy o Sodium están instalados y `lodMode=auto`, Aurora no dibuja su LOD. Con Voxy evita duplicar geometría; con Sodium es una decisión de coste (el LOD sí se dibuja con Sodium, pero añade unos pocos FPS). `lodMode=aurora` lo fuerza.
- **Extras**: altura de nubes, desactivar nubes y control de niebla por frame.
- **Medición**: `/aurora status` muestra FPS y ms/frame; `/aurora bench` promedia FPS, mínimo, máximo y frame time.

### Bundle open source

El build por defecto empaqueta los nueve mods de la tabla anterior sin fusionar su código. Cada uno conserva id, mixins, configuración y licencia. Aurora los detecta y evita duplicar trabajo:

- **Lithium**: ticking, IA, física y allocations del servidor integrado.
- **FerriteCore**: memoria de blockstates, modelos y datos de bloque.
- **MoreCulling**: culling de bloques y marcos.
- **Cloth Config**: dependencia de configuración de MoreCulling.
- **Rail Optimization**: actualización de raíles powered/activator.
- **ImmediatelyFast**: envío de buffers de HUD/texto/entidades.
- **BadOptimizations**: microoptimizaciones de cliente.
- **Clumps**: agrupa orbes de XP en el servidor integrado, reduciendo entidades.
- **Cubes Without Borders**: pantalla completa sin bordes (borderless/windowed) configurable. Es una utilidad de ventana, no una mejora de FPS; se incluye por ser open source, cliente y compatible con 26.2/Sodium.

Detalle, hashes y verificación en [`docs/BUNDLE.md`](docs/BUNDLE.md) y [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

### API de addons

Otros mods pueden declarar los entrypoints `aurora` y `aurora:client` para recibir inicialización, tick, limpieza y submissions de render. Ver [`docs/ADDONS.md`](docs/ADDONS.md).

## Comandos

| Comando | Efecto |
|---|---|
| `/aurora` o `/aurora status` | Estado de memoria, LOD, contadores, hooks y compatibilidad |
| `/aurora lod on` | Activa el LOD de Aurora |
| `/aurora lod off` | Desactiva el LOD |
| `/aurora lod auto` | Cede el LOD a Voxy o Sodium si están presentes |
| `/aurora lod voxy` | Fuerza el modo Voxy (Aurora no dibuja LOD) |
| `/aurora lod worldgen on\|off` | Activa/desactiva la generación de LOD por delante (solo singleplayer) |
| `/aurora particles <0-20000>` | Límite de partículas (`0` desactiva el límite) |
| `/aurora entities <16-512>` | Distancia de culling de entidades |
| `/aurora trim` | Limpia caches y partículas ahora |
| `/aurora reload` | Recarga `config/aurora.json` |
| `/aurora compat` | Muestra mods de rendimiento detectados |
| `/aurora bench [1-60]` | Mide FPS y ms/frame durante N segundos (10 por defecto) |
| `/aurora help` | Ayuda |

## Configuración

Archivo: `config/aurora.json`. Se sanea al cargar: valores fuera de rango se corrigen en vez de romper el juego.

| Campo | Defecto | Descripción |
|---|---:|---|
| `leakGuard` | `true` | Activa la limpieza periódica |
| `trimIntervalTicks` | `1200` | Intervalo de limpieza (100–72000) |
| `clearCachesOnDisconnect` | `true` | Limpia LOD/partículas al salir del mundo |
| `entityCulling` | `true` | Culling de entidades |
| `entityCullDistance` | `96` | Distancia en bloques (16–512) |
| `cullBosses` | `false` | Ocultar jefes a distancia |
| `cullNamedEntities` | `false` | Ocultar entidades con nombre |
| `particleLimit` | `4000` | Máximo de partículas (0–20000) |
| `reduceParticles` | `true` | Activa el límite |
| `trackingEmitterLimit` | `256` | Máximo de emisores de seguimiento (0–4096) |
| `cloudHeightOverride` | `false` | Forzar altura de nubes |
| `cloudHeight` | `192.0` | Altura usada si el override está activo |
| `disableClouds` | `false` | Ocultar nubes |
| `fogOverride` | `false` | Forzar niebla |
| `fogStart` / `fogEnd` | `0.75` / `1.0` | Fracciones de la distancia de render |
| `lodEnabled` | `true` | Activa el LOD |
| `lodMode` | `auto` | `auto` (cede a Voxy/Sodium), `aurora` (fuerza), `voxy` (sin LOD), `off` |
| `lodMaxChunks` | `4096` | Tamaño máximo de la cache LRU |
| `lodCellSize` | `2` | Bloques por celda (1, 2, 4 u 8) |
| `lodStartDistanceChunks` | `8` | Inicio del LOD (se eleva al borde del render vanilla) |
| `lodMaxDistanceChunks` | `96` | Alcance máximo del LOD |
| `lodMaxQuads` | `12000` | Presupuesto de quads por frame |
| `lodFog` | `true` | Mezcla el LOD con el color de niebla |
| `lodDebug` | `false` | Log detallado de LOD/limpieza (limitado a 1 línea/s) |
| `lodWorldgen` | `false` | Genera LOD por delante en singleplayer (opt-in) |
| `lodWorldgenBatch` | `4` | Chunks generados por tick (1–8); 8 llena el horizonte antes |
| `lodWorldgenRadiusChunks` | `18` | Radio máximo del worldgen, acotado a `lodMaxDistanceChunks` |
| `sodiumCompat` | `true` | Detección suave de Sodium |
| `voxyCompat` | `true` | Detección suave de Voxy |
| `logCompat` | `true` | Loguear compatibilidad al iniciar |

## Rendimiento medido

Escena de terreno: mundo `AuroraBench`, `x=-600,z=0`, yaw 270, pitch 25, 1920x1080, render distance 16, simulación 12, vsync off, 60 s de calentamiento y 30 s de medida. Una corrida por configuración.

| Configuración | FPS medio | Mín | Máx | ms/frame (1000/FPS) | Heap |
|---|---:|---:|---:|---:|---:|
| Aurora on (sin Sodium) | 708.5 | 515 | 752 | 1.41 | 726.2 MB |
| Sodium off (Aurora off) | 753.2 | 608 | 769 | 1.33 | 591.6 MB |
| Sodium on (Aurora on) | 743.7 | 600 | 766 | 1.34 | 1.3 GB |
| Sodium + LOD capturado | 727.4 | 553 | 744 | 1.37 | 914.2 MB |
| Sodium + LOD worldgen agresivo (radio 20, lote 8) | 349.8 | 229 | 383 | 2.86 | 538.6 MB |
| Sodium + LOD worldgen conservador (radio 18, lote 4) | 596.8 | 369 | 667 | 1.68 | 761.1 MB |
| Sodium on control | 754.8 | 591 | 773 | 1.32 | 764.6 MB |

Lectura honesta: en esta tanda **Sodium es ~5–6.5% más rápido que Aurora-only en terreno** (la brecha era ~19.7% en la tanda anterior; el equipo estaba en otro estado y no es directamente equivalente). Sodium-on frente a Sodium-off es **-1.3%**, dentro de la deriva del control (**+1.5%**): en esta escena de terreno el culling/partículas de Aurora no marca una diferencia medible. El LOD capturado cuesta **-2.2%** frente a Sodium-on (**-3.6%** frente al control). El LOD worldgen conservador cuesta **-19.8%** frente a Sodium-on (**-20.9%** frente al control) y dibuja 11.031 quads; el agresivo cuesta **-53%** con 23.779 quads. Ambos llenan el horizonte (visible en las capturas), pero son demasiado caros para un default: `lodWorldgen` queda en **`false`** (opt-in). Con `lodMode=auto` y Sodium presente, Aurora cede el LOD y reporta `LOD=0 chunks/0 B` (verificado en cliente).

Escena de entidades (Sodium en ambas corridas, mismo bundle):

| Entidades | Culling | FPS medio | Diferencia |
|---|---:|---:|---:|
| 450 (225 cerdos + 225 gólems) | ON | 737.8 | +0.7% (ruido) |
| 450 | OFF | 732.4 | — |
| 1.350 cerdos | ON | 923.6 | **+22.0%** |
| 1.350 | OFF | 756.9 | — |

La escena de 1.350 cerdos se midió con la cámara a `y=140`, pitch 0, para que las entidades quedaran contra el cielo. Capturas: `D:\OpenCode\entity-bench-heavy-cull-on.png` (sin cerdos) y `...-off.png` (con cerdos).

> Los FPS son del contador interno del juego. `options.txt` tenía `maxFps:260`, pero las cifras lo superaron; por eso se usan solo para comparar configuraciones en el mismo equipo, no como FPS absolutos. Hay una corrida por configuración y la varianza min/max es amplia.

## Compilar y probar

```bat
cd D:\Cosas\AI\opencode\projects\aurora-optimizer
.\gradlew.bat build --no-daemon --console=plain
.\gradlew.bat test --no-daemon --console=plain
```

- Build por defecto: `build/libs/Aurora-0.1.0.jar` (Aurora + 9 mods anidados).
- Solo Aurora: `.\gradlew.bat build -PcoreOnly` → `Aurora-0.1.0-core.jar` (sin terceros).
- Perfil Sodium: `.\gradlew.bat build -PsodiumProfile` → `Aurora-0.1.0-sodium.jar` (cliente-only, exige Sodium; no lo empaqueta).

Prueba de cliente con un mundo concreto:

```bat
.\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraTest
```

## Arquitectura

```
Aurora (common)                 AuroraClient (cliente)
├─ AuroraConfig                 ├─ MemoryGuard ── LevelLeakGuard
├─ AuroraCompat                 ├─ LodManager ── LodCache ── LodChunk
├─ AuroraAddons                 ├─ WorldgenLodGenerator (singleplayer)
└─ API de addons                ├─ LodRenderer
                                ├─ ParticleBudget
                                ├─ Extras
                                ├─ AuroraCommands
                                └─ HookProbe

Mixins de cliente: ClientLevel, ParticleEngine, EntityRenderDispatcher
```

Más detalle en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md), [`docs/MEMORIA.md`](docs/MEMORIA.md), [`docs/LOD.md`](docs/LOD.md), [`docs/COMPATIBILIDAD.md`](docs/COMPATIBILIDAD.md), [`docs/BUNDLE.md`](docs/BUNDLE.md) y [`docs/RENDIMIENTO.md`](docs/RENDIMIENTO.md).

## Límites conocidos

- Aurora **no reemplaza** el pipeline de terreno de Sodium. Si buscas el máximo FPS de chunks vanilla, usa Sodium; Aurora lo acompaña.
- El LOD propio es geométrico y barato (quads de color), no texturizado como Voxy. Su objetivo es dar contexto lejano con muy poca RAM/CPU.
- Sin worldgen, el LOD se alimenta de chunks que el cliente ya cargó: es memoria de lo visitado. Con `lodWorldgen=true` en singleplayer sí genera un anillo por delante, pero sigue sin texturas ni iluminación y no funciona en multijugador.
- `lodDebug=true` desactiva el frustum culling y usa `lodStartDistanceChunks` tal cual. Es solo diagnóstico; el log se limita a una línea por segundo.
- El culling de entidades y el límite de partículas cambian lo que se ve; son configurables.
- Sodium y Voxy no se distribuyen en el jar por sus licencias. El bundle solo contiene mods open source. El perfil Sodium exige Sodium pero tampoco lo empaqueta.
- Con Sodium, el LOD propio se dibuja (verificado), pero `lodMode=auto` lo cede para no añadir coste a quien busca el máximo FPS de terreno. Usa `lodMode=aurora` para activarlo.
- No hay una medición multirrepetición con intervalo de confianza; las cifras son de una corrida por configuración.

## Licencia

MIT. Ver `LICENSE`. Los mods anidados conservan sus licencias; ver `THIRD_PARTY_NOTICES.md`, `LICENSES/` y `META-INF/licenses/` dentro del jar.
