# Avisos de terceros - Aurora Optimizer

Aurora Optimizer (codigo propio) esta licenciado bajo MIT; consulta `LICENSE`.
El jar unificado (el que se genera por defecto) incluye, como artefactos
anidados de Fabric Jar-in-Jar, los mods originales sin modificar. Cada uno
conserva su id, su codigo y su licencia. Aurora **no fusiona el codigo fuente**
de estos mods: los empaqueta y coordina para que funcionen juntos.

## Incluidos en el jar unificado

| Mod | Version | Licencia | Fuente | Rol |
| --- | --- | --- | --- | --- |
| Lithium | 0.25.3+mc26.2 | LGPL-3.0-only | https://github.com/CaffeineMC/lithium | Optimizacion de logica de juego: ticking, IA, fisica y allocations del servidor integrado. |
| FerriteCore | 9.0.0-fabric | MIT | https://github.com/malte0811/FerriteCore | Reduccion de memoria en caches de blockstates, modelos y datos de bloque. |
| MoreCulling | 1.8.1 | GPL-3.0-only | https://github.com/FxMorin/MoreCulling | Culling de bloques, marcos de item y otras geometrias que vanilla dibuja de mas. |
| Cloth Config API | 26.2.155+fabric | LGPL-3.0-only | https://github.com/shedaniel/cloth-config | Libreria de configuracion requerida por MoreCulling. |
| Rail Optimization | 2.0+mc26.1.2 | GPL-3.0-only | https://github.com/EasterGhost/RailOptimization | Actualizacion mas rapida de railes powered/activator; fork de FxMorin/RailOptimization. |
| ImmediatelyFast | 1.16.5+26.2 | LGPL-3.0-or-later | https://github.com/RaphiMC/ImmediatelyFast | Envio inmediato de buffers de HUD, texto y entidades; reduce el coste de subida a GPU. |
| BadOptimizations | 2.4.1 | MIT | https://github.com/ItsThosea/BadOpitmizations | Microoptimizaciones de cliente: allocaciones, luces, texturas y rutas calientes. |
| Clumps | 26.2.1 | MIT | https://github.com/jaredlll08/Clumps | Agrupa orbes de XP en el servidor integrado para reducir entidades. |
| Cubes Without Borders | 5.0.0+26.2 | MIT | https://github.com/Kira-NT/cubes-without-borders | Pantalla completa sin bordes configurable (cliente). No promete FPS; es una utilidad de ventana. |

Textos completos: `LICENSES/` y, dentro del jar, `META-INF/licenses/`.

## Evaluados pero no incluidos

| Mod | Ultima version | Licencia | Motivo |
| --- | --- | --- | --- |
| MemoryLeakFix | 1.1.5 | LGPL-2.1-only | Sin build para 26.2. Los leaks que corregia quedaron resueltos en vanilla desde 1.20.5. La guardia de memoria de Aurora cubre sus propias caches. |
| Carpet Fixes | 1.17.0 | MIT | Sin build para 26.2 (ultimo: 1.20.4) y requiere Carpet. Es una coleccion de fixes server-side; no se puede cargar en 26.2 sin un port completo. |
| Blanket | 1.1.4 | MIT | Sin build para 26.2 (ultimo: 1.20.1). Varios de sus fixes de cliente ya estan en versiones modernas. |

Se conservan sus licencias en `LICENSES/` como referencia y para dejar claro
que fueron evaluados.

## Notas de distribucion y buena fe

- Los jars anidados son copias sin modificar descargadas de Modrinth. Sus
  hashes SHA-512 estan documentados en `docs/BUNDLE.md`.
- Aurora no fusiona codigo: el build unificado empaqueta los jars originales
  como mods separados de Fabric. Si eres autor y prefieres que se retire el
  bundle, abre un issue.
- Lithium, Cloth Config e ImmediatelyFast son LGPL-3.0: se distribuyen sin
  modificar y con enlace a su codigo fuente y texto de licencia.
- FerriteCore, BadOptimizations, Clumps y Cubes Without Borders son MIT: se
  conserva su aviso de copyright.
- Cubes Without Borders es cliente-only y declara conflictos con otros mods de
  pantalla completa sin bordes (Borderless Mining, FullscreenFix, etc.). Ninguno
  de esos mods esta en el bundle, asi que no hay conflicto interno.
- MoreCulling y Rail Optimization son GPL-3.0: se distribuyen sin modificar,
  con enlace a su fuente y texto de licencia. El conjunto no cambia sus
  terminos.
- Aurora puede compilarse sin el bundle con `gradlew build -PcoreOnly`; ese jar
  solo contiene el codigo MIT de Aurora.
- El perfil `gradlew build -PsodiumProfile` genera un jar cliente-only que
  declara `depends.sodium >= 0.9.2`, pero **no empaqueta Sodium**. Sodium sigue
  bajo Polyform Shield 1.0.0 y se instala aparte; el perfil solo expresa la
  dependencia para evitar arranques sin el renderer de terreno.

## Verificacion

Las versiones y hashes se verificaron contra la API de Modrinth el
2026-10-30. Para reproducir la descarga:

```powershell
.\gradlew.bat dependencies --configuration modRuntimeClasspath
```

El detalle de cada artefacto y su SHA-512 esta en `docs/BUNDLE.md`.
