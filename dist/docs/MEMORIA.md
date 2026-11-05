# Memoria y fugas

## Qué se acota

| Recurso | Límite | Dónde |
|---|---|---|
| Cache LOD | `lodMaxChunks` (4096 por defecto) | `LodCache` (LRU) |
| Partículas | `particleLimit` (4000 por defecto) | `ParticleEngineMixin` + `ParticleBudget` |
| Emisores de seguimiento | `trackingEmitterLimit` (256 por defecto) | `ParticleEngineMixin` |
| Entidades dibujadas | `entityCullDistance` (96 bloques) | `EntityRenderDispatcherMixin` |
| Tiempo entre limpiezas | `trimIntervalTicks` (1200 ticks) | `MemoryGuard` |
| Chunks soltados al salir | render distance + 4, máximo 64 de radio | `LevelLeakGuard` |

## Por qué no hay fuga en el LOD

`LodChunk.capture` lee altura y color y devuelve un objeto con:

- `int chunkX`, `int chunkZ`, `int cellSize`, `int cells`
- `short[] heights` (una altura por celda)
- `int[] colors` (un color por celda)

No guarda el `LevelChunk`, ni el `ClientLevel`, ni `BlockState`, ni entidades. Por
eso la cache puede crecer como mucho hasta `lodMaxChunks` entradas y su memoria
estimada es predecible:

```
64 bytes + celdas * 2 + celdas * 4
```

Con `cellSize=2` hay 64 celdas por chunk: 64 + 128 + 256 = **448 bytes** por chunk
(estimación; el objeto y los arrays tienen overhead de JVM). 4096 chunks ≈ 1,8 MB
de datos útiles, muy lejos de retener el mundo entero.

## Limpieza

1. **Periódica**: `MemoryGuard` llama a `LodManager.trim()` y a los addons.
2. **Al descargar un chunk**: `ClientLevelMixin.unload` captura el chunk antes de
   que desaparezca; la cache LRU decide si lo conserva.
3. **Al desconectar**: `ClientLevelMixin.disconnect` llama a
   `MemoryGuard.onDisconnect`, que vacía LOD y partículas y ejecuta
   `LevelLeakGuard` sobre el nivel que se está abandonando.
4. **Al cerrar el cliente**: `ClientLifecycleEvents.CLIENT_STOPPING` vacía la cache.
5. **Manual**: `/aurora trim`.

### LevelLeakGuard

Vanilla libera el `ClientLevel` entero cuando nadie lo referencia, pero
`ClientLevel.disconnect()` no vacía por sí solo entidades, tint caches, block
entities globales, progreso de destrucción ni la cache de chunks. Si un mod o
una referencia rezagada conserva el nivel, todo eso sigue vivo.

Al desconectar, `LevelLeakGuard`:

- marca como `DISCARDED` las entidades del nivel (excepto el jugador) y llama a
  `onClientRemoval`;
- limpia `clearTintCaches()`, `getGloballyRenderedBlockEntities()` y
  `destructionProgress()`;
- suelta los chunks de la cache con `getChunkSource().drop(...)` en un radio de
  `min(64, renderDistance + 4)`;
- registra el resultado en el log y en `/aurora status`.

Evidencia real de una salida al título:

```
Aurora leak guard: nivel descargado; entidades 1 -> 1 (removidas 0),
chunks soltados 1681, tint caches limpiadas
```

Solo había el jugador, por eso `removidas 0`; los 1.681 chunks sí se soltaron.
No sustituye al GC: elimina las referencias grandes para que un nivel retenido
no arrastre el mundo.

## Partículas

`ParticleEngineMixin` cuenta las partículas de todos los grupos **más** la cola
`particlesToAdd` y los `trackingEmitters` antes de aceptar una nueva. Si el total
alcanza `particleLimit`, cancela el `add`. Además, limita los emisores de
seguimiento con `trackingEmitterLimit`.

En 26.2 `ParticleEngine.setLevel` ya llama a `clearParticles()`, que vacía
grupos, cola, emisores y contadores. El hueco real no era el cambio de nivel:
`add` encolaba en `particlesToAdd` sin mirar esa cola, y
`createTrackingEmitter` no tenía tope. El mixin cubre ambos.

Esto no “arregla” un leak de un mod externo que retenga partículas por su cuenta,
pero evita que el motor acumule objetos sin límite. Para diagnosticar un leak
concreto, usa `/aurora status`, un volcado de heap y Spark.

## Diagnóstico

```
/aurora status
```

Muestra heap usado/máximo, memoria directa, chunks LOD, RAM estimada del LOD,
quads del último frame, partículas filtradas, emisores filtrados, entidades
ocultas, chunks/muestras de worldgen, niveles limpiados, entidades liberadas,
chunks soltados y hooks de mixin activos.
