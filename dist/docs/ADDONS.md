# API de addons

Aurora expone dos entrypoints de Fabric para que otros mods aporten funciones sin
tocar el núcleo.

## Addon común

```json
{
  "entrypoints": {
    "aurora": ["com.ejemplo.MiAddon"]
  }
}
```

```java
public final class MiAddon implements dev.aurora.optimizer.addon.AuroraAddon {
    @Override
    public String id() {
        return "mi-addon";
    }

    @Override
    public void onInitialize(dev.aurora.optimizer.AuroraConfig config) {
        // Registrar bloques, comandos o configuración propia.
    }

    @Override
    public void onTrim() {
        // Liberar caches propias cuando Aurora limpia memoria.
    }
}
```

Se carga en el entrypoint `main`, también en servidor dedicado. Si lanza una
excepción, Aurora la registra y sigue funcionando.

## Addon de cliente

```json
{
  "entrypoints": {
    "aurora:client": ["com.ejemplo.MiAddonCliente"]
  }
}
```

```java
public final class MiAddonCliente implements dev.aurora.optimizer.addon.AuroraClientAddon {
    @Override
    public String id() {
        return "mi-addon-cliente";
    }

    @Override
    public void onClientTick(net.minecraft.client.Minecraft client) {
        // Lógica por tick.
    }

    @Override
    public void onCollectSubmits(
            net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        // Enviar geometría propia con context.submitNodeCollector().
    }
}
```

Los addons de cliente reciben tick y submissions **después** de que Aurora
registre los suyos. Un fallo en un addon se captura y se loguea; no se propaga.

## Límites

- La API es deliberadamente pequeña en `0.1.0`: no hay registro de comandos ni
  de configuración por addon todavía.
- No compartas estado entre el hilo de render y el de tick sin sincronización.
- No retengas `LevelChunk` ni `ClientLevel` en caches de addon; usa instantáneas.
