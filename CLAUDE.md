# Mod de renders isométricos de estructuras — Minecraft 1.12.2 (LiteLoader)

## Qué se construye
Un mod de cliente para Minecraft 1.12.2, cargado con LiteLoader y entregado como un único `.litemod`. El jugador marca el punto 1 y el punto 2 en el mundo; el mod genera un PNG isométrico, con fondo transparente, de la estructura de esa región. Un menú interactivo con vista previa ajusta rotación, elevación, zoom, desplazamiento y resolución.

## Documentos (fuente de verdad)
- `docs/ERS.md`: **qué** se construye (requisitos RF/RNF/RN con criterios de aceptación, preguntas abiertas, conflictos).
- `docs/PLAN.md`: **cómo y cuándo** (tareas T-0xx, dependencias, hitos, pruebas).

## Reglas no negociables
- Minecraft 1.12.2 y **LiteLoader obligatorio**. No usar Forge.
- Java 8, mappings MCP, OpenGL de función fija (LWJGL 2). Sin shaders modernos.
- Entrega: un solo `.litemod` con versión y licencia declaradas.
- El mod es de solo lectura: **nunca modifica bloques ni entidades del mundo**.
- No copiar código ni recursos de isometric-renders (gliscowo) sin verificar y cumplir su licencia.
- Un fallo de selección, render o menú **no debe cerrar el juego**: se informa en el chat y se restaura el estado gráfico.
- Código organizado por paquetes (selección, render, menú, configuración), con comentarios en los puntos de render. Textos al usuario en español.
- Alcance v1: ver ERS §3. Fuera: WebSocket, atlas, mobs/jugadores/ítems en el suelo, regiones mayores de 64×64×64, otros loaders, lote. Opcionales E10 pedidos por el solicitante el 2026-11-14 y ya dentro del alcance: marcos de objetos, pinturas y armor stands (T-044), corte por altura (T-043) e ítem por ID o en la mano (T-042).

## Cómo trabajar
1. Una tarea del plan a la vez, en orden y respetando dependencias.
2. Lee los requisitos de la tarea y sus criterios Dado/Cuando/Entonces antes de escribir código.
3. No inventes requisitos ni amplíes el alcance. Si falta una decisión, marca `[SUPUESTO]` y pregunta al solicitante; no resuelvas preguntas abiertas (ERS §13) en silencio.
4. Escribe pruebas unitarias para la lógica pura: cálculo de región, validaciones, límites de cámara, nombres de archivo, configuración.
5. Al cerrar una tarea: commit pequeño con el ID (`T-017: ...`) y un resumen con requisitos cubiertos.
6. Tareas con 👁 en `docs/PLAN.md` (render visual, menú, carga en juego): entrega al solicitante la lista exacta de qué comprobar en el juego y espera su aprobación antes de continuar con las dependientes. No declares «funciona» algo que solo se ve en el juego sin esa verificación.

## Comandos de build y ejecución
Gradle 4.10.3 (wrapper) con JDK 8 fijado en `gradle.properties`; plugin `com.anatawa12.forge:ForgeGradle:2.3-1.0.+` (liteloader), MCP `snapshot_20170624`, LiteLoader `1.12.2-SNAPSHOT`, JUnit 4.12.
- Preparar entorno (una vez): `./gradlew setupDecompWorkspace`
- Build + pruebas: `./gradlew build` → `build/libs/mod-isorender-0.1.0-mc1.12.2.litemod`
- Solo pruebas: `./gradlew test`; una clase: `./gradlew test --tests com.judamar.isorender.SmokeTest`
- Paquetes: `com.judamar.isorender.{seleccion,render,menu,configuracion}`; `litemod.json` es manual en `src/main/resources`.
- Con caché limpia, `setupDecompWorkspace` falla con `ZipException: invalid entry compressed size` (JDK 8u512): hay que re-empaquetar los `*-sources.jar` de `~/.gradle/caches/modules-2` sin compresión (STORED) y borrar `~/.gradle/caches/minecraft/deobfedDeps/*sources*`. Documentar en T-007.

## Estado del proyecto
- Decidido: enfoque en estructuras con menú interactivo; LiteLoader obligatorio; ejecuta Claude Code con el solicitante como supervisor y probador.
- Pendiente del solicitante: P-06 (fecha y presupuesto). P-08, P-09, P-12 y P-13 (selección, tamaño máximo, vista previa, ajustes del menú) antes de la épica E3.
- Licencia decidida: MIT (2026-11-14). El solicitante pidió (2026-11-14) implementar las tareas opcionales T-042, T-043 y T-044.
- Supuestos activos: comandos `/isorender pos1|pos2|clear|menu|render`, tecla K, región máxima 64³, resolución 512–4096 px, imagen cuadrada, cámara isométrica por defecto (rotación 45°, elevación 35,264°).

## Definición de terminado
Compila con JDK 8; pasan los casos de prueba de los requisitos Must; sin defectos críticos ni altos; probado en cliente vanilla 1.12.2 con LiteLoader limpio; documentación al día; aprobado por el solicitante.
