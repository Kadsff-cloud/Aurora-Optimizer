package dev.aurora.optimizer;

import dev.aurora.optimizer.addon.AuroraAddons;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. The mod is primarily client-side, but the config and the
 * addon API live here so a dedicated server can load the same jar and expose
 * the memory-leak guard to server-side addons.
 */
public final class Aurora implements ModInitializer {
    public static final String MOD_ID = "aurora";
    public static final Logger LOGGER = LoggerFactory.getLogger("Aurora");

    private static AuroraConfig config;

    @Override
    public void onInitialize() {
        config = AuroraConfig.load();
        LOGGER.info("Aurora Optimizer {} cargado (config: {})", AuroraConfig.VERSION, config.path());
        LOGGER.info("LOD: {} | limite de particulas: {} | culling de entidades: {} bloques",
                config.lodEnabled ? "activado" : "desactivado",
                config.particleLimit,
                config.entityCullDistance);
        AuroraAddons.loadCommon();
    }

    public static AuroraConfig config() {
        if (config == null) {
            config = AuroraConfig.load();
        }
        return config;
    }

    public static void reloadConfig() {
        config = AuroraConfig.load();
        LOGGER.info("Configuracion recargada");
    }
}
