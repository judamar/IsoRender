# Entorno de desarrollo y pruebas (T-005, T-006, T-007)

## Versiones fijadas
| Componente | Versión |
|---|---|
| Minecraft | 1.12.2 (mappings MCP `snapshot_20170624`) |
| LiteLoader | `1.12.2-SNAPSHOT` (repositorio `dl.liteloader.com/versions`) |
| JDK de compilación | 8 (8u512, `/usr/lib/jvm/java-8-openjdk`, fijado en `gradle.properties`) |
| Gradle | 4.10.3 (wrapper) |
| Plugin | `com.anatawa12.forge:ForgeGradle:2.3-1.0.+` (`net.minecraftforge.gradle.liteloader`) |
| Pruebas | JUnit 4.12 |
| Cliente de pruebas | Prism Launcher, instancia `IsoRender-test`: Minecraft 1.12.2 + LiteLoader 1.12.2-SNAPSHOT + LWJGL 2, sin otros mods |

## Comandos
- Preparar (una vez): `./gradlew setupDecompWorkspace`
- Compilar y probar: `./gradlew build` → `build/libs/mod-isorender-0.1.0-mc1.12.2.litemod`
- Instalar en el cliente de pruebas: copiar el `.litemod` a `<instancia>/minecraft/mods/`

## Problema conocido
Con caché de Gradle limpia, `setupDecompWorkspace` falla con `ZipException: invalid entry compressed size` (JDK 8u512). Solución: re-empaquetar con compresión STORED todos los `*-sources.jar` de `~/.gradle/caches/modules-2/files-2.1` y borrar `~/.gradle/caches/minecraft/deobfedDeps/**/*sources*`.

## Verificación de T-005
El log del cliente (`latest.log`) muestra `Successfully added mod IsoRender version 0.1.0` e `Initialising mod IsoRender`. Aviso inocuo: falta `libfliteWrapper.so` (Narrator).
