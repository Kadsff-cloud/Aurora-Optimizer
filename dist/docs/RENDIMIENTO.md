# Rendimiento: qué se midió y qué se puede afirmar

## Resumen

- **Terreno**: Sodium sigue siendo más rápido que Aurora-only. En la tanda A/B del
  2026-10-30, Sodium-on dio **743.7 FPS** (control final **754.8**) y Aurora-only
  **708.5 FPS**: Sodium es **~5.0% más rápido** frente a Sodium-on y **~6.5%**
  frente al control. Aurora **no supera a Sodium** en el pipeline de terreno y no
  se afirma lo contrario.
- **Entidades**: con 1.350 cerdos y Sodium en ambas corridas, el culling de
  Aurora dio **923.6 FPS** frente a **756.9 FPS** con culling off: **+22.0%**.
  Es una medición anterior, de una escena concreta, y se mantiene como evidencia
  separada.
- **LOD capturado**: **727.4 FPS**, un **-2.2%** frente a Sodium-on y **-3.6%**
  frente al control.
- **LOD worldgen**: la configuración agresiva (radio 20, lote 8, 60k quads) dio
  **349.8 FPS (-53%)**; la conservadora (radio 18, lote 4, 12k quads) dio
  **596.8 FPS (-19.8% frente a Sodium-on, -20.9% frente al control)**. Sigue
  siendo cara: por eso el worldgen queda **opt-in** (`lodWorldgen=false`).
- **Paquete recomendado**: `Sodium + Aurora + bundle open source`. Sodium hace el
  terreno; Aurora aporta culling, memoria, LOD y extras; el bundle aporta
  Lithium, FerriteCore, MoreCulling, Cloth Config, Rail Optimization,
  ImmediatelyFast, BadOptimizations y Clumps.
- **No hay superioridad universal**: en terreno Aurora pierde; en escenas con
  muchas entidades gana gracias al culling; el LOD es una función de contexto,
  no una mejora gratuita de FPS.

## Qué optimiza Aurora

Aurora no reemplaza el pipeline de terreno de Sodium. Sus palancas son:

- Culling de entidades por distancia (96 bloques por defecto).
- Límite global de partículas y tope de emisores de seguimiento.
- Cache LOD acotada, limpieza periódica y leak guard al salir del mundo.
- Extras de nubes/niebla.
- LOD lejano barato (quads de color) para chunks ya visitados y, en
  singleplayer, generado por delante con el `ChunkGenerator` integrado.
- Bundle de mods open source que cubren lógica, memoria, bloques, raíles y
  buffers.

Sodium optimiza generación, compilación y dibujado de secciones de terreno. Son
capas distintas y se pueden usar juntas.

## Metodología

### Escena de terreno

- Mundo: `AuroraBench`.
- Posición: `x=-600, z=0`; cámara sobre la superficie +30 bloques (`y≈99`).
- Orientación: yaw 270 (este), pitch 25.
- Resolución: 1920x1080.
- Render distance: 16; simulación: 12.
- VSync: off.
- Calentamiento: 60 s.
- Medición: 30 s con `/aurora bench 30`.
- Una corrida por configuración.
- Los benchmarks se hicieron antes de añadir Clumps al bundle; Clumps solo
  agrupa orbes de XP y no interviene en las escenas de terreno/entidades medidas.

Configuraciones de la tanda:

| Nombre | Sodium | Aurora |
| --- | --- | --- |
| `aurora-on` | No | Culling/partículas/limpieza on, LOD off |
| `sodium-off` | Sí | Todo off |
| `sodium-on` | Sí | Culling/partículas/limpieza on, LOD off |
| `sodium-lod-captured` | Sí | Como `sodium-on` + LOD capturado (`lodWorldgen=false`) |
| `sodium-lod-worldgen` | Sí | Como `sodium-on` + LOD worldgen agresivo (`lodWorldgen=true`, lote 8, radio 20, 60k quads) |
| `sodium-lod-worldgen-conservative` | Sí | Igual pero lote 4, radio 18, 12k quads |
| `sodium-on-control` | Sí | Repetición de `sodium-on` al final para medir deriva |

El control final existe porque una tanda secuencial puede calentar el equipo.
`control - sodium-on = +1.5%`; cualquier diferencia menor que esa no se considera
una señal fiable.

### Escena de entidades

- Mundo: `AuroraBench`.
- 450 entidades: 225 cerdos al oeste y 225 gólems al este, `NoAI`, `NoGravity`,
  `Silent`, `Invulnerable`.
- 1.350 cerdos: generados al este en una cuadrícula, a `y=140`.
- Cámara: `x=-600, z=0`; para la prueba de 1.350 cerdos, `y=140` y pitch 0,
  mirando al este, con las entidades contra el cielo.
- Sodium en ambas corridas; mismo bundle en ambas.
- Calentamiento: 45 s; medición: 20 s.
- A/B: `entityCulling=true` contra `entityCulling=false`.

### LOD capturado vs worldgen

- Todas las corridas usan Sodium, `lodMode=aurora`, render distance 16,
  calentamiento 60 s y medición 30 s.
- `sodium-lod-captured`: `lodWorldgen=false`; la cache se llena con la captura
  normal (12 chunks cada 10 ticks).
- `sodium-lod-worldgen`: `lodWorldgen=true`; el generador rellena el anillo
  17–20 en el hilo del servidor integrado. La variante agresiva usa lote 8,
  radio 20 y 60k quads; la conservadora usa lote 4, radio 18 y 12k quads.
- El renderer solo dibuja LOD más allá de `renderDistance + 1` (17 chunks), así
  que ambos modos compiten por la misma franja visible. La diferencia es cómo se
  llena la cache y cuánta geometría se dibuja.
- El coste de generación se mide aparte con `worldgenNanos / worldgenChunks`
  (ms/chunk) y se reporta en `/aurora status`; el FPS mide el coste de render en
  régimen, no el pico de generación.

### Auto-LOD

`lodMode=auto` cede el LOD cuando detecta Voxy (dibuja su propio LOD) o Sodium
(decisión de coste: el LOD sí se dibuja con Sodium, pero añade unos pocos FPS y
en escenas con colinas puede quedar casi oculto). La prueba en cliente con
Sodium y `auto` dio `LOD=0 chunks/0 B` y el log
`Sodium detectado: en lodMode=auto Aurora cede el LOD...`. Para medir el coste
del LOD propio con Sodium hay que forzar `lodMode=aurora`, como se hace en las
filas de LOD.

### Métrica

`/aurora bench` muestrea `Minecraft.getFps()` una vez por tick y promedia. El
ms/frame que se publica se deriva de `1000/FPS` para que sea coherente con la
media; la versión anterior usaba una muestra instantánea de `getFrameTimeNs()` y
podía dar valores incompatibles con los FPS.

## Resultados de terreno

| Configuración | FPS medio | Mín | Máx | ms/frame (1000/FPS) | Heap | LOD |
|---|---:|---:|---:|---:|---:|---|
| Aurora on (sin Sodium) | 708.5 | 515 | 752 | 1.41 | 726.2 MB | 0 |
| Sodium off (Aurora off) | 753.2 | 608 | 769 | 1.33 | 591.6 MB | 0 |
| Sodium on (Aurora on) | 743.7 | 600 | 766 | 1.34 | 1.3 GB | 0 |
| Sodium + LOD capturado | 727.4 | 553 | 744 | 1.37 | 914.2 MB | 60 chunks / 1.498 quads |
| Sodium + LOD worldgen agresivo (20/8/60k) | 349.8 | 229 | 383 | 2.86 | 538.6 MB | 518 chunks / 23.779 quads |
| Sodium + LOD worldgen conservador (18/4/12k) | 596.8 | 369 | 667 | 1.68 | 761.1 MB | 280 chunks / 11.031 quads |
| Sodium on control | 754.8 | 591 | 773 | 1.32 | 764.6 MB | 0 |

Detalle de la cache en las corridas de LOD:

- Capturado: `LOD=60 chunks/26.3 KB | quads=1498 | worldgen=0`.
- Worldgen agresivo: `LOD=518 chunks/226.6 KB | quads=23779 |
  worldgen=528 chunks/33792 muestras`.
- Worldgen conservador: `LOD=280 chunks/122.5 KB | quads=11031 |
  worldgen=284 chunks/18176 muestras`.

### Lectura

- Sodium-on frente a Aurora-on: **+5.0%** para Sodium. Frente al control final,
  **+6.5%**. La brecha de la tanda anterior (~19.7%) no es directamente
  comparable: el equipo estaba en otro estado y aquella tanda no tenía control.
- Sodium-off frente a Aurora-on: **+6.3%** para Sodium.
- Sodium-on frente a Sodium-off: **-1.3%**, dentro de la deriva del control
  (**+1.5%**). En esta escena de terreno, el culling/partículas de Aurora no
  marca una diferencia medible.
- LOD capturado: **-2.2%** frente a Sodium-on y **-3.6%** frente al control.
  Dibuja 1.498 quads y la cache solo tiene 60 chunks: el coste es pequeño, pero
  también es poca geometría.
- LOD worldgen agresivo: **-53.0%** frente a Sodium-on (**-53.7%** frente al
  control). Llena el horizonte (visible en la captura), pero dibuja 23.779 quads
  y no es aceptable como default.
- LOD worldgen conservador: **-19.8%** frente a Sodium-on (**-20.9%** frente al
  control). Dibuja 11.031 quads, **7.4 veces más** que el LOD capturado. La
  mayor parte del coste es geometría dibujada, no solo el hilo de worldgen; no se
  hizo un perfilado separado de CPU/GPU, así que esa atribución es una
  interpretación, no una medición aislada.
- Decisión: `lodWorldgen=false` por defecto. Quien lo active obtiene los
  defaults conservadores (lote 4, radio 18, 12k quads) y puede subirlos a
  conciencia. Se activa con `/aurora lod worldgen on` o editando
  `config/aurora.json`.
- Con `lodMode=auto` y Sodium presente, Aurora cede el LOD y reporta
  `LOD=0 chunks/0 B` (verificado en cliente). Para ver el LOD propio con Sodium
  hay que usar `lodMode=aurora`.

## Resultados de entidades

| Escena | Culling | FPS medio | Mín | Máx | ms/frame | Diferencia |
|---|---:|---:|---:|---:|---:|---:|
| 450 entidades | ON | 737.8 | 577 | 755 | 1.36 | +0.7% (ruido) |
| 450 entidades | OFF | 732.4 | 636 | 747 | 1.37 | — |
| 1.350 cerdos | ON | 923.6 | 745 | 958 | 1.08 | **+22.0%** |
| 1.350 cerdos | OFF | 756.9 | 538 | 816 | 1.32 | — |

### Lectura

- Con 450 entidades, la diferencia es **0.7%** y no se puede separar del ruido.
  La escena está limitada por terreno/GPU, no por entidades.
- Con 1.350 entidades y la cámara a su altura, el culling ahorra trabajo de
  render y la diferencia es **+22.0%**. Es la evidencia más clara de que el
  culling aporta valor cuando hay muchas entidades a distancia.
- Capturas:
  - `D:\OpenCode\entity-bench-heavy-cull-on.png`: no se ven cerdos; el culling
    los oculta.
  - `D:\OpenCode\entity-bench-heavy-cull-off.png`: se ve la fila de cerdos en el
    cielo.
- La cifra de 450 entidades con culling ON/OFF se midió con pitch 25 y cámara a
  `y≈99`; los gólems a `y=140` quedaban parcialmente ocultos por la colina. Por
  eso se hizo la prueba pesada a `y=140` y pitch 0.

## Memoria

- La guardia de memoria mantiene acotada la cache LOD y limpia partículas; no es
  un benchmark de leaks de vanilla.
- El leak guard suelta chunks, limpia tint caches y elimina entidades al
  desconectar. En la prueba real soltó **1.681 chunks** y dejó el nivel sin
  referencias grandes; no se midió el heap antes/después con un perfilador, solo
  se verificó la limpieza en el log.
- En las corridas, el heap reportado osciló entre **479.7 MB y 1.3 GB** según
  configuración y momento. No se hizo una prueba de estabilidad de heap a lo
  largo de horas.
- FerriteCore reduce memoria de blockstates/modelos; su efecto no se aisló en
  estas pruebas.
- MemoryLeakFix no se incluye: los leaks que corregía quedaron resueltos en
  vanilla desde 1.20.5 y no hay build 26.2.

## Qué no se afirma

- **No** se afirma que Aurora sea más rápida que Sodium en terreno. Los números
  muestran lo contrario.
- **No** se afirma que Aurora sola sea mejor que Sodium sola en todos los
  escenarios.
- **No** se afirma que el +22% de entidades se traduzca en +22% en un mundo
  normal: depende de cuántas entidades haya, de la distancia y del cuello de
  botella.
- **No** se afirma que el LOD de Aurora sustituya a Voxy: es geometría de color,
  sin texturas ni iluminación. En singleplayer puede generar un anillo por
  delante, pero no funciona en multijugador.
- **No** se afirma que el LOD sea gratis: el capturado cuesta ~2–3.6% y el
  worldgen conservador ~20%. Son funciones de contexto, no mejoras de FPS.
- **No** hay multirrepetición ni intervalo de confianza. Una corrida por
  configuración; la varianza min/max es amplia. El control de deriva acota la
  comparación, no la convierte en estadísticamente robusta.
- Los FPS son del contador interno. `options.txt` tenía `maxFps:260`, pero las
  cifras lo superaron; úsalos para comparar configuraciones en el mismo equipo,
  no como FPS absolutos.

## Conclusión

La propuesta honesta es un **paquete**:

```
Sodium (terreno, licencia Polyform Shield, instalar aparte)
+ Aurora (memoria, culling, LOD, extras)
+ bundle open source (Lithium, FerriteCore, MoreCulling, Cloth Config,
  Rail Optimization, ImmediatelyFast, BadOptimizations, Clumps)
```

En terreno, Sodium es el motor y Aurora no lo supera. En escenas con muchas
entidades, el culling de Aurora sí marca una diferencia medida (**+22%** con
1.350 entidades). En memoria y funciones, Aurora y el bundle cubren cosas que
Sodium no ofrece. El LOD worldgen es una función experimental y opt-in: mejora
el horizonte, pero cuesta FPS y no debe venderse como una ganancia de
rendimiento.
