package dev.aurora.optimizer.client;

import dev.aurora.optimizer.AuroraConfig;
import dev.aurora.optimizer.Aurora;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Extras al estilo Sodium Extra que se aplican por frame sin tocar las
 * opciones del usuario: altura de nubes y control de niebla.
 */
public final class Extras {
    private Extras() {
    }

    public static void onStartMain(LevelTerrainRenderContext context) {
        AuroraConfig config = Aurora.config();
        if (context.levelState() == null) {
            return;
        }

        if (config.disableClouds) {
            context.levelState().cloudHeight = -2048.0f;
        } else if (config.cloudHeightOverride) {
            context.levelState().cloudHeight = config.cloudHeight;
        }

        if (config.fogOverride) {
            CameraRenderState camera = context.levelState().cameraRenderState;
            if (camera != null && camera.fogData != null) {
                FogData fog = camera.fogData;
                float blocks = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0f;
                fog.environmentalStart = blocks * config.fogStart;
                fog.environmentalEnd = blocks * config.fogEnd;
                if (Aurora.config().lodDebug) {
                    Aurora.LOGGER.info("Niebla: {} -> {}", fog.environmentalStart, fog.environmentalEnd);
                }
            }
        }
    }
}
