package dev.aurora.optimizer.client;

import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Limpieza defensiva del estado que queda colgado del ClientLevel al salir de
 * un mundo. Vanilla libera el nivel entero cuando nadie lo referencia, pero
 * {@code ClientLevel.disconnect()} no vacia entidades, tint caches, mapas de
 * destruccion ni la cache de chunks. Si un mod (o una referencia rezagada)
 * conserva el nivel, todo eso sigue vivo.
 *
 * <p>No sustituye al GC: elimina las referencias grandes para que un nivel
 * retenido no arrastre chunks, entidades y datos de mapa.</p>
 */
public final class LevelLeakGuard {
    private LevelLeakGuard() {
    }

    public static void onDisconnect(Minecraft client, ClientLevel level) {
        if (client == null || level == null) {
            return;
        }
        AuroraConfig config = Aurora.config();
        if (!config.clearCachesOnDisconnect) {
            return;
        }

        int entitiesBefore = level.getEntityCount();
        int entitiesRemoved = 0;
        try {
            List<Entity> entities = new ArrayList<>();
            for (Entity entity : level.entitiesForRendering()) {
                if (entity != null && entity != client.player && !entity.isRemoved()) {
                    entities.add(entity);
                }
            }
            for (Entity entity : entities) {
                entity.setRemoved(Entity.RemovalReason.DISCARDED);
                entity.onClientRemoval();
                entitiesRemoved++;
            }
        } catch (Throwable t) {
            Aurora.LOGGER.warn("No se pudieron limpiar todas las entidades del nivel: {}", t.toString());
        }

        try {
            level.clearTintCaches();
        } catch (Throwable t) {
            Aurora.LOGGER.warn("No se pudieron limpiar los tint caches: {}", t.toString());
        }
        try {
            level.getGloballyRenderedBlockEntities().clear();
        } catch (Throwable t) {
            Aurora.LOGGER.warn("No se pudieron limpiar los block entities globales: {}", t.toString());
        }
        try {
            level.destructionProgress().clear();
        } catch (Throwable t) {
            Aurora.LOGGER.warn("No se pudo limpiar el progreso de destruccion: {}", t.toString());
        }

        int chunksDropped = 0;
        try {
            if (client.player != null) {
                int radius = Math.min(64, client.options.getEffectiveRenderDistance() + 4);
                ChunkPos center = client.player.chunkPosition();
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        level.getChunkSource().drop(new ChunkPos(center.x() + dx, center.z() + dz));
                        chunksDropped++;
                    }
                }
            }
        } catch (Throwable t) {
            Aurora.LOGGER.warn("No se pudieron soltar todos los chunks del nivel: {}", t.toString());
        }

        int entitiesAfter = level.getEntityCount();
        AuroraStats.levelsCleared++;
        AuroraStats.entitiesCleared += Math.max(0, entitiesBefore - entitiesAfter);
        AuroraStats.chunksDropped += chunksDropped;
        Aurora.LOGGER.info("Aurora leak guard: nivel descargado; entidades {} -> {} (removidas {}), chunks soltados {}, tint caches limpiadas",
                entitiesBefore, entitiesAfter, entitiesRemoved, chunksDropped);
    }
}
