# Pruebas y evidencia

## 1. Compilación

```bat
cd D:\Cosas\AI\opencode\projects\aurora-optimizer
.\gradlew.bat clean build --no-daemon --console=plain
```

Resultado: **BUILD SUCCESSFUL**.

El build por defecto genera un jar unificado con **9 mods anidados** en `META-INF/jars/`:

| Jar anidado | Mod |
| --- | --- |
| `gvQqBUqZ-f7vZ0VWU.jar` | Lithium 0.25.3+mc26.2 |
| `uXXizFIs-d5ddUdiB.jar` | FerriteCore 9.0.0-fabric |
| `51shyZVL-D5oVCouK.jar` | MoreCulling 1.8.1 |
| `9s6osm5g-Nv3xnWXd.jar` | Cloth Config 26.2.155+fabric |
| `QSvN5pBU-QQzYehRo.jar` | Rail Optimization 2.0+mc26.1.2 |
| `5ZwdcRci-pMWERcSu.jar` | ImmediatelyFast 1.16.5+26.2 |
| `g96Z4WVZ-JmPs4Wie.jar` | BadOptimizations 2.4.1 |
| `Wnxd13zP-dEMopoOJ.jar` | Clumps 26.2.1 |
| `ETlrkaYF-ZM0HUAhK.jar` | Cubes Without Borders 5.0.0+26.2 |

Verificación:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z=[IO.Compression.ZipFile]::OpenRead('D:\Cosas\AI\opencode\projects\aurora-optimizer\build\libs\Aurora-0.1.0.jar')
$z.Entries | Where-Object { $_.FullName -like 'META-INF/jars/*' } | Select-Object FullName,Length
$z.Dispose()
```

También incluye `THIRD_PARTY_NOTICES.md`, `LICENSE_aurora` y `META-INF/licenses/` con los textos de todos los mods evaluados.

### Perfil core

```bat
.\gradlew.bat build -PcoreOnly --no-daemon --console=plain
```

Genera `Aurora-0.1.0-core.jar` con clasificador propio, **sin jars anidados**
(`nested=0`) y sin dependencia de Sodium. No pisa el jar unificado.

### Perfil Sodium

```bat
.\gradlew.bat clean build -PsodiumProfile --no-daemon --console=plain
```

Genera `Aurora-0.1.0-sodium.jar` con:

- `"environment": "client"`;
- `"depends": { ..., "sodium": ">=0.9.2" }`;
- los mismos 9 jars anidados;
- **Sodium fuera del jar** (no aparece en `META-INF/jars/`).

Verificación del metadata:

```text
environment=client
depends.sodium=>=0.9.2
nested=9
sodium_inside=false
```

## 2. Tests unitarios

```bat
.\gradlew.bat test --no-daemon --console=plain
```

Cubren:

- Saneado de configuración (rangos, modos de LOD, niebla, límites de worldgen y emisores).
- LRU del LOD: límite, expulsión, actualización de una entrada existente, `clear`, `trimTo` y estimación de bytes.
- `LodChunk.of` y `estimatedBytes`.
- Cesión automática del LOD: `auto` cede ante Sodium y Voxy; `aurora` lo fuerza; si el usuario desactiva `sodiumCompat`/`voxyCompat`, `auto` no cede.
- `ParticleBudget`: suma de grupos + cola + emisores, valores negativos, límite 0 = ilimitado y tope de emisores independiente.
- Defaults del worldgen: `lodWorldgen=false` (opt-in), lote 4, radio 18 y presupuesto de 12k quads.

Resultado: **15 tests, 0 fallos**:

| Clase | Tests |
|---|---:|
| `AuroraConfigTest` | 7 |
| `ParticleBudgetTest` | 3 |
| `LodCacheTest` | 5 |

## 3. Servidor dedicado

```bat
.\gradlew.bat runServer --no-daemon --console=plain
```

El log muestra la carga de Aurora y `Done`:

```text
[Aurora] Aurora Optimizer 0.1.0 cargado (config: .\config\aurora.json)
[Server thread/INFO] (Minecraft) Done (0.660s)! For help, type "help"
```

Los mixins de cliente no se aplican en servidor. El jar común incluye CWB como
mod anidado cliente-only; Fabric Loader lo ignora en el servidor y el arranque
no falla. No aparece `Mixin apply failed` ni `Incompatible mod set`.

## 4. Cliente con mundo integrado

```bat
.\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraTest
```

El log debe mostrar:

```
Aurora Optimizer 0.1.0 cargado
Compatibilidad detectada -> Sodium: false, Sodium Extra: false, Iris: false, Voxy: false
Mixin activo: entidades
Mixin activo: particulas
Starting integrated minecraft server version 26.2
Mixin activo: nivel
```

En la prueba final con LOD:

```
LOD: primera captura en (x, z), cache N chunks / ...
LOD: primer frame dibujado con 3840 quads desde 60 chunks
```

`/aurora status` respondió con LOD, quads y hooks `entidades, particulas, nivel, render`.

### 4.1 Idiomas en cliente (`es_es` / `en_us`)

Prueba: `bench/scripts/lang-test.ps1 -Lang en_us` y `-Lang es_es`. El script
fija `lang:` en `run/options.txt`, abre `AuroraBench`, ejecuta `/aurora help` y
`/aurora status` y captura la ventana. Es el **mismo jar** en ambas corridas.

| Idioma | Resultado visible | Capturas |
| --- | --- | --- |
| `en_us` | Help y status en inglés; `Hooks: entities, level, particles, render`; cifras `538.0 MB / 7.9 GB` | `bench/screenshots/lang-en_us-help.png`, `lang-en_us-status.png` |
| `es_es` | Help y status en español; `Hooks: entidades, nivel, particulas, render` | `bench/screenshots/lang-es_es-help.png`, `lang-es_es-status.png` |

Los textos están en `src/main/resources/assets/aurora/lang/en_us.json` y
`es_es.json` (46 claves cada uno). Los logs conservan el formato estable que
parsean los scripts de benchmark; solo se localiza la salida de chat.

## 5. Bundle unificado en cliente

Prueba: `bench/scripts/bundle-test.ps1`, con Sodium 0.9.2 en `run/mods` y el jar unificado en el classpath de desarrollo.

Resultado del log:

```
Bundle 26.2 -> Lithium=true | FerriteCore=true | MoreCulling=true | ClothConfig=true | RailOptimization=true | ImmediatelyFast=true | BadOptimizations=true | Clumps=true | CubesWithoutBorders=true
```

`/aurora status`:

```
Estado /aurora: heap=829,5 MB / 7,9 GB | directo=1,9 MB | LOD=0 chunks/0 B | quads=0 | hooks=entidades, particulas, nivel, render
```

### Errores observados

La última corrida con los 9 mods contó **14 líneas de ERROR**. No son fallos de
mixins ni de CWB:

- `Failed to fetch user properties` (401, cuenta de desarrollo sin sesión).
- `Failed to fetch Realms feature flags` / `Failed to retrieve profile key pair` (401/Realms).
- `Error reading pack metadata` (resource pack sin metadata completa).

No aparece `Mixin apply failed`, `MixinApplyError`, `MixinTransformerError` ni
`Incompatible mod set`. El log de la prueba confirma
`CubesWithoutBorders=true` y el cliente abrió el mundo con normalidad.

Capturas: `bench/screenshots/bundle-compat.png`, `bench/screenshots/bundle-status.png`.

## 6. LOD: auto, worldgen y leak guard

### 6.1 Auto-LOD con Sodium

Prueba: `bench/scripts/auto-lod-test.ps1`, con Sodium 0.9.2 en `run/mods`, `lodMode=auto`, `lodEnabled=true`, `sodiumCompat=true` y `voxyCompat=true`.

Resultado:

```
Estado /aurora: heap=943,5 MB / 7,9 GB | directo=3,0 MB | LOD=0 chunks/0 B | quads=0 | hooks=entidades, particulas, nivel, render
Sodium detectado: en lodMode=auto Aurora cede el LOD y deja el terreno a Sodium; usa lodMode=aurora para forzar el LOD propio.
```

No aparece `LOD: primera captura...`: Aurora no captura ni dibuja LOD cuando cede el control. Esto confirma en cliente la lógica cubierta por los tests unitarios: con Sodium y `auto`, el LOD propio queda en `0 chunks/0 B`; con `lodMode=aurora` se fuerza.

### 6.2 Worldgen LOD en singleplayer

Prueba: `bench/scripts/worldgen-test.ps1`, mundo `AuroraBench`, `lodMode=aurora`, `lodWorldgen=true`, lote 8, radio 20, render distance 16, Sodium 0.9.2 cargado.

Resultado del log:

```
LOD worldgen: generando anillos 17..20 en singleplayer, lote 8 chunk(s)/tick
LOD worldgen: primer chunk generado en (-55, -17), cache 1 chunks / 448 B
Estado /aurora: ... LOD=141 chunks/61,7 KB | quads=14184 | worldgen=144 chunks/9216 muestras ...
```

Al final de la sesión la cache llegó a **205 chunks / 21.389 quads**. El primer frame dibujado con Sodium se registró como:

```
LOD: primer frame dibujado con 678 quads desde 8 chunks
```

Esto también demuestra que Sodium no descarta la geometría custom de Fabric: el `SubmitNodeCollector` sigue procesando el LOD. Lo que hace `lodMode=auto` es cederlo por política de coste, no porque sea invisible.

### 6.3 Leak guard al desconectar

En la misma prueba, al salir al título:

```
Aurora leak guard: nivel descargado; entidades 1 -> 1 (removidas 0), chunks soltados 1681, tint caches limpiadas
```

Solo estaba el jugador, por eso no se removió ninguna entidad; los **1.681 chunks** de la cache del cliente sí se soltaron y las tint caches se limpiaron. El contador aparece en `/aurora status` como `leaks: niveles, entidades, chunks`.

### 6.4 Coste medido del LOD

Tanda A/B del 2026-10-30, Sodium en todas las corridas, render distance 16,
60 s de calentamiento y 30 s de medida. El detalle completo está en
[`RENDIMIENTO.md`](RENDIMIENTO.md) y los resultados crudos en
`bench/results/bench-ab-*.txt`.

| Modo | FPS medio | Quads | Diferencia vs Sodium-on |
|---|---:|---:|---:|
| Sodium-on (sin LOD) | 743.7 | 0 | — |
| LOD capturado | 727.4 | 1.498 | -2.2% |
| LOD worldgen agresivo (20/8/60k) | 349.8 | 23.779 | -53.0% |
| LOD worldgen conservador (18/4/12k) | 596.8 | 11.031 | -19.8% |
| Sodium-on control | 754.8 | 0 | +1.5% (deriva) |

Por ese coste, `lodWorldgen` queda **desactivado por defecto**. El LOD
capturado sigue disponible con `lodMode=aurora`; con `lodMode=auto` y Sodium
presente, Aurora cede el LOD.

## 7. Culling de entidades

### Escena de 1.350 cerdos

Función de datapack: `aurora:spawn_many_pigs` (1.350 cerdos, `NoAI`, `NoGravity`, `Silent`, `Invulnerable`), al este de `x=-600`, a `y=140`.

Conteo verificado:

```
Test passed. Count: 1350
#pigs has 1350 [aurora_count]
```

Captura histórica: `D:\OpenCode\heavy-entity-count.png` (no copiada a `bench/`).

### A/B con Sodium

| Escena | Culling | FPS medio | Mín | Máx | ms/frame |
|---|---:|---:|---:|---:|---:|
| 1.350 cerdos | ON | 923.6 | 745 | 958 | 1.08 |
| 1.350 cerdos | OFF | 756.9 | 538 | 816 | 1.32 |

Diferencia: **+22.0%** a favor de ON.

Capturas:

- `bench/screenshots/entity-bench-heavy-cull-on.png`: no se ven cerdos.
- `bench/screenshots/entity-bench-heavy-cull-off.png`: se ve la fila de cerdos en el cielo.

### Escena de 450 entidades

| Escena | Culling | FPS medio | Diferencia |
|---|---:|---:|---:|
| 225 cerdos + 225 gólems | ON | 737.8 | +0.7% (ruido) |
| 225 cerdos + 225 gólems | OFF | 732.4 | — |

Con 450 entidades la escena está limitada por terreno/GPU y la diferencia no es concluyente. Se reporta como resultado nulo, no como mejora.

## 8. Limpieza de la escena

```text
#pigs has 0 [aurora_count]
#golems has 0 [aurora_count]
```

Captura: `bench/screenshots/cleanup-entities.png`.

## 9. Limitaciones de la evidencia

- Las pruebas de cliente usan una cuenta de desarrollo sin sesión válida; los 401 de Mojang/Realms son esperables.
- `options.txt` tenía `maxFps:260`, pero las cifras de FPS lo superaron. Se usan solo para comparar configuraciones en el mismo equipo, no como FPS absolutos.
- Una corrida por configuración; la varianza min/max es amplia. No hay intervalo de confianza.
- El benchmark muestrea una vez por tick; para análisis fino hace falta Spark o un perfilador de GPU.
- Con `lodMode=aurora` forzado, el LOD con Sodium **sí se dibuja** (678 quads en el primer frame de la prueba de worldgen y píxeles de color en la captura), pero en la escena de colinas queda casi todo oculto tras el terreno/frustum. Con `lodMode=auto` Aurora cede y no captura LOD (`LOD=0 chunks/0 B`).
- El worldgen LOD solo funciona en singleplayer y usa el servidor integrado; no se probó en un servidor dedicado ni en multijugador (por diseño no se ejecuta allí).
- No se midió estabilidad de heap durante horas; la guardia de memoria acota caches, no demuestra ausencia de leaks de vanilla.
- Sodium y Voxy no se incluyen en el jar por licencia (Polyform Shield y All Rights Reserved).

## 10. Reproducir

```bat
cd D:\Cosas\AI\opencode\projects\aurora-optimizer
.\gradlew.bat clean build --no-daemon --console=plain
.\gradlew.bat test --no-daemon --console=plain
```

Los scripts de benchmark están copiados en `bench/scripts/`; los resultados
crudos, en `bench/results/`, y las capturas, en `bench/screenshots/`. Conservan
las rutas absolutas de la sesión original para no alterar lo que se ejecutó.
Sodium no se copia al proyecto por licencia: los scripts esperan el jar en
`D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar`.
