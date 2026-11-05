package dev.aurora.optimizer.addon;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;

/**
 * Variante cliente para addons que necesitan tick o geometria propia.
 * Entrada: "aurora:client".
 */
public interface AuroraClientAddon {
    String id();

    default void onClientTick(Minecraft client) {
    }

    default void onCollectSubmits(LevelRenderContext context) {
    }
}
