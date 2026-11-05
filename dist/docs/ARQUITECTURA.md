# Arquitectura

## Principio

Aurora separa **datos** de **referencias vivas**. La cache LOD guarda únicamente
primitivos (alturas `short[]`, colores `int[]`), nunca `LevelChunk`, `ClientLevel`
ni entidades. Esa decisión es la que evita que una cache de LOD se convierta en
una fuga de RAM: el peor caso está acotado por `lodMaxChunks` y por el tamaño de
cada instantánea.

## Entrypoints

- `dev.aurora.optimizer.Aurora` (`main`): carga configuración y addons comunes.
  También permite que un servidor dedicado cargue el mismo jar. `AuroraCompat`
  detecta Sodium, Voxy, Iris y los nueve mods del bundle (incluido `cwb`) sin
  declararlos dependencias duras.
- `dev.aurora.optimizer.client.AuroraClient` (`client`): registra tick, render,
  comandos y ciclo de vida del cliente.

## Flujo por tick

```
ClientTickEvents.END_CLIENT_TICK
├─ LodManager.onClientTick        -> captura en anillo 12 chunks cada 10 ticks
├─ WorldgenLodGenerator.onClientTick -> LOD por delante (solo singleplayer)
├─ MemoryGuard.onClientTick       -> cada trimIntervalTicks recorta cache LOD
└─ AuroraAddons.tickClient        -> addons de cliente
```

`WorldgenLodGenerator` ejecuta el muestreo en el hilo del servidor integrado y
entrega lotes al cliente; nunca toca el render thread durante la generación.

## Flujo de render

```
LevelRenderEvents.START_MAIN      -> Extras (nubes/niebla)
LevelRenderEvents.COLLECT_SUBMITS -> LodRenderer + addons
```

`LodRenderer` hace una sola submission por frame con `RenderTypes.debugQuads()`.
No crea buffers por chunk ni guarda estado de GPU entre frames.

## Mixins

| Mixin | Objetivo | Motivo |
|---|---|---|
| `ClientLevelMixin` | `ClientLevel.<init>`, `unload`, `disconnect` | Capturar chunks antes de descargarlos, limpiar caches y ejecutar `LevelLeakGuard` al salir |
| `ParticleEngineMixin` | `ParticleEngine.<init>`, `add`, `createTrackingEmitter` | Presupuesto global de partículas y tope de emisores |
| `EntityRenderDispatcherMixin` | `EntityRenderDispatcher.<init>`, `shouldRender` | Culling por distancia antes del frustum test |

`HookProbe` registra una sola vez cada hook activo. Se puede ver en
`/aurora status` y en el log como `Mixin activo: ...`.

## Seguridad

- Los addons se ejecutan dentro de `try/catch`: un addon roto no tumba el cliente.
- La captura de LOD también está protegida: un chunk raro se descarta.
- La configuración se sanea al cargar y al guardar.
