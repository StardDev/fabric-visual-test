# fabric-visual-test

Una skill para agentes de IA (Claude Code y otros) que **prueban mods de Fabric dentro de Minecraft por su cuenta**. El agente abre el cliente, crea un mundo, ejecuta comandos, pulsa teclas, saca capturas y las revisa. Así comprueba cambios visuales o reproduce bugs sin pedirte que abras el juego y mires.

*An agent skill that lets AI coding agents run a Fabric mod in a real Minecraft client, drive it, take screenshots and review them, to verify visual changes and reproduce visual bugs on their own.*

## Por qué

Un agente puede compilar un mod y pasar tests unitarios, pero no sabe si un modelo se dibuja bien, si una animación vuelve a su sitio o si una textura del servidor llega al cliente. Normalmente eso acaba en "ábrelo y dime qué ves". Con esta skill el agente lo comprueba solo:

1. Mete en el mod una clase temporal que maneja el juego: espera al menú, crea un mundo plano en creativo, ejecuta comandos, mantiene o pulsa teclas y saca capturas en ticks concretos.
2. La activa solo con una propiedad de Gradle (`gradlew runClient -PmiPrueba`), así un `runClient` normal no cambia.
3. Lanza el cliente, que se cierra solo al terminar.
4. Junta las capturas en un mosaico, las mira y las compara con lo esperado.
5. Si la imagen no basta, mete logs temporales, mide el estado en cada frame y lee el código de las librerías implicadas.
6. Arregla, repite la misma prueba para comprobar el arreglo y limpia todo lo temporal.

Ejemplo real: un arma de un mod con animaciones GeckoLib se quedaba con la pose de correr al dejar de correr. El agente reprodujo el bug con capturas y encontró la causa con logs: el icono del hotbar marcaba los huesos como animados y GeckoLib dejaba de resetearlos. Luego comprobó el arreglo con la misma prueba, sin que nadie abriera el juego.

## Contenido

| Archivo | Qué es |
|---|---|
| [`SKILL.md`](SKILL.md) | Las instrucciones que sigue el agente: pasos, buenas prácticas y problemas comunes |
| [`templates/VisualCheck.java`](templates/VisualCheck.java) | Plantilla de la clase de prueba (mundo, comandos, teclas, capturas, cierre) |
| [`scripts/montage.py`](scripts/montage.py) | Junta capturas en un mosaico ordenado y con etiquetas |

## Instalar

**Claude Code**, clonando el repo directamente como skill:

```
# Windows (PowerShell)
git clone https://github.com/StardDev/fabric-visual-test "$env:USERPROFILE\.claude\skills\fabric-visual-test"

# Linux / macOS
git clone https://github.com/StardDev/fabric-visual-test ~/.claude/skills/fabric-visual-test
```

Reinicia Claude Code. La skill se usa sola cuando hace falta comprobar algo en el juego, o con `/fabric-visual-test`.

**Otros agentes**: dile que lea `SKILL.md` y lo siga. Necesita poder ejecutar comandos en tu máquina y, para revisar capturas, leer imágenes.

## Requisitos

- Mod de Fabric con [Loom](https://github.com/FabricMC/fabric-loom) (`gradlew runClient`).
- La plantilla usa nombres de Yarn para Minecraft 1.21.1. Con otra versión o con Mojmap hay que ajustar algunos nombres; la skill lo explica.
- Python con Pillow para el mosaico: `pip install pillow`.
- Mientras corre la prueba se abre una ventana de Minecraft (30-90 s).

## Licencia

[MIT](LICENSE)
