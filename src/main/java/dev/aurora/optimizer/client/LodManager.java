package dev.aurora.optimizer.client;

import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.List;

/**
 * Captura y mantiene el LOD lejano. La captura se hace en el hilo del cliente
 * y solo guarda datos primitivos; nunca retiene chunks ni entidades.
 */
public final class LodManager {
    private static final LodCache CACHE = new LodCache(4096);
    private static ResourceKey<Level> dimension;
    private static int tickCounter;
    private static int ringCursor;
    private static int ringIndex;

    private LodManager() {
    }

    public static void onClientTick(Minecraft client) {
        AuroraConfig config = Aurora.config();
        if (client.level == null) {
            if (CACHE.size() > 0 || dimension != null) {
                CACHE.clear();
                WorldgenLodGenerator.reset();
            }
            dimension = null;
            return;
        }

        if (dimension != client.level.dimension()) {
            CACHE.clear();
            dimension = client.level.dimension();
            ringCursor = 0;
            ringIndex = 0;
            WorldgenLodGenerator.reset();
        }

        if (!config.lodActive()) {
            return;
        }

        if (++tickCounter % 10 == 0) {
            captureRing(client, config);
        }
    }

    private static void captureRing(Minecraft client, AuroraConfig config) {
        if (client.player == null || client.level == null) {
            return;
        }

        // Se piden varios anillos justo fuera del borde vanilla: ahi empieza el LOD.
        // Los chunks lejanos que no esten cargados se omiten hasta que el jugador se acerque.
        int baseRadius = Math.min(48, Math.max(2, client.options.getEffectiveRenderDistance() + 1));
        int ringCount = Math.max(1, Math.min(3, config.lodMaxDistanceChunks - baseRadius));
        int radius = baseRadius + (ringIndex % ringCount);
        int side = radius * 2 + 1;
        int perimeter = side * 4;
        int batch = 12;

        for (int i = 0; i < batch && ringCursor < perimeter; i++) {
            int index = ringCursor++;
            int x;
            int z;

            if (index < side) {
                x = index - radius;
                z = -radius;
            } else if (index < side * 2) {
                x = radius;
                z = index - side - radius;
            } else if (index < side * 3) {
                x = radius - (index - side * 2);
                z = radius;
            } else {
                x = -radius;
                z = radius - (index - side * 3);
            }

            int chunkX = client.player.chunkPosition().x() + x;
            int chunkZ = client.player.chunkPosition().z() + z;
            if (CACHE.contains(chunkX, chunkZ)) {
                continue;
            }

            LevelChunk chunk = client.level.getChunkSource()
                    .getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk != null && !chunk.isEmpty()) {
                capture(client.level, chunk, config);
            }
        }

        if (ringCursor >= perimeter) {
            ringCursor = 0;
            ringIndex++;
        }
    }

    public static void onChunkUnload(ClientLevel level, LevelChunk chunk) {
        AuroraConfig config = Aurora.config();
        if (!config.lodActive() || chunk == null || chunk.isEmpty()) {
            return;
        }
        capture(level, chunk, config);
    }

    private static void capture(ClientLevel level, LevelChunk chunk, AuroraConfig config) {
        try {
            LodChunk lod = LodChunk.capture(level, chunk, config.cellSize());
            if (!lod.isEmpty()) {
                CACHE.put(lod);
                AuroraStats.chunksCaptured++;
                if (AuroraStats.chunksCaptured == 1) {
                    Aurora.LOGGER.info("LOD: primera captura en ({}, {}), cache {} chunks / {}",
                            lod.chunkX, lod.chunkZ, CACHE.size(), MemoryGuard.formatBytes(CACHE.estimatedBytes()));
                }
            }
        } catch (Throwable t) {
            // Un chunk raro no debe tumbar el cliente; se descarta la captura.
            if (config.lodDebug) {
                Aurora.LOGGER.warn("No se pudo capturar el chunk {} {}", chunk.getPos(), t.toString());
            }
        }
    }

    public static void applyConfig() {
        AuroraConfig config = Aurora.config();
        CACHE.setMaxChunks(config.lodMaxChunks);
        if (!config.lodActive()) {
            CACHE.clear();
        }
    }

    public static void trim() {
        CACHE.setMaxChunks(Aurora.config().lodMaxChunks);
        CACHE.trim();
    }

    /** Inserta un chunk generado por worldgen sin pasar por la captura del cliente. */
    public static void addGenerated(LodChunk lod) {
        if (lod == null || lod.isEmpty()) {
            return;
        }
        CACHE.put(lod);
        AuroraStats.worldgenChunks++;
        if (AuroraStats.worldgenChunks == 1) {
            Aurora.LOGGER.info("LOD worldgen: primer chunk generado en ({}, {}), cache {} chunks / {}",
                    lod.chunkX, lod.chunkZ, CACHE.size(), MemoryGuard.formatBytes(CACHE.estimatedBytes()));
        }
    }

    public static boolean contains(int chunkX, int chunkZ) {
        return CACHE.contains(chunkX, chunkZ);
    }

    public static void clear() {
        CACHE.clear();
        dimension = null;
        ringCursor = 0;
        ringIndex = 0;
        WorldgenLodGenerator.reset();
    }

    public static List<LodChunk> snapshot() {
        return CACHE.snapshot();
    }

    public static int size() {
        return CACHE.size();
    }

    public static long bytes() {
        return CACHE.estimatedBytes();
    }
}
