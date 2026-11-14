# Plan de trabajo — Mod de renders isométricos de estructuras (Minecraft 1.12.2, LiteLoader)

Versión v0.4 (adaptada para ejecución con Claude Code). Basado en el Plan de Trabajo v0.3.

# 0. Guía de ejecución para Claude Code

Este plan lo ejecuta Claude Code. Reglas de uso:

- Trabaja **una tarea T-0xx a la vez**, en orden, sin saltar dependencias (columna «Dep.» de la EDT).
- Antes de codificar, lee los requisitos de la tarea en `docs/ERS.md` y cumple sus criterios de aceptación (Dado/Cuando/Entonces).
- Una tarea termina cuando: compila, pasan las pruebas unitarias aplicables, hay commit con el ID de la tarea y se entregó un resumen con los requisitos cubiertos.
- Las tareas con 👁 tienen **puerta de verificación humana**: al terminar, Claude Code indica al solicitante qué comprobar en el juego y espera su aprobación antes de iniciar tareas que dependan de ella.
- No se resuelven en silencio preguntas abiertas ni se amplía el alcance: se marca `[SUPUESTO]` o se pregunta (ERS §13).

**Tareas con puerta de verificación humana 👁** (render visual, menú o carga en el juego): T-010, T-011, T-012, T-014, T-016, T-017, T-018, T-019, T-020, T-021, T-022, T-023, T-024, T-025, T-026, T-027, T-031, T-034, T-035, T-036, T-037, T-042, T-043, T-044 (opcionales E10).

**Tareas verificables de forma autónoma** (build, pruebas unitarias, revisión de código): T-004 a T-009, T-013, T-015, T-028 a T-030, T-032, T-033, T-038 a T-041.

**Pendiente del solicitante:** P-06 (fecha y presupuesto) y las preguntas P-08, P-09, P-12 y P-13 antes de iniciar E3.

# 1. Estrategia de entrega

Se propone una entrega **iterativa e incremental con MVP**, en sprints de 2 semanas, con validación del solicitante en hitos clave. El orden ataca primero el mayor riesgo: prototipo en LiteLoader y decisión go/no-go, luego selección y motor de render, luego el menú con vista previa (R-01, R-02, R-03 de la ERS).

**MVP (solo requisitos Must):** RF-001, RF-002, RF-003, RF-004, RF-005, RF-007, RF-009, RF-010, RF-011, RF-014, RF-016, RF-017, RF-018, RF-019, RF-020, RF-021, RF-024, RF-025, RF-027, con los RNF Must. Esfuerzo de construcción hasta el MVP (tareas marcadas en la EDT, sin pruebas formales, UAT ni documentación): **284,1 h**. Los Should (RF-006, RF-008, RF-012, RF-013, RF-022, RF-023, RF-026, RF-028, RF-029) completan la v1.

**Ejecutor asumido** *\[SUPUESTO\]* (P-06): Claude Code desarrolla; el solicitante revisa, prueba en el juego y aprueba las puertas de verificación. Las horas de este plan son **equivalentes humanos de referencia** para dimensionar el trabajo; no son el tiempo de Claude Code. El calendario real depende de la disponibilidad del solicitante para probar.

# 2. Desglose de trabajo (EDT)

Jerarquía: Épica → Tarea. Horas: O = optimista, P = probable, Pe = pesimista, E = esperado PERT. Las épicas E0 a E9 forman el alcance de la v1; E10 es opcional y se estima aparte.

| **ID** | **Tarea** | **Requisitos** | **Resp.** | **Dep.** | **O** | **P** | **Pe** | **E** |
|---|---|---|---|---|---|---|---|---|
| **E0** | **Gestión y levantamiento** | | | | **9** | **15** | **28** | **16,1** |
| T-001 | Sesión de aclaración con el solicitante y cierre de preguntas abiertas | Todos | Analista | — | 2 | 4 | 8 | 4,3 |
| T-002 | Validación de la ERS v0.3 y emisión de la v1.0 | Todos | Analista | T-001 | 2 | 3 | 6 | 3,3 |
| T-003 | Gestión y seguimiento del proyecto (reporte por sprint) | — | Analista | T-002 | 5 | 8 | 14 | 8,5 |
| **E1** | **Entorno y toolchain** | | | | **13** | **24** | **45** | **25,7** |
| T-004 | Montar JDK 8, Gradle y entorno con MCP 1.12.2 (ForgeGradle) | RNF-006 | Desarrollador | T-002 | 6 | 12 | 24 | 13,0 |
| T-005 | Instalar LiteLoader 1.12.2 y perfil de pruebas con cliente vanilla | RNF-001 | Desarrollador | T-004 | 2 | 4 | 8 | 4,3 |
| T-006 | Repositorio, estructura de paquetes y script de build reproducible | RNF-006 | Desarrollador | T-004 | 3 | 5 | 8 | 5,2 |
| T-007 | Documentar y fijar versiones del entorno | RNF-006 | Desarrollador | T-006 | 2 | 3 | 5 | 3,2 |
| **E2** | **Prototipo mínimo en LiteLoader** | | | | **17** | **34** | **68** | **36,9** |
| T-008 | Investigar API de LiteLoader 1.12.2 (descriptor, clase de entrada, tecla, comando, pantalla propia, hooks de render y de mundo) | RF-001, RF-003, RF-016 | Desarrollador | T-005 | 6 | 12 | 24 | 13,0 |
| T-009 | Prototipo «hola mundo»: carga como .litemod y escribe en el chat | RF-001, RF-002 | Desarrollador | T-008, T-006 | 4 | 8 | 16 | 8,7 |
| T-010 | Prototipo: tecla y comando abren una pantalla de prueba | RF-016 | Desarrollador | T-009 | 4 | 8 | 16 | 8,7 |
| T-011 | Hito go/no-go: validar render GL propio y lectura de bloques del mundo con LiteLoader | RF-001, RNF-001 | Desarrollador, Solicitante | T-010 | 3 | 6 | 12 | 6,5 |
| **E3** | **Selección de región** | | | | **14** | **28** | **54** | **30,0** |
| T-012 | Estado de selección: comandos pos1, pos2 y clear; cálculo y aviso de dimensiones | RF-003, RF-004, RF-005, RF-008 | Desarrollador | T-011 | 4 | 8 | 14 | 8,3 |
| T-013 | Validaciones de región (puntos, tamaño máximo, chunks cargados, solo aire) | RF-007, RNF-014 | Desarrollador | T-012 | 4 | 8 | 16 | 8,7 |
| T-014 | Contorno de la selección dibujado en el mundo | RF-006 | Desarrollador | T-013 | 6 | 12 | 24 | 13,0 |
| **E4** | **Motor de render de estructuras** | | | | **71** | **136** | **266** | **146,7** |
| T-015 | Framebuffer de alta resolución: creación, uso y liberación | RF-025, RNF-009 | Desarrollador | T-011 | 6 | 12 | 24 | 13,0 |
| T-016 | Cámara ortográfica parametrizable (rotación, elevación, zoom, desplazamiento) y encuadre automático | RF-010, RF-017, RF-018 | Desarrollador | T-015 | 10 | 18 | 32 | 19,0 |
| T-017 | Dibujar los bloques de la región con su estado real (BlockRendererDispatcher, eliminación de caras ocultas, iluminación) | RF-009, RF-014 | Desarrollador | T-016, T-013 | 16 | 30 | 60 | 32,7 |
| T-018 | Caché de geometría de la estructura para vista previa y render (listas de dibujo/VBO) | RNF-002, RNF-003 | Desarrollador | T-017 | 8 | 16 | 32 | 17,3 |
| T-019 | Lectura de píxeles, PNG con transparencia y antialiasing sin halos | RF-011, RF-024, RNF-004 | Desarrollador | T-017 | 10 | 18 | 34 | 19,3 |
| T-020 | Bloques transparentes, tintados y orden de dibujo (cristal, hojas, agua) | RF-012 | Desarrollador | T-019 | 8 | 16 | 32 | 17,3 |
| T-021 | Bloques con entidad de bloque (cofres, camas, carteles) | RF-013 | Desarrollador | T-019 | 8 | 16 | 32 | 17,3 |
| T-022 | Restauración del estado gráfico, manejo de errores y control de memoria | RNF-005, RNF-010, RNF-014 | Desarrollador | T-019 | 5 | 10 | 20 | 10,8 |
| **E5** | **Menú interactivo y salida** | | | | **38** | **68** | **126** | **72,8** |
| T-023 | Pantalla base del menú; apertura por tecla y comando | RF-016 | Desarrollador | T-011 | 6 | 10 | 18 | 10,7 |
| T-024 | Controles de rotación, elevación, zoom y desplazamiento; entrada numérica | RF-017, RF-018, RF-023, RNF-012 | Desarrollador | T-023, T-016 | 10 | 18 | 32 | 19,0 |
| T-025 | Vista previa en vivo dentro del menú (incluye selector de fondo y toggle de perspectiva, peticiones del 2026-11-14) | RF-019, RNF-003, RF-032, RF-033 | Desarrollador | T-024, T-018 | 10 | 20 | 40 | 21,7 |
| T-026 | Selector de resolución, botón Renderizar, restablecer y presets | RF-020, RF-021, RF-022 | Desarrollador | T-025, T-019 | 6 | 10 | 18 | 10,7 |
| T-027 | Mensajes de resultado y error en el chat; textos en español | RF-027, RNF-011 | Desarrollador | T-013, T-019 | 3 | 5 | 10 | 5,5 |
| T-028 | Nombres de archivo únicos y carpeta de salida | RF-024, RF-026 | Desarrollador | T-019 | 3 | 5 | 8 | 5,2 |
| **E6** | **Configuración** | | | | **7** | **14** | **28** | **15,2** |
| T-029 | Lectura y escritura del archivo de configuración y del último ajuste de cámara | RF-028, RF-029 | Desarrollador | T-024 | 4 | 8 | 16 | 8,7 |
| T-030 | Integrar la configuración en selección, render y menú | RF-028, RF-029 | Desarrollador | T-029 | 3 | 6 | 12 | 6,5 |
| **E7** | **Empaquetado y legal** | | | | **4** | **8** | **16** | **8,7** |
| T-031 | Empaquetado final del .litemod con versión y licencia | RF-002, RNF-007 | Desarrollador | T-026, T-030 | 3 | 6 | 12 | 6,5 |
| T-032 | Revisión de licencias (sin código del original; licencia propia) | RNF-008 | Desarrollador, Solicitante | T-031 | 1 | 2 | 4 | 2,2 |
| **E8** | **Pruebas y correcciones** | | | | **43** | **74** | **132** | **78,6** |
| T-033 | Diseño de casos de prueba y estructuras de referencia | Todos | Desarrollador | T-032 | 5 | 8 | 12 | 8,2 |
| T-034 | Pruebas funcionales (selección, render, menú, configuración) | RF-001 a RF-029 | Desarrollador | T-033 | 14 | 24 | 40 | 25,0 |
| T-035 | Pruebas de rendimiento, calidad de imagen, memoria y vista previa | RNF-002, RNF-003, RNF-004, RNF-009, RNF-014 | Desarrollador | T-034 | 8 | 14 | 26 | 15,0 |
| T-036 | Pruebas de compatibilidad, estabilidad, usabilidad del menú y no modificación del mundo | RNF-001, RNF-005, RNF-012, RNF-013 | Desarrollador | T-034 | 6 | 10 | 18 | 10,7 |
| T-037 | UAT con el solicitante y correcciones tras feedback | Todos | Solicitante, Desarrollador | T-035, T-036 | 10 | 18 | 36 | 19,7 |
| **E9** | **Documentación y cierre** | | | | **13** | **24** | **46** | **25,8** |
| T-038 | Manual de usuario (selección, menú, comandos, configuración) | RF-003, RF-016, RF-028 | Desarrollador | T-037 | 4 | 8 | 14 | 8,3 |
| T-039 | Documentación técnica (entorno, puntos de render, motor) | RNF-006 | Desarrollador | T-037 | 3 | 5 | 10 | 5,5 |
| T-040 | Release v1.0: etiqueta de versión, artefacto y notas de versión | RF-002 | Desarrollador | T-038, T-039 | 2 | 3 | 6 | 3,3 |
| T-041 | Soporte post-entrega (2 semanas, bolsa de horas) | — | Desarrollador | T-040 | 4 | 8 | 16 | 8,7 |
| | **Subtotal tareas (horas)** | | | | **229** | **425** | **809** | **456,5** |

Responsables: Analista, Desarrollador (único desarrollador), Solicitante. Los totales por épica son la suma de sus tareas.

## 2.1 Épica opcional E10

| **ID** | **Tarea** | **Requisitos** | **Dep.** | **O** | **P** | **Pe** | **E** |
|---|---|---|---|---|---|---|---|
| T-042 | Render de ítem en la mano y de ítem o bloque por ID | RF-030 | T-019 | 8 | 14 | 26 | 15,0 |
| T-043 | Corte por altura de la estructura | RF-015 | T-017 | 6 | 10 | 18 | 10,7 |
| T-044 | Entidades decorativas en el render (marcos de objetos, pinturas, armor stands) | RF-034 | T-021 | 8 | 14 | 26 | 15,0 |
| | **Total opcional (sin buffer)** | | | **22** | **38** | **70** | **40,7** |

T-042 (ítem en la mano o por ID) se implementó a petición del solicitante (2026-11-14) y lleva puerta 👁. T-043 (corte por altura) se implementó a petición del solicitante (2026-11-14) y lleva puerta 👁. Con 15 % de buffer: 46,8 h. T-044 (RF-034) la pidió el solicitante (2026-11-14) tras ver que el marco de objetos no aparece (fuera de la v1) y lleva puerta 👁. No está en el total general.

# 3. Estimación

Valor esperado PERT = (O + 4P + Pe) / 6, redondeado a una décima por tarea; los totales suman las tareas ya redondeadas. Incluye levantamiento, entorno, pruebas, correcciones tras feedback, documentación, despliegue y soporte.

| **Épica** | **O (h)** | **P (h)** | **Pe (h)** | **E (h)** |
|---|---|---|---|---|
| E0 Gestión y levantamiento | 9 | 15 | 28 | 16,1 |
| E1 Entorno y toolchain | 13 | 24 | 45 | 25,7 |
| E2 Prototipo mínimo en LiteLoader | 17 | 34 | 68 | 36,9 |
| E3 Selección de región | 14 | 28 | 54 | 30,0 |
| E4 Motor de render de estructuras | 71 | 136 | 266 | 146,7 |
| E5 Menú interactivo y salida | 38 | 68 | 126 | 72,8 |
| E6 Configuración | 7 | 14 | 28 | 15,2 |
| E7 Empaquetado y legal | 4 | 8 | 16 | 8,7 |
| E8 Pruebas y correcciones | 43 | 74 | 132 | 78,6 |
| E9 Documentación y cierre | 13 | 24 | 46 | 25,8 |
| **Total de tareas** | **229** | **425** | **809** | **456,5** |
| Buffer de contingencia (15 %) | | | | 68,4 |
| **Total general con buffer** | | | | **524,9** |

**Buffer del 15 %:** riesgo alto por LiteLoader poco documentado (R-01), rendimiento con regiones grandes (R-02), vista previa en vivo (R-03) y entidades de bloque (R-06). Equivale a 68,4 h, repartidas en los sprints.

**Nivel de confianza: bajo.** Depende de: que LiteLoader permita pantallas propias y render GL (go/no-go), del tamaño real de las estructuras (P-09), de la fluidez que se exija a la vista previa (P-12), del equipo asumido (P-06) y de la disponibilidad del solicitante para la UAT. A 20 h/semana el esfuerzo equivale a 27 semanas; a 30 h/semana, a 18 semanas.

**Cambio frente a la v0.1:** el reenfoque a estructuras con menú interactivo subió el esfuerzo de 363,3 h a 524,9 h. La confirmación de LiteLoader no cambia las horas.

# 4. Cronograma

Sprints de 2 semanas (40 h de capacidad: 34,7 h de tareas y el resto de buffer). Duración total: **14 sprints = 28 semanas**, del 16/11/2026 al 28/05/2027. Las tareas pueden abarcar dos sprints; se listan en el sprint donde terminan.

| **Sprint** | **Periodo** | **Objetivo** | **Tareas que terminan** | **Horas** | **Buffer** | **Hito** |
|---|---|---|---|---|---|---|
| S1 | 16/11/2026 – 27/11/2026 | Gestión y levantamiento; Entorno y toolchain | 001, 002, 003, 004, 005 | 34,7 | 5,2 | — |
| S2 | 30/11/2026 – 11/12/2026 | Entorno y toolchain; Prototipo mínimo en LiteLoader | 006, 007, 008, 009 | 34,7 | 5,2 | — |
| S3 | 14/12/2026 – 25/12/2026 | Prototipo mínimo en LiteLoader; Selección de región | 010, 011, 012, 013 | 34,7 | 5,2 | H1, H2 |
| S4 | 28/12/2026 – 08/01/2027 | Selección de región; Motor de render de estructuras | 014, 015 | 34,7 | 5,2 | H3 |
| S5 | 11/01/2027 – 22/01/2027 | Motor de render de estructuras | 016, 017 | 34,7 | 5,2 | — |
| S6 | 25/01/2027 – 05/02/2027 | Motor de render de estructuras | 018 | 34,7 | 5,2 | — |
| S7 | 08/02/2027 – 19/02/2027 | Motor de render de estructuras | 019, 020 | 34,7 | 5,2 | H4 |
| S8 | 22/02/2027 – 05/03/2027 | Motor de render de estructuras; Menú interactivo y salida | 021, 022, 023 | 34,7 | 5,2 | — |
| S9 | 08/03/2027 – 19/03/2027 | Menú interactivo y salida | 024, 025 | 34,7 | 5,2 | — |
| S10 | 22/03/2027 – 02/04/2027 | Menú interactivo y salida; Configuración | 026, 027, 028, 029, 030 | 34,7 | 5,2 | H5, H6 |
| S11 | 05/04/2027 – 16/04/2027 | Empaquetado y legal; Pruebas y correcciones | 031, 032, 033 | 34,7 | 5,2 | — |
| S12 | 19/04/2027 – 30/04/2027 | Pruebas y correcciones | 034, 035, 036 | 34,7 | 5,2 | H7 |
| S13 | 03/05/2027 – 14/05/2027 | Pruebas y correcciones; Documentación y cierre | 037, 038, 039, 040 | 34,7 | 5,2 | H8 |
| S14 | 17/05/2027 – 28/05/2027 | Documentación y cierre | 041 | 5,4 | 0,8 | — |
| **Total** | | | | **456,5** | **68,4** | |

«Tareas que terminan» lista el número de la tarea T-0xx. Total general (horas + buffer): 524,9 h.

# 5. Ruta crítica y dependencias

Con un solo desarrollador casi todo el trabajo es secuencial. La ruta crítica lógica es:

T-004 → T-005 → T-008 → T-009 → T-010 → T-011 → T-015 → T-016 → T-017 → T-018 → T-025 → T-026 → T-031 → T-032 → T-033 → T-034 → T-035/T-036 → T-037 → T-038/T-039 → T-040

**Dependencias del cliente** (con fecha límite sugerida):

| **Qué se necesita** | **De quién** | **Fecha límite sugerida** |
|---|---|---|
| Respuesta a P-06 (equipo, fecha, presupuesto) | Solicitante | Lo antes posible |
| Respuestas a P-08, P-09, P-12 y P-13 (selección, tamaño, vista previa, ajustes del menú) | Solicitante | Antes de iniciar E3 (sprint 3) |
| Respuestas a P-03, P-04 y P-14 (mods, ángulo y formato de imagen) | Solicitante | Antes de iniciar E4 (sprint 4) |
| Licencia deseada para el mod (P-05) | Solicitante | Antes de T-031 (sprint 11) |
| Validación de la ERS v0.2 y de los supuestos | Solicitante | Fin del sprint 1 |
| Mundos de prueba con estructuras de referencia (P-09) | Solicitante | Antes de T-017 (sprint 5) |
| Disponibilidad para la UAT (T-037) | Solicitante | Sprint 13 |
| Equipo de referencia para rendimiento (P-16) | Solicitante / desarrollador | Antes de T-035 |

# 6. Hitos y entregables

| **Hito** | **Descripción** | **Sprint** | **Criterio de aceptación** | **Aprueba** |
|---|---|---|---|---|
| H1 | Prototipo cargado en LiteLoader: tecla y comando abren una pantalla de prueba | S3 | El .litemod se carga y la tecla abre la pantalla de prueba. | Solicitante revisa el prototipo |
| H2 | Go/no-go técnico de LiteLoader | S3 | Evidencia de que LiteLoader permite render GL propio y lectura de bloques; decisión documentada. | Solicitante confirma continuar o ajustar el alcance |
| H3 | Selección de región operativa | S4 | Punto 1, punto 2, dimensiones, validaciones y contorno funcionan (RF-003 a RF-008). | Solicitante valida la selección |
| H4 | Estructura renderizada a PNG | S7 | Una estructura de referencia se exporta a PNG transparente con cámara por defecto (RF-009 a RF-011, RF-014, RF-024). | Solicitante valida la calidad visual |
| H5 | Menú con vista previa y botón Renderizar | S10 | El menú ajusta cámara con vista previa y exporta (RF-016 a RF-021). | Solicitante valida el menú |
| H6 | Funcionalidad completa de la v1 | S10 | RF-001 a RF-029 implementados. | — |
| H7 | Release candidate | S12 | Pruebas funcionales, de rendimiento, de compatibilidad y de estabilidad terminadas. | — |
| H8 | Entrega v1.0 | S13 | UAT aprobada y .litemod publicado. | Solicitante firma la aceptación |

# 7. Plan de pruebas y calidad

**Tipos de prueba:** unitarias (cálculo de región, validaciones, límites de cámara, nombres de archivo, lectura de configuración); de integración (selección y render reales hasta el PNG en un cliente); de rendimiento, calidad de imagen, vista previa y memoria; de compatibilidad (vanilla + LiteLoader sin Forge); de usabilidad del menú; de seguridad básica (sanear rutas y nombres, y comprobar que el mundo no se modifica); y UAT.

**Ejecuta:** el desarrollador (todas, salvo UAT) y el solicitante (UAT). **Entornos:** desarrollo (IDE con cliente de pruebas), pruebas (cliente vanilla 1.12.2 + LiteLoader limpio con el .litemod y mundos con estructuras de referencia) y UAT (el equipo del solicitante, si es posible).

**Criterios de salida:** 100 % de los casos Must pasan; al menos 90 % de los Should; cero defectos críticos o altos abiertos; RNF-002, RNF-003 y RNF-004 medidos y dentro del umbral; UAT aprobada.

| **Caso** | **Descripción** | **Requisitos** | **Tipo** |
|---|---|---|---|
| CP-01 | Carga del mod en cliente 1.12.2 con LiteLoader; versión incorrecta; otros mods | RF-001 | Funcional |
| CP-02 | Un solo .litemod con metadatos de versión y licencia | RF-002, RNF-007 | Funcional |
| CP-03 | Punto 1 y punto 2: apuntando a bloque, sin apuntar, reemplazo, punto 2 = punto 1 | RF-003, RF-004 | Funcional |
| CP-04 | Región: dimensiones correctas, orden inverso de los puntos | RF-005 | Funcional |
| CP-05 | Contorno de selección visible y limpieza de la selección | RF-006, RF-008 | Funcional |
| CP-06 | Validaciones: falta un punto, región mayor que 64³, chunks no cargados, solo aire | RF-007, RNF-014 | Funcional |
| CP-07 | Render de estructura de referencia: bloques correctos, orientaciones, puertas, vallas | RF-009, RF-014 | Funcional |
| CP-08 | Encuadre automático con rotaciones y elevaciones extremas y región alargada | RF-010 | Funcional |
| CP-09 | Alfa = 0 fuera de la silueta; alfa intermedio en cristal | RF-011 | Funcional |
| CP-10 | Cristal, hojas, agua y tintes por bioma | RF-012 | Funcional |
| CP-11 | Cofre, cama y cartel en la región; entidad que falla se omite con aviso | RF-013 | Funcional |
| CP-12 | Menú: abrir por tecla y comando; sin selección; segunda pantalla abierta | RF-016 | Funcional |
| CP-13 | Controles de rotación y elevación, con límites | RF-017 | Funcional |
| CP-14 | Controles de zoom y desplazamiento, con límites | RF-018 | Funcional |
| CP-15 | Vista previa en vivo: actualización, región inválida, región grande | RF-019, RNF-003 | Funcional / rendimiento |
| CP-16 | Resolución en el menú, botón Renderizar y doble clic | RF-020, RF-021, RF-025 | Funcional |
| CP-17 | Restablecer, presets y entrada numérica (válida e inválida) | RF-022, RF-023 | Funcional |
| CP-18 | PNG en carpeta: permisos, carpeta inexistente, nombres únicos | RF-024, RF-026 | Funcional |
| CP-19 | Mensajes de chat de éxito y de error, en español | RF-027, RNF-011 | Funcional |
| CP-20 | Configuración: válida, corrupta, ausente; último ajuste de cámara | RF-028, RF-029 | Funcional |
| CP-21 | Compatibilidad en cliente vanilla 1.12.2 + LiteLoader sin Forge | RNF-001 | Compatibilidad |
| CP-22 | 5 renders de 32³ a 2048 px: promedio ≤ 10 s, máximo ≤ 15 s | RNF-002 | Rendimiento |
| CP-23 | Bordes sobre fondo blanco y negro al 400 %, sin halos | RNF-004 | Calidad |
| CP-24 | 30 fallos forzados consecutivos sin cierre del juego | RNF-005 | Estabilidad |
| CP-25 | Compilación con JDK 8 y revisión de estructura y comentarios | RNF-006 | Revisión de código |
| CP-26 | Revisión legal de licencias | RNF-008 | Revisión |
| CP-27 | 30 renders y 30 aperturas del menú: memoria y VRAM estables | RNF-009 | Rendimiento |
| CP-28 | Fotograma posterior al render y al cerrar el menú idéntico al anterior | RNF-010 | Funcional |
| CP-29 | Menú en 854×480 y en 1920×1080: controles visibles y sin superposición | RNF-012 | Usabilidad |
| CP-30 | El mundo no cambia tras 20 selecciones y renders | RNF-013 | Funcional |
| CP-31 | Render de ítem/bloque por ID (solo si se hace T-042) | RF-030 | Funcional (opcional) |
| CP-32 | Corte por altura desde el menú (T-043) | RF-015 | Funcional (opcional) |
| CP-33 | Marcos, pinturas y armor stands en el render (T-044) | RF-034 | Funcional (opcional) |

Cada requisito de la ERS tiene al menos un caso, salvo RF-031 (Won't en la v1).

# 8. Despliegue y puesta en producción

- **Entornos:** desarrollo, pruebas y release (carpeta de mods del usuario).

- **Estrategia de release:** versionado semántico; la v1.0.0 se marca con etiqueta en Git y se publica como un único .litemod con notas de versión.

- **Rollback:** el usuario conserva el .litemod anterior y lo restituye en la carpeta de mods; la configuración incluye versión de formato para no romper archivos antiguos.

- **Backups:** repositorio Git remoto con código y scripts de build; copia del .litemod publicado.

- **Migración:** no hay datos que migrar.

**Lista de verificación de salida:**

- Casos Must pasados y criterios de salida cumplidos.

- Probado en cliente vanilla 1.12.2 con LiteLoader limpio y con estructuras de referencia.

- .litemod con versión y licencia; revisión de licencias hecha (T-032).

- Manual de usuario y documentación técnica entregados.

- Aceptación de la UAT registrada.

# 9. Gestión de cambios y comunicación

- **Cambios de alcance:** todo cambio pasa por el analista, que estima su impacto en horas. Entran sin costo solo los que caben en el buffer; el resto se cotiza aparte o pasa a fase 2. El reenfoque a estructuras de esta versión es un ejemplo: sumó 161,6 h.

- **Fuera de la v1 por defecto:** WebSocket, atlas, entidades, regiones mayores de 64³, otros loaders, lote.

- **Comunicación:** reporte corto al final de cada sprint *\[SUPUESTO\]* y revisión con el solicitante en los hitos H2 a H5 y H8.

- **Canal:** el que el solicitante indique (P-06); registro de decisiones en el repositorio.

# 10. Riesgos del plan y contingencia

| **ID** | **Riesgo** | **Prob.** | **Impacto** | **Mitigación / contingencia** |
|---|---|---|---|---|
| RP-01 | LiteLoader no permite pantalla propia o el render requerido. | Media | Alto | Go/no-go en S3; si falla, escalar al solicitante para reducir alcance; Forge está descartado. |
| RP-02 | Respuestas tardías del solicitante (P-06, P-08, P-09). | Media | Alto | Avanzar con supuestos explícitos; fechas límite en la sección 5. |
| RP-03 | El motor de render para regiones grandes cuesta más de lo estimado (T-017, T-018). | Alta | Alto | Buffer; reducir el máximo a 32³ y diferir la caché si hace falta. |
| RP-04 | La vista previa en vivo no es fluida. | Media | Medio | Vista previa simplificada o botón «Actualizar» (P-12). |
| RP-05 | Entidades de bloque (T-021) más difíciles de lo previsto. | Alta | Medio | Prioridad Should; omitir con aviso y pasar a fase 2. |
| RP-06 | Halos o transparencia incorrecta en el framebuffer. | Media | Medio | Prueba temprana en T-019; probar supersampling. |
| RP-07 | Toolchain antigua que no se monta. | Media | Medio | T-004 con rango pesimista de 24 h; documentar versiones (T-007). |
| RP-08 | Poca disponibilidad del solicitante para probar en el juego (puertas 👁). | Media | Alto | Agrupar verificaciones por hito; avanzar con tareas autónomas mientras espera aprobación. |
| RP-09 | Más cambios de alcance durante el desarrollo. | Media | Alto | Proceso de cambios de la sección 9; congelar la ERS v1.0 tras T-002. |

# 11. Resumen para el cliente

**Qué se entrega:** un mod para Minecraft 1.12.2 (archivo .litemod para LiteLoader). Marcas dos puntos en el mundo y el mod genera una imagen PNG isométrica, con fondo transparente, de la estructura que hay entre ellos. Un menú con vista previa te deja ajustar cámara, ángulo, zoom y resolución (512 a 4096 px) antes de exportar.

**Cuánto tiempo:** el cronograma de la sección 4 es una referencia en equivalentes humanos (524,9 horas con margen, unas 28 semanas a 20 h/semana). Con Claude Code el plazo real lo marca la rapidez con que el solicitante apruebe las puertas de verificación en el juego.

**Fases (semanas, aproximadas y superpuestas):** entorno y prototipo (semanas 1 a 6, con validación técnica de LiteLoader), selección de región (5 a 8), motor de render (7 a 16), menú interactivo (15 a 20), configuración y empaquetado, pruebas y entrega (21 a 26).

**Qué necesitamos de ti:** confirmar equipo y fechas, contestar cómo quieres seleccionar los puntos y el tamaño de estructuras esperado, validar los supuestos marcados y probar la versión final.

**Qué queda fuera:** WebSocket, atlas de ítems, entidades y mobs, regiones mayores de 64 × 64 × 64, bloques de otros mods con garantía y otros loaders. El render de ítems sueltos y el corte por altura son opcionales.

# Control de versiones

| **Versión** | **Fecha** | **Cambios** | **Autor** |
|---|---|---|---|
| v0.1 | 13/11/2026 | Borrador inicial a partir del brief (ítems y bloques individuales). | Analista de software (Claude) |
| v0.2 | 13/11/2026 | Reenfoque a estructuras con menú interactivo; se rehacen EDT, estimación, cronograma y pruebas. | Analista de software (Claude) |
| v0.3 | 13/11/2026 | LiteLoader confirmado como obligatorio; se elimina el plan B con Forge. Sin cambio en horas. | Analista de software (Claude) |
