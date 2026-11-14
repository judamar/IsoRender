# Registro de licencias (T-032, RNF-007, RNF-008)

Fecha de la revisión: 2026-11-14. Versión revisada: 0.1.0 (commit posterior a `3b6d798`).

## Licencia del mod
MIT, © 2026 judamar. Archivo `LICENSE` en la raíz del repositorio y dentro del `.litemod` (lo comprueba la tarea Gradle `validarLitemod`). El `litemod.json` declara nombre, versión 0.1.0, mcversion 1.12.2 y «Licencia MIT».

## Mod original (isometric-renders, de gliscowo): RNF-008
**No se usa código ni recursos del original.** Comprobado:
- Búsqueda en `src/`, `build.gradle`, `settings.gradle` y `gradle.properties` de «gliscowo», «isometric-renders», «io.wispforest», «com.glisco» y «owo»: sin coincidencias.
- El original solo existe para 1.18/1.19 (ERS §1) y no se consultó su código fuente: el motor es propio (framebuffer, cámara ortográfica/perspectiva con `glFrustum`, display lists, ordenación de translúcidas, conversión a PNG).
- El `.litemod` contiene únicamente clases de `com.judamar.isorender`, `litemod.json` y `LICENSE`: ninguna textura, modelo, fuente ni otro recurso de terceros.
- Autoría del repositorio: 30 commits, todos del mismo autor (con asistencia de Claude, indicada en cada commit).

Por tanto no hay obligaciones de licencia derivadas del original. Si en el futuro se incorporara algo suyo, hay que verificar su licencia antes y registrarlo aquí.

## Dependencias (no se redistribuyen en el paquete)
| Componente | Uso | Distribución |
|---|---|---|
| Minecraft 1.12.2 (Mojang) | API de cliente, solo para compilar y ejecutar | No incluido; el jugador ya lo tiene |
| LiteLoader 1.12.2 | Cargador del mod, solo para compilar | No incluido |
| MCP `snapshot_20170624` (mappings) | Nombres de desarrollo (ForgeGradle) | No incluido; el `.litemod` se reofusca |
| JUnit 4.12 (EPL-1.0) | Solo pruebas | No incluido |
| ForgeGradle (plugin de build) | Solo herramienta de compilación | No incluido |

Las texturas de bloques y modelos que aparecen en los PNG proceden del propio cliente de Minecraft del jugador en tiempo de ejecución (el mod no las copia ni las empaqueta). Los PNG generados son obra del usuario a partir de su propio mundo.

## Pendiente
- Confirmación del solicitante de esta revisión (T-032 es de responsabilidad compartida): ver el mensaje de cierre de la tarea.
