# Evidencia de benchmark

Esta carpeta conserva los scripts, configuraciones, resultados crudos y capturas
de la tanda A/B del **2026-10-30**, para que la evidencia no dependa de archivos
sueltos fuera del proyecto.

## Contenido

| Carpeta | Qué contiene |
| --- | --- |
| `scripts/` | Scripts PowerShell usados para lanzar el cliente, fijar la escena, medir y capturar. |
| `config/` | Configuraciones `aurora.json` de cada modo (`off`, `on`, LOD capturado, worldgen agresivo y conservador). |
| `results/` | Salida cruda de cada corrida (`LABEL`, `RESULT`, `STATUS`, `SCREENSHOT`). |
| `screenshots/` | Capturas de las corridas y de las escenas de entidades. |

Los scripts conservan las rutas absolutas de la sesión original
(`D:\OpenCode` para scripts/configs y `D:\Cosas\AI\aurora-optimizer` para el
proyecto). Son evidencia histórica reproducible con ajustes de ruta; no se
reescribieron para no alterar lo que se ejecutó.

## Resultado principal

| Corrida | FPS medio | Quads LOD | Nota |
| --- | ---: | ---: | --- |
| `aurora-on` | 708.5 | 0 | Aurora sin Sodium |
| `sodium-off` | 753.2 | 0 | Sodium sin Aurora |
| `sodium-on` | 743.7 | 0 | Sodium + Aurora |
| `sodium-lod-captured` | 727.4 | 1.498 | LOD capturado |
| `sodium-lod-worldgen` | 349.8 | 23.779 | Worldgen agresivo (20/8/60k) |
| `sodium-lod-worldgen-conservative` | 596.8 | 11.031 | Worldgen conservador (18/4/12k) |
| `sodium-on-control` | 754.8 | 0 | Control de deriva: +1.5% |

Lectura: Sodium sigue siendo ~5–6.5% más rápido que Aurora-only en terreno; el
LOD capturado cuesta ~2–3.6%; el worldgen conservador ~20% y el agresivo ~53%.
Por eso `lodWorldgen=false` es el default.

## Sodium

Sodium **no se copia** al proyecto: su licencia Polyform Shield 1.0.0 no permite
redistribuirlo como parte de este paquete. Los scripts esperan el jar en
`D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar`. Para reproducir, descárgalo de
Modrinth y colócalo en esa ruta (o ajusta la variable `$sodium` del script).

## Reproducir

```powershell
cd D:\Cosas\AI\opencode\projects\aurora-optimizer
.\gradlew.bat build --no-daemon --console=plain
powershell -NoProfile -ExecutionPolicy Bypass -File .\bench\scripts\bench-client.ps1 `
  -Label mi-corrida -Mode sodium-on -Seconds 30 -Warmup 60
```

El script escribe el resultado en `D:\OpenCode\bench-mi-corrida.txt` y la captura
en `D:\OpenCode\bench-mi-corrida.png`, igual que en la tanda original.
