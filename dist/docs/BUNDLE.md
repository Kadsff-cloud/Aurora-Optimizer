# Bundle unificado 26.2

Aurora puede distribuirse de tres formas:

| Build | Comando | Contenido |
| --- | --- | --- |
| Unificado (por defecto) | `gradlew build` | Aurora + Lithium + FerriteCore + MoreCulling + Cloth Config + Rail Optimization + ImmediatelyFast + BadOptimizations + Clumps + Cubes Without Borders en un solo jar, como mods anidados de Fabric Jar-in-Jar. |
| Solo Aurora | `gradlew build -PcoreOnly` | Solo el codigo MIT de Aurora (`Aurora-0.1.0-core.jar`). No incluye terceros. |
| Perfil Sodium | `gradlew build -PsodiumProfile` | Igual que el unificado, pero `Aurora-0.1.0-sodium.jar` es cliente-only y declara `depends.sodium >= 0.9.2`. Sodium **no se empaqueta**. |

El build unificado **no fusiona codigo fuente**. Cada mod conserva su id,
sus mixins, su configuracion y su licencia; Fabric Loader los carga como mods
separados desde `META-INF/jars/`. Aurora actua como coordinador y evita
duplicar trabajo cuando detecta que el otro mod ya esta presente.

## Artefactos incluidos

Verificado contra la API de Modrinth el 2026-10-30.

| Mod | Version | Licencia | SHA-512 | Fuente |
| --- | --- | --- | --- | --- |
| Lithium | 0.25.3+mc26.2 | LGPL-3.0-only | `148b638f3c6229fbaf487120a2344a0af5e411a5aa6533d5db9d75da0a8c0d8304f63eb4cca13f4d03b2c9b4c23d559dd74c1d832422ef8a3087bd005e62a8bd` | https://github.com/CaffeineMC/lithium |
| FerriteCore | 9.0.0-fabric | MIT | `d81fa97e11784c19d42f89c2f433831d007603dd7193cee45fa177e4a6a9c52b384b198586e04a0f7f63cd996fed713322578bde9a8db57e1188854ae5cbe584` | https://github.com/malte0811/FerriteCore |
| MoreCulling | 1.8.1 | GPL-3.0-only | `9c331ba5f2ce9c322c41aa577f1029f3420d5507d0d942aadbb43fb9bd82e7da6f72e3a0b26c4ad96394669a99a7feb512b5119b3095fde2e0a437bf68c08b40` | https://github.com/FxMorin/MoreCulling |
| Cloth Config API | 26.2.155+fabric | LGPL-3.0-only | `37b1e402f0df5a383656e21a38ee18cdd15cb4ba3fb62fbeba82ef4b959a4479fc32718ac0d9d154a7d9104c5f7315bfa67dbeced0b8ff240b8039d4848d5df1` | https://github.com/shedaniel/cloth-config |
| Rail Optimization | 2.0+mc26.1.2 | GPL-3.0-only | `a0f7d92db42cfdb44cd815b82f8bbb68528b54531836b4ff27ac724f2ac3cc3ab51b5b5b9798c0ccd8d5aa03cbf386183d973d731d705d896d330bc3d618dc4e` | https://github.com/EasterGhost/RailOptimization |
| ImmediatelyFast | 1.16.5+26.2 | LGPL-3.0-or-later | `5836b089072c0fd796613d42c3bd01c7e401b6ad53de80328bb9d74d585de9be0ff0cfc4d6ffa1ed41008750573b0525eff8c064162a2f724ad16817a8fdaf22` | https://github.com/RaphiMC/ImmediatelyFast |
| BadOptimizations | 2.4.1 | MIT | `f91c409d4ce68d027ac0a11b6d2828c5f6d057e749ff0ce596f3a84a65fc1e3905b08f4d97a21a7302e2bfaf4ff865907272bd9c7ffa8487701b22e40dfe2f97` | https://github.com/ItsThosea/BadOpitmizations |
| Clumps | 26.2.1 | MIT | `a57044f0c9a07b19cd38b85528fd6b3958600c3ed1568d8a7ce32a7a473570362861ef433be4f23b02a43c05d658e9a5a71f9218fbdb963ae76a1714a0439fbe` | https://github.com/jaredlll08/Clumps |
| Cubes Without Borders | 5.0.0+26.2 | MIT | `ccd86e34dc252d59ec8ef5bbba0fb7f23f04facc4028e07b0bb6e8e0cf19b27c94a4c355886f9addd0a472ede0b21d19f95626fffb0b1ee4ce3d005b210e4cce` | https://github.com/Kira-NT/cubes-without-borders |

MoreCulling requiere Cloth Config; por eso Cloth Config tambien se incluye.
Rail Optimization declara compatibilidad con 26.2 en Modrinth aunque el nombre
del archivo diga `+mc26.1.2`; se prueba en el cliente antes de publicar.

## Que aporta cada uno

- **Lithium**: optimiza logica de juego (ticking, IA, fisica, allocations) en
  el servidor integrado. Complementa a Aurora, que trabaja sobre memoria,
  culling de entidades, LOD y extras de render.
- **FerriteCore**: reduce el uso de memoria de blockstates, modelos y datos de
  bloque. Es el complemento natural de la guardia de memoria de Aurora.
- **MoreCulling**: culling de bloques, marcos de item y otras geometrias.
  Aurora mantiene su culling de entidades por distancia, que MoreCulling no
  cubre.
- **Cloth Config**: libreria de configuracion de MoreCulling.
- **Rail Optimization**: acelera la actualizacion de railes powered/activator.
- **ImmediatelyFast**: reduce el coste de subida de buffers de HUD, texto y
  entidades. Trabaja sobre el envio a GPU, no sobre el terreno de Sodium.
- **BadOptimizations**: microoptimizaciones de cliente en allocaciones, luces,
  texturas y rutas calientes. No incluye un renderizador de terreno.
- **Clumps**: agrupa orbes de XP en el servidor integrado. Reduce entidades en
  granjas y complementa el culling de entidades de Aurora.
- **Cubes Without Borders**: permite pantalla completa sin bordes (borderless o
  windowed) y se configura desde Video Settings o `cwb.json`. Es una utilidad de
  ventana, **no una mejora de FPS**; se incluye porque es open source, cliente y
  compatible con 26.2/Sodium. Declara conflictos con otros mods de pantalla
  completa, ninguno presente en el bundle.

## Evaluados y descartados

- **MemoryLeakFix**: los leaks que corregia quedaron resueltos en vanilla
  desde 1.20.5. No hay build para 26.2 y no aporta nada sobre Aurora moderno.
- **Carpet Fixes**: ultimo build 1.20.4; requiere Carpet. No es viable en 26.2
  sin un port completo. Es una coleccion de fixes server-side, no un mod de
  rendimiento de cliente.
- **Blanket**: ultimo build 1.20.1. Varios de sus fixes ya estan en vanilla
  moderno. No hay build para 26.2.

## Verificar el jar

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z=[IO.Compression.ZipFile]::OpenRead('D:\Cosas\AI\opencode\projects\aurora-optimizer\build\libs\Aurora-0.1.0.jar')
$z.Entries | Where-Object { $_.FullName -like 'META-INF/jars/*' } | Select-Object FullName,Length
$z.Dispose()
```

Debe listar los nueve jars anidados. El log del cliente debe mostrar
`Bundle 26.2 -> Lithium=true | FerriteCore=true | MoreCulling=true |
ClothConfig=true | RailOptimization=true | ImmediatelyFast=true |
BadOptimizations=true | Clumps=true | CubesWithoutBorders=true`.

## Notas de compatibilidad

- Aurora no requiere Sodium, pero la combinacion recomendada sigue siendo
  **Sodium + Aurora + bundle**. Sodium hace el terreno base; Aurora aporta
  memoria, culling, LOD y extras; Lithium/FerriteCore/MoreCulling/Rail
  Optimization/ImmediatelyFast/BadOptimizations/Clumps cubren logica, memoria,
  bloques, railes, envio de buffers y entidades de XP; Cubes Without Borders
  aporta pantalla completa sin bordes, sin prometer FPS.
- Si Voxy o Sodium estan presentes y `lodMode=auto`, Aurora cede el LOD: a Voxy
  para no dibujar dos veces la misma geometria, y a Sodium por politica de coste
  (el LOD si se dibuja con Sodium, pero anade unos pocos FPS). `lodMode=aurora`
  lo fuerza para pruebas.
- Los overlays de culling pueden solaparse: MoreCulling culla bloques y Aurora
  culla entidades por distancia. No comparten el mismo objetivo.
- Cubes Without Borders es cliente-only y solo conflictua con otros mods de
  pantalla completa sin bordes; no compite con Sodium, Voxy ni Aurora.
- Si un mod del bundle se actualiza, hay que actualizar su version y hash en
  `build.gradle` y en este documento.

## Sodium y Voxy: por que no estan dentro

El jar unificado solo contiene mods **open source**. Sodium y Voxy se instalan
aparte por licencia y por dependencias:

| Mod | Licencia | Motivo para no incluirlo |
| --- | --- | --- |
| Sodium | Polyform Shield 1.0.0 | No es open source; su licencia restringe ciertos usos y redistribuciones. |
| Voxy | All Rights Reserved | No se puede redistribuir. |
| Sodium Extra | LGPL-3.0-only | Requiere Sodium; si se anida sin Sodium, Fabric Loader no carga el bundle. |
| Better Block Entities | LGPL-3.0-or-later | Requiere Sodium; mismo problema. |

Aurora implementa extras propios equivalentes en parte a Sodium Extra
(altura/desactivacion de nubes, niebla, limite de particulas y culling de
entidades) y cede el LOD a Voxy o Sodium con `lodMode=auto`. Para el paquete completo,
instala aparte:

```
Sodium + Sodium Extra + Better Block Entities + Voxy (opcional)
+ Aurora-0.1.0.jar (bundle open source)
```

No instales Lithium, FerriteCore, MoreCulling, Cloth Config, Rail Optimization,
ImmediatelyFast, BadOptimizations, Clumps ni Cubes Without Borders por separado
si usas el jar unificado.
