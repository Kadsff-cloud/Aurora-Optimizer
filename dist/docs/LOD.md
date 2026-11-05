# LOD lejano

## Idea

Aurora captura una **instantánea** de cada chunk: para cada celda de `cellSize`
bloques guarda la altura de la superficie y el color del bloque. Luego dibuja
esas celdas como quads planos más allá de la distancia de render vanilla.

No es un LOD texturizado como Voxy. Es una aproximación barata: sirve para ver
la silueta del terreno lejano sin cargar chunks completos ni geometría compleja.

## Captura

- `LodManager.onClientTick` recorre un anillo alrededor del jugador y captura
  como máximo 12 chunks cada 10 ticks.
- `ClientLevelMixin.unload` captura el chunk justo antes de que el cliente lo
  descargue, para no perder el borde del mundo visible.
- La captura usa `Heightmap.Types.MOTION_BLOCKING` y `BlockState.getMapColor`.
- Si el chunk está vacío o la altura queda fuera de los límites, la celda se
  marca como `EMPTY`.

## Worldgen (solo singleplayer)

`WorldgenLodGenerator` genera un LOD de terreno **por delante** del radio de
chunks cargado, sin cargar chunks completos:

- Usa el `ChunkGenerator` y el `RandomState` del servidor integrado.
- Muestrea altura con `ChunkGenerator.getBaseHeight(...MOTION_BLOCKING...)` y
  color con `BiomeSource.getNoiseBiome` + `Biome.getGrassColor/getWaterColor`.
- El trabajo se ejecuta en el hilo del servidor y se entrega al cliente en
  lotes de `lodWorldgenBatch` chunks por tick (4 por defecto; 8 en la prueba
  agresiva).
- Solo recorre anillos entre el borde del render vanilla y
  `lodWorldgenRadiusChunks` (18 por defecto), y se salta los chunks que ya
  están cargados o en cache.
- Cuando el anillo está completo, espera 100 ticks antes de volver a mirar.
- En multijugador no se ejecuta: se mantiene la captura de chunks cargados.

No es Voxy: no genera mallas texturizadas ni iluminación, solo la misma
instantánea de altura/color que la captura normal. La diferencia es que puede
rellenar el horizonte sin haber visitado esos chunks.

**Está desactivado por defecto** (`lodWorldgen=false`) porque cuesta FPS. La
medición conservadora (radio 18, lote 4, 12k quads) dio **596.8 FPS** frente a
**743.7** de Sodium-on: **-19.8%**. La agresiva (radio 20, lote 8, 60k quads)
dio **349.8 FPS**: **-53%**. La mayor parte del coste es la geometría dibujada
(11.031 y 23.779 quads), no solo la generación. Actívalo solo si prefieres el
horizonte lleno y aceptas la pérdida; el detalle está en
[`RENDIMIENTO.md`](RENDIMIENTO.md).

## Cache

`LodCache` es un `LinkedHashMap` en modo acceso (LRU) con límite duro. Al superar
`lodMaxChunks`, expulsa las entradas menos usadas. `snapshot()` devuelve una
lista inmutable que el renderer puede recorrer sin bloquear la captura.

## Render

`LodRenderer` se engancha a `LevelRenderEvents.COLLECT_SUBMITS`:

1. Toma un snapshot de la cache.
2. Calcula el inicio (`lodStartDistanceChunks`, elevado al borde del render
   vanilla para no solapar) y el alcance máximo.
3. Descarta chunks fuera de distancia o del frustum.
4. Mezcla el color con el color de niebla según la distancia.
5. Dibuja como máximo `lodMaxQuads` quads en una sola submission.

## Alcance real

Sin worldgen, Aurora no genera datos de terreno que el cliente no haya visto: la
captura ocurre en el anillo del borde del render y cuando un chunk se descarga.
El LOD funciona como **memoria de lo visitado**. Con `lodWorldgen=true` y un
mundo singleplayer, el generador rellena además un anillo por delante usando el
generador del servidor integrado; sigue sin cargar chunks completos ni generar
geometría texturizada. Es opt-in por su coste medido (~20% en la configuración
conservadora).

Con Sodium, el LOD **sí se dibuja**: el pipeline de Sodium envuelve el terreno,
pero el `SubmitNodeCollector` de vanilla sigue procesando la geometría custom.
La prueba de worldgen con Sodium cargado registró
`LOD: primer frame dibujado con 678 quads desde 8 chunks`. Aun así,
`lodMode=auto` cede el LOD cuando Sodium está presente por una decisión de
producto: quien usa Sodium busca el máximo FPS de terreno y el LOD añade un
coste pequeño que en escenas con colinas puede quedar casi oculto. Usa
`lodMode=aurora` para activarlo; el modo `auto` no lo dibuja con Sodium.

## Modos

| `lodMode` | Comportamiento |
|---|---|
| `auto` | Usa Aurora LOD, salvo que Voxy o Sodium estén instalados: entonces cede el LOD |
| `aurora` | Fuerza el LOD de Aurora aunque Voxy/Sodium estén presentes (con Voxy puede duplicar geometría; con Sodium se dibuja y añade su coste) |
| `voxy` | Aurora no dibuja LOD; se asume que Voxy lo hace |
| `off` | Sin LOD |

## Ajuste

- `lodCellSize`: 1 = más detalle y más quads; 8 = más barato y más grueso.
- `lodMaxDistanceChunks`: alcance máximo. Súbelo solo si el presupuesto de quads
  y la GPU lo permiten.
- `lodMaxQuads`: límite por frame. Si se alcanza, se corta el dibujado; no se
  encola trabajo para el frame siguiente.
- `lodStartDistanceChunks`: el renderer lo eleva a `renderDistance + 1` para no
  dibujar LOD dentro del terreno vanilla. Con `lodDebug=true` se usa el valor
  configurado y se omite el frustum para diagnosticar la geometría.
- `lodWorldgen`: activa la generación por delante en singleplayer (por defecto
  `false`, opt-in). En multijugador se ignora y solo se captura lo cargado.
- `lodWorldgenBatch`: chunks generados por tick (1–8; 4 por defecto). Más lote
  llena el horizonte antes, pero carga más el hilo del servidor integrado. La
  prueba agresiva usó 8; la conservadora, 4.
- `lodWorldgenRadiusChunks`: radio máximo del worldgen, acotado a
  `lodMaxDistanceChunks` (18 por defecto). Debe ser mayor que el render
  distance para aportar algo.

## Evidencia de funcionamiento

Al entrar en un mundo, el log muestra:

```
Mixin activo: nivel
LOD: primera captura en (x, z), cache N chunks / ...
LOD diag: active=true pose=true state=true camera=true chunks=N
LOD: primer frame dibujado con N quads desde M chunks
```

Con worldgen activo aparecen además:

```
LOD worldgen: generando anillos 17..20 en singleplayer, lote 8 chunk(s)/tick
LOD worldgen: primer chunk generado en (x, z), cache 1 chunks / 448 B
```

Y `/aurora status` muestra `chunks`, `RAM LOD`, `Quads LOD`, `chunks capturados`,
`worldgen` (chunks/muestras) y el coste medio en ms/chunk. En la prueba real se
generaron **144 chunks / 9.216 muestras** con un coste medio de pocos ms por
chunk; el detalle está en [`PRUEBAS.md`](PRUEBAS.md).
