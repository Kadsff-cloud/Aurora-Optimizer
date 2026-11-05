package dev.aurora.optimizer.addon;

import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Carga y despacha los addons sin acoplarlos al nucleo. */
public final class AuroraAddons {
    private static final List<AuroraAddon> COMMON = new ArrayList<>();
    private static final List<AuroraClientAddon> CLIENT = new ArrayList<>();
    private static boolean commonLoaded;
    private static boolean clientLoaded;

    private AuroraAddons() {
    }

    public static void loadCommon() {
        if (commonLoaded) {
            return;
        }
        commonLoaded = true;
        for (EntrypointContainer<AuroraAddon> container : FabricLoader.getInstance()
                .getEntrypointContainers("aurora", AuroraAddon.class)) {
            try {
                AuroraAddon addon = container.getEntrypoint();
                addon.onInitialize(Aurora.config());
                COMMON.add(addon);
                Aurora.LOGGER.info("Addon de Aurora cargado: {} ({})", addon.id(), container.getProvider().getMetadata().getId());
            } catch (Throwable t) {
                Aurora.LOGGER.warn("Un addon de Aurora fallo al inicializarse", t);
            }
        }
    }

    public static void loadClient() {
        if (clientLoaded) {
            return;
        }
        clientLoaded = true;
        for (EntrypointContainer<AuroraClientAddon> container : FabricLoader.getInstance()
                .getEntrypointContainers("aurora:client", AuroraClientAddon.class)) {
            try {
                AuroraClientAddon addon = container.getEntrypoint();
                CLIENT.add(addon);
                Aurora.LOGGER.info("Addon cliente de Aurora cargado: {} ({})", addon.id(), container.getProvider().getMetadata().getId());
            } catch (Throwable t) {
                Aurora.LOGGER.warn("Un addon cliente de Aurora fallo al inicializarse", t);
            }
        }
    }

    public static void tickClient(Minecraft client) {
        for (AuroraClientAddon addon : CLIENT) {
            try {
                addon.onClientTick(client);
            } catch (Throwable t) {
                Aurora.LOGGER.warn("El addon {} fallo en tick", addon.id(), t);
            }
        }
    }

    public static void collectSubmits(LevelRenderContext context) {
        for (AuroraClientAddon addon : CLIENT) {
            try {
                addon.onCollectSubmits(context);
            } catch (Throwable t) {
                Aurora.LOGGER.warn("El addon {} fallo al dibujar", addon.id(), t);
            }
        }
    }

    public static void trim() {
        for (AuroraAddon addon : COMMON) {
            try {
                addon.onTrim();
            } catch (Throwable t) {
                Aurora.LOGGER.warn("El addon {} fallo al limpiar", addon.id(), t);
            }
        }
    }

    public static int commonCount() {
        return COMMON.size();
    }

    public static int clientCount() {
        return CLIENT.size();
    }
}
