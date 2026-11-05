# Compatibilidad

Aurora detecta mods por id con `FabricLoader.isModLoaded`. El jar común no
declara dependencias obligatorias; el perfil `-PsodiumProfile` sí exige Sodium
(ver abajo).

| Mod | Id detectado | Comportamiento |
|---|---|---|
| Sodium | `sodium`, `sodium-fabric` | Aurora deja el terreno base a Sodium. El LOD propio **sí se dibuja** con Sodium (verificado: 678 quads en el primer frame), pero `lodMode=auto` lo cede por política de coste. Memoria, culling y extras siguen activos. |
| Sodium Extra | `sodium-extra` | Solo se detecta; Aurora aporta sus propios extras (nubes/niebla) sin tocar las opciones de Sodium Extra. |
| Iris | `iris` | Se detecta y se registra. Aurora no inyecta shaders. |
| Voxy | `voxy` | Con `lodMode=auto`, Aurora cede el LOD a Voxy para no duplicar geometría. |
| Cubes Without Borders | `cwb` | Se detecta y se registra. Es una utilidad de pantalla completa sin bordes; no toca el render de terreno. Solo conflictúa con otros mods de pantalla completa, ninguno incluido. |

## Perfil Sodium

El build por defecto no exige Sodium. El comando:

```bat
.\gradlew.bat build -PsodiumProfile
```

genera `Aurora-0.1.0-sodium.jar`, un artefacto **solo cliente** con
`depends.sodium >= 0.9.2` en `fabric.mod.json`. Sodium **no se empaqueta**
(Polyform Shield 1.0.0): el usuario lo instala aparte. El jar común y el core
siguen funcionando sin Sodium y en servidor dedicado.

## Worldgen LOD

`lodWorldgen=true` solo actúa en **singleplayer**, porque necesita el
`ChunkGenerator` y el `RandomState` del servidor integrado. En multijugador se
ignora y queda la captura de chunks cargados. No envía paquetes ni genera
terreno en el servidor remoto.

## Reglas prácticas

1. **Sodium + Aurora**: configuración recomendada. Sodium optimiza chunks; Aurora
   añade memoria, culling y extras. Deja `lodMode=auto`: Aurora cede el LOD a
   Sodium para no añadir coste a quien busca el máximo FPS de terreno. Usa
   `lodMode=aurora` si quieres ver el LOD propio con Sodium; se dibuja, pero
   cuesta unos pocos FPS y en escenas con colinas puede quedar casi oculto.
2. **Voxy + Aurora**: deja `lodMode=auto`. Si fuerzas `aurora`, verás dos LOD.
3. **Solo Aurora**: funciona, pero el terreno base sigue siendo el de vanilla.
   Con `lodWorldgen=true` puedes rellenar el horizonte en singleplayer sin Voxy.
4. **Servidor**: el jar se puede cargar en un servidor dedicado; los mixins son
   de cliente y no se aplican allí. El worldgen LOD no se ejecuta en servidor
   remoto.

## Lo que Aurora no hace

- No reemplaza el renderer de terreno de Sodium.
- No implementa shaders ni cambios de pipeline de GPU.
- No garantiza compatibilidad binaria con mods que mezclen las mismas clases;
  los mixins están limitados a puntos concretos y se registran en el log.
- No oculta el hecho de que dos mods de LOD pueden duplicar geometría si se
  fuerzan modos incompatibles.
- No genera LOD en multijugador ni envía datos de terreno al servidor.

## Verificar

```
/aurora compat
/aurora status
```

El log de inicio muestra:

```
Compatibilidad detectada -> Sodium: ..., Sodium Extra: ..., Iris: ..., Voxy: ...
```
