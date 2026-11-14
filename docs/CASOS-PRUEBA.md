# Casos de prueba y estructuras de referencia (T-033)

Base: docs/PLAN.md §7 (CP-01 a CP-31) y los criterios Dado/Cuando/Entonces de docs/ERS.md. Entorno de pruebas: cliente vanilla 1.12.2 + LiteLoader limpio (instancia `IsoRender-test`) con el único `.litemod`. Mundo: superplano en creativo, de un jugador, con trucos activados.

**Columna Tipo:** `U` = ya cubierto por pruebas unitarias (`./gradlew test`); `J` = se comprueba en el juego (lo ejecuta el solicitante con la lista de abajo; el desarrollador entrega la lista y espera su aprobación, como pide CLAUDE.md); `M` = medición.
**Estado:** `—` sin ejecutar, `OK`, `FALLA (nota)`.

## 1. Estructuras de referencia
Se construyen con comandos vanilla desde el punto de la esquina mínima (jugador de pie sobre suelo plano; las coordenadas `~` son relativas a él). Cada estructura se describe por **qué ejercita**. Usa `/fill ~ ~ ~ ~dx ~dy ~dz <bloque>`; límite vanilla 32 768 bloques por comando.

| ID | Tamaño | Contenido | Ejercita |
|---|---|---|---|
| ER-1 «Mixta» | 8×8×8 | Suelo de tablones; paredes de adoquín; ventanas de cristal y cristal tintado; puerta de madera (2 bloques de alto); valla y cristal fino (conexiones); escalera y losa en varias orientaciones; antorcha; hojas de roble; 1 bloque de agua; cofre, cama y cartel | CP-07, 09, 10, 11 |
| ER-2 «Sólido 16» | 16×16×16 | Cubo de piedra con un hueco de 4×4×4 con cristal | CP-15 (16³, ≥15 fps), CP-08 |
| ER-3 «Sólido 32» | 32×32×32 | Cubo de piedra con vetas de cristal y hojas | CP-22 (renders a 2048 px), CP-27 |
| ER-4 «Máximo» | 64×64×64 | Cubo de piedra (8 comandos `/fill` de 32×32×32), con cristal y agua en un lado | CP-06 (límite), CP-15 (≥5 fps, vista simplificada), CP-24 (memoria) |
| ER-5 «Alargada» | 64×1×4 y 4×1×64 | Línea de bloques de colores distintos | CP-08 (encuadre extremo) |
| ER-6 «Negativa» | 6×6×6 | Cualquier estructura con la región en X, Z negativos | CP-18 (nombre con negativos) |
| ER-7 «Solo aire» | 5×5×5 | Región vacía sobre el cielo | CP-06 |
| ER-8 «Marco» | 3×3×1 | Un marco de objetos con un ítem, junto a bloques | Limitación conocida (entidades fuera de la v1, T-044) |

Ejemplo de comandos para ER-1 (esquina mínima = posición del jugador; ajusta si algo no coincide con tu versión):
```
/fill ~ ~ ~ ~7 ~ ~7 minecraft:planks
/fill ~ ~1 ~ ~7 ~4 ~7 minecraft:cobblestone
/fill ~1 ~1 ~1 ~6 ~3 ~6 minecraft:air
/fill ~2 ~2 ~ ~5 ~3 ~ minecraft:glass
/setblock ~6 ~2 ~ minecraft:stained_glass 14
/setblock ~3 ~1 ~ minecraft:oak_door 0
/setblock ~3 ~2 ~ minecraft:oak_door 8
/fill ~ ~5 ~ ~7 ~5 ~7 minecraft:leaves
/setblock ~2 ~1 ~5 minecraft:chest 3
/setblock ~4 ~1 ~5 minecraft:bed 0
/setblock ~6 ~1 ~2 minecraft:standing_sign 4
/setblock ~1 ~1 ~1 minecraft:water
```
ER-2/3/4: `/fill ~ ~ ~ ~15 ~15 ~15 minecraft:stone`, `~31` y, para 64³, ocho `/fill` con las esquinas `~0/~32` en cada eje.

## 2. Matriz de casos
Pasos con la selección hechos con `/isorender pos1` y `pos2` apuntando a bloques de la estructura. Los criterios exactos están en el ERS (se cita el CA).

| Caso | Estructura | Pasos | Resultado esperado | Tipo | Estado |
|---|---|---|---|---|---|
| CP-01 | — | Arrancar con el `.litemod`; repetir con versión de MC distinta y con otros mods | Carga y saludo en el chat; LiteLoader lista IsoRender 0.1.0 | J | OK (T-034, solicitante, 2026-11-14) |
| CP-02 | — | `unzip -l` del `.litemod`; `./gradlew validarLitemod` | `litemod.json` + `LICENSE`; la tarea falla si falta alguno | M | OK (T-031) |
| CP-03 | ER-1 | pos1 apuntando a bloque; pos1 sin apuntar (usa el bloque bajo los pies); pos1 de nuevo (reemplaza); pos2 = pos1 | Mensajes con coordenadas; región 1×1×1 válida | J/U | OK (T-034, solicitante, 2026-11-14) |
| CP-04 | ER-1 | pos1/pos2 en orden inverso | Mismas dimensiones (RN-002) | U | OK |
| CP-05 | ER-1 | Ver el contorno; `/isorender clear` | Caja visible desde dentro y fuera; desaparece al limpiar | J | OK (T-034, solicitante, 2026-11-14) |
| CP-06 | ER-4, ER-7 | Un solo punto; 65 de lado; chunk descargado (volar lejos); región solo de aire; Renderizar | Mensaje de motivo en español en cada caso, sin PNG | J/U | OK (T-034, solicitante, 2026-11-14) |
| CP-07 | ER-1 | `/isorender render` y abrir el PNG | Bloques correctos y orientados; puerta, valla, escaleras, losas | J | OK (T-034, solicitante, 2026-11-14) |
| CP-08 | ER-5, ER-2 | Rotaciones 0, 45, 90, 180, 270 y elevaciones 5, 35, 90; vista isométrica y perspectiva | Estructura entera dentro del PNG con margen | J/U | OK (T-034, solicitante, 2026-11-14) |
| CP-09 | ER-1 | Abrir el PNG sobre fondo blanco y negro | Alfa 0 fuera de la silueta; alfa parcial en cristal | J/M | OK (T-034, solicitante, 2026-11-14) |
| CP-10 | ER-1 | Cristal, hojas, agua en bioma distinto (bosque, pantano) | Translúcidos mezclados, tinte por bioma | J | OK (T-034, solicitante, 2026-11-14) |
| CP-11 | ER-1 | Cofre, cama, cartel; cabeza | Entidades de bloque dibujadas; una que falla se omite con aviso | J | OK (T-034, solicitante, 2026-11-14) |
| CP-12 | — | K; `/isorender menu`; K con inventario abierto; sin selección | Menú abre; no se abre un 2.º; con «Falta definir la selección» y Renderizar gris | J | OK (T-023) |
| CP-13 | ER-1 | Deslizadores y campos de rotación/elevación, extremos | Valores limitados a 0–359 y 5–90 | J/U | OK (T-024) |
| CP-14 | ER-1 | Zoom y desplazamientos, extremos | Limitados a 25–400 % y ±100 % | J/U | OK (T-024) |
| CP-15 | ER-2, ER-4 | Mover controles 10 s con F3 | ≥15 fps (16³), ≥5 fps (64³, vista simplificada) | J/M | OK (T-025, sin medida) |
| CP-16 | ER-1 | Resolución 2048, Renderizar, doble clic | PNG 2048×2048; un solo render; rango inválido rechazado | J/U | OK (T-026) |
| CP-17 | ER-1 | Restablecer, presets, entrada numérica válida/inválida | Valores correctos; error en español | J/U | OK (T-024/T-026) |
| CP-18 | ER-6 | Dos renders seguidos; carpeta inexistente; solo lectura; coordenadas negativas | Dos archivos; carpeta creada; error informado sin PNG parcial; nombre válido | J/U | OK (T-028) |
| CP-19 | — | Éxito y fallo | Ruta una vez; causa en español | J | OK (T-027) |
| CP-20 | — | Configuración válida, corrupta, ausente; último ajuste | Ver T-029/T-030 | J/U | OK |
| CP-21 | — | Cliente vanilla + LiteLoader sin Forge | Sin errores atribuibles al mod en `latest.log` | J | OK (T-034, solicitante, 2026-11-14) |
| CP-22 | ER-3 | 5 renders de 32³ a 2048 px (`/isorender render`) midiendo el tiempo del chat | Promedio ≤ 10 s, máximo ≤ 15 s | M | OK (T-034, solicitante, sin cifras anotadas) |
| CP-23 | ER-1 | PNG a zoom 400 % sobre blanco y negro | Bordes sin halos | J | OK (T-019) |
| CP-24 | ER-1 | 30 fallos forzados | Sin cierre | J | OK (T-022) |
| CP-25 | — | `./gradlew build` con JDK 8; revisión de paquetes y comentarios | Compila; estructura por paquetes | M | OK |
| CP-26 | — | Revisión legal | docs/LICENCIAS.md | M | OK (T-032) |
| CP-27 | ER-3 | 30 renders y 30 aperturas del menú (`estres 30`) | Memoria ±5 %, sin error GL | J/M | OK (T-022) |
| CP-28 | ER-1 | F2 antes/después de renderizar y de cerrar el menú | Fotograma idéntico | J | OK (T-022) |
| CP-29 | — | Menú en 854×480 y 1920×1080 | Controles visibles y sin solape | J | OK (T-026; 1920×1080 en T-034) |
| CP-30 | ER-1 | 20 selecciones y renders; comparar el mundo (F3+A, `/clone` de control) | El mundo no cambia | J | OK (T-034, solicitante, 2026-11-14) |
| CP-31 | — | Solo si T-042/T-043 | — | — | no aplica |

## 3. Procedimiento
1. **T-034** (funcionales): el desarrollador entrega la lista del §2 por bloques (selección → render → menú → configuración); el solicitante ejecuta y anota Estado y defectos (con severidad).
2. **T-035/T-036**: mediciones (CP-15, 22, 23, 27) y compatibilidad/estabilidad (CP-21, 24, 29, 30).
3. **Salida:** 100 % de los Must, ≥ 90 % de los Should, cero defectos críticos/altos (PLAN §7).
