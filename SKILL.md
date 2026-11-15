---
name: fabric-visual-test
description: Prueba visual automática de un mod de Fabric (Minecraft) sin pedirle al usuario que abra el juego. Mete una clase temporal en el cliente que crea un mundo, ejecuta comandos, pulsa teclas y saca capturas; luego lanza `gradlew runClient`, junta las capturas en un mosaico y las mira. Úsala cuando haya que comprobar algo que solo se ve dentro del juego (render, animaciones, HUD, modelos, texturas, recursos enviados por el servidor, efectos al correr/apuntar/disparar) o para reproducir un bug visual que el usuario describe o enseña en una captura.
---

# Prueba visual de un mod de Fabric

El juego se maneja solo desde una clase del propio mod. No hace falta ningún programa externo ni que el usuario mire el cliente: tú lanzas el cliente, esperas a que se cierre y lees las capturas (las imágenes se pueden leer con la herramienta de leer archivos).

## Cuándo usarla

- Hay un cambio visual que confirmar: modelo, animación, mano/brazos, HUD, partículas, sonidos con subtítulos, texturas.
- El usuario describe o enseña un bug visual: primero **reprodúcelo** con capturas, luego arréglalo y vuelve a sacar capturas para comprobarlo.
- Hay que ver que algo llega al cliente (resource packs del servidor, sincronización de datos): las capturas y el log lo enseñan.

No sirve para servidores dedicados sin cliente. Para eso basta `runServer` y leer el log, pero el servidor dedicado necesita `eula=true` en `run/eula.txt`: **no aceptes la EULA por el usuario**, pregúntale. El servidor integrado del cliente (lo que usa esta skill) no la pide.

## Pasos

### 1. Mira lo que ya hay

Busca en el proyecto clases parecidas (`*VisualCheck*`, `ScreenshotRecorder`, `createIntegratedServerLoader`) y propiedades en `build.gradle` (`loom { runs { client { vmArg ... } } }`). Si ya existe una prueba que hace lo que necesitas, úsala tal cual. Mira también:

- El source set del cliente (`src/client/java` con `splitEnvironmentSourceSets()`, o `src/main/java` si no está separado).
- El inicializador de cliente (`ClientModInitializer` en `fabric.mod.json`, entrada `client`).
- Los mappings (`yarn_mappings` en `gradle.properties`). La plantilla usa nombres de **Yarn 1.21.1**; con otra versión o con Mojmap algunos nombres cambian (compílalo y corrige).
- `run/config/<mod>/`: la configuración que leerá el juego de prueba. Puedes editarla para la prueba (está fuera de git normalmente), pero dile al usuario qué cambiaste.

### 2. Escribe la clase temporal

Copia `templates/VisualCheck.java` al paquete del cliente, cámbiale el nombre (p. ej. `SprintVisualCheck`), el `package` y el guion de la prueba (`script`). La plantilla ya resuelve lo difícil:

- Espera a la pantalla de título sin overlay de carga y crea un mundo plano en creativo con nombre único.
- Pone `pauseOnLostFocus = false`: la ventana suele estar sin foco y el juego se pausaría.
- Cada tick fija perspectiva, FOV, yaw y pitch para que las capturas sean comparables.
- Ejecuta comandos en el servidor integrado (`give`, `time set`, comandos del mod...).
- Pulsa teclas con `KeyBinding.setPressed` (mantener) o `KeyBinding.onKeyPressed` (pulsación suelta, p. ej. recargar con R).
- Saca capturas a `run/screenshots/<prefijo>-<tick>.png` y cierra el juego al final.

El tiempo va en ticks (20 por segundo) contados desde que el jugador está en el mundo. Deja ~40 ticks al principio para que carguen chunks y lleguen los datos del servidor, y limpia el chat (`getChatHud().clear(false)`) antes de las capturas. Saca varias capturas en cada fase (al empezar, durante, justo después y un rato después): los bugs de transición solo se ven así.

### 3. Actívala solo si se pide

En el inicializador de cliente:

```java
if (Boolean.getBoolean("<mod>.miPrueba")) MiPruebaVisualCheck.register();
```

En `build.gradle`, dentro de `loom { runs { client { ... } } }` (créalo si no existe):

```groovy
if (project.hasProperty('miPrueba')) vmArg '-D<mod>.miPrueba=true'
```

Así un `runClient` normal no la ejecuta.

### 4. Lánzala

```
rm -f run/screenshots/<prefijo>-*.png
timeout 580 ./gradlew runClient -PmiPrueba > <scratchpad>/run.log 2>&1
```

- Se abre una ventana de Minecraft y se cierra sola (30-90 s). Ponle a la herramienta un timeout de 10 minutos; la primera vez Gradle tarda más.
- Si no se cierra, la prueba se quedó esperando (pantalla inesperada, error). Mira el log.
- Si el usuario está usando el PC, avísale antes de lanzarla: abre una ventana.
- Si Gradle no descarga dependencias, mira si hay notas del entorno sobre mirrors de Maven (memoria del usuario, CLAUDE.md). Nunca uses `--refresh-dependencies` sin entender por qué fallan.
- Busca en el log errores y lo que te interese: `grep -i "error\|exception\|<mod>" run.log`. El aviso `Unable to delete file ...latest.log` no importa.

### 5. Mira las capturas

Junta varias en un mosaico para leer una sola imagen:

```
python <ruta de esta skill>/scripts/montage.py run/screenshots "<prefijo>-*.png" <scratchpad>/mosaico.png
```

Lee el mosaico; si algo no se aprecia, lee la captura suelta a tamaño completo. Describe lo que ves comparándolo con lo que debería pasar.

### 6. Si la imagen no basta, mide

Añade logs temporales con un prefijo fácil de buscar, uno de cada N frames para no llenar el log:

```java
if ((DBG++ % 20) == 0) LOGGER.info("DBG stance={} rot={}", ..., ...);
```

Luego `grep -o "DBG.*" run.log | sed 's/\x1b\[[0-9;]*m//g'`. Imprime el estado que sospechas (valores de huesos, flags, estado de la animación) antes y después del momento del bug. Si hay una librería de por medio (GeckoLib, etc.), lee su código fuente del jar `-sources` en `~/.gradle/caches/modules-2/files-2.1/` para entender qué hace con esos valores.

### 7. Arregla, repite y limpia

1. Arregla y vuelve al paso 4 con el mismo guion. Compara capturas antes/después.
2. Quita los logs `DBG` siempre.
3. Quita la clase temporal, su línea en el inicializador y la línea de `build.gradle`, salvo que el usuario quiera quedársela como prueba fija (pregúntale o díselo).
4. Borra las capturas de `run/screenshots` que creaste.
5. Compila (`gradlew build`) para confirmar que todo queda bien.
6. Al usuario: qué reprodujiste, qué viste, qué cambiaste y cómo lo comprobaste. Si cambiaste algo en `run/config`, díselo.

## Problemas comunes

- **No pasa nada al entrar**: el inicializador no registró la prueba (propiedad mal escrita en `build.gradle` o en el `Boolean.getBoolean`).
- **Se queda en la pantalla de título**: hay un overlay o pantalla distinta (aviso de mods, pantalla de error). Mira el log.
- **El jugador no corre**: hace falta `forwardKey` y `sprintKey` a la vez, y comida > 6 en supervivencia (en creativo no importa).
- **Capturas negras o del menú**: la captura salió antes de entrar; cuenta los ticks solo cuando `client.player != null && client.currentScreen == null`.
- **El chat tapa la imagen**: límpialo antes de capturar.
- **Clase en archivo con otro nombre / mayúsculas**: en Windows `Foo.java` y `foo.java` son el mismo archivo para el sistema, pero no para javac. Si falla con "should be declared in a file named", renómbralo con `git mv` en dos pasos.
