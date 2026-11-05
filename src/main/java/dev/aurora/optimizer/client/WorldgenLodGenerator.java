package dev.aurora.optimizer.client;

import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.List;

/**
 * Genera un LOD de terreno por delante del radio de chunks cargado, solo en
 * singleplayer. Usa el {@link ChunkGenerator} y el {@link RandomState} del
 * servidor integrado para muestrear altura y bioma sin cargar chunks completos.
 *
 * <p>El trabajo se hace en el hilo del servidor y se entrega al cliente en
 * lotes pequenos. Asi el render no paga el coste de worldgen y el servidor no
 * recibe una avalancha de muestras de golpe.</p>
 */
public final class WorldgenLodGenerator {
    private static boolean inFlight;
    private static long generation;
    private static int ringRadius = -1;
    private static int ringCursor;
    private static boolean loggedUnavailable;
    private static boolean loggedFirstAttempt;
    private static int idleTicks;

    private WorldgenLodGenerator() {
    }

    public static void onClientTick(Minecraft client) {
        AuroraConfig config = Aurora.config();
        if (!config.lodWorldgen || !config.lodActive()) {
            return;
        }
        if (client.level == null || client.player == null || client.isPaused() || inFlight) {
            return;
        }
        if (idleTicks > 0) {
            idleTicks--;
            return;
        }
        if (!client.hasSingleplayerServer()) {
            if (!loggedUnavailable) {
                loggedUnavailable = true;
                Aurora.LOGGER.info("LOD worldgen: solo disponible en singleplayer; en multijugador se usa la captura de chunks cargados.");
            }
            return;
        }

        IntegratedServer server = client.getSingleplayerServer();
        if (server == null) {
            return;
        }
        ResourceKey<Level> dimension = client.level.dimension();
        ServerLevel serverLevel = server.getLevel(dimension);
        if (serverLevel == null) {
            return;
        }

        List<ChunkPos> batch = nextBatch(client, config);
        if (batch.isEmpty()) {
            // El anillo ya esta completo (o el radio no supera el render
            // vanilla): no recorrer 4096 posiciones cada tick.
            idleTicks = 100;
            return;
        }
        if (!loggedFirstAttempt) {
            loggedFirstAttempt = true;
            Aurora.LOGGER.info("LOD worldgen: generando anillos {}..{} en singleplayer, lote {} chunk(s)/tick",
                    Math.max(config.lodStartDistanceChunks, client.options.getEffectiveRenderDistance() + 1),
                    config.lodWorldgenRadiusChunks, config.lodWorldgenBatch);
        }

        long myGeneration = generation;
        int cellSize = config.cellSize();
        inFlight = true;
        try {
            server.execute(() -> {
                List<LodChunk> generated = new ArrayList<>(batch.size());
                long start = System.nanoTime();
                long samples = 0;
                try {
                    ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
                    RandomState randomState = serverLevel.getChunkSource().randomState();
                    for (ChunkPos pos : batch) {
                        if (serverLevel.getChunkSource().hasChunk(pos.x(), pos.z())) {
                            continue;
                        }
                        LodChunk lod = generate(serverLevel, generator, randomState, pos, cellSize);
                        if (lod != null && !lod.isEmpty()) {
                            generated.add(lod);
                            samples += (long) lod.cells * lod.cells;
                        }
                    }
                } catch (Throwable t) {
                    Aurora.LOGGER.warn("LOD worldgen: fallo generando lote en {}: {}", dimension.identifier(), t.toString());
                }
                long elapsed = System.nanoTime() - start;
                long finalSamples = samples;
                try {
                    client.execute(() -> {
                        if (myGeneration != generation) {
                            return;
                        }
                        if (client.level == null || !client.level.dimension().equals(dimension)) {
                            inFlight = false;
                            return;
                        }
                        for (LodChunk lod : generated) {
                            LodManager.addGenerated(lod);
                        }
                        AuroraStats.worldgenSamples += finalSamples;
                        AuroraStats.worldgenNanos += elapsed;
                        inFlight = false;
                    });
                } catch (Throwable t) {
                    if (myGeneration == generation) {
                        inFlight = false;
                    }
                    Aurora.LOGGER.warn("LOD worldgen: no se pudo entregar el lote al cliente: {}", t.toString());
                }
            });
        } catch (Throwable t) {
            inFlight = false;
            Aurora.LOGGER.warn("LOD worldgen: no se pudo programar el lote en el servidor: {}", t.toString());
        }
    }

    /**
     * Devuelve el siguiente lote de posiciones del anillo. Se salta lo que ya
     * esta en cache; los chunks cargados se descartan despues, en el servidor.
     */
    private static List<ChunkPos> nextBatch(Minecraft client, AuroraConfig config) {
        int start = Math.max(config.lodStartDistanceChunks,
                client.options.getEffectiveRenderDistance() + 1);
        int max = Math.min(config.lodWorldgenRadiusChunks, config.lodMaxDistanceChunks);
        if (max <= start) {
            return List.of();
        }
        if (ringRadius < start || ringRadius > max) {
            ringRadius = start;
            ringCursor = 0;
        }

        List<ChunkPos> batch = new ArrayList<>(config.lodWorldgenBatch);
        int attempts = 0;
        while (batch.size() < config.lodWorldgenBatch && attempts < 4096) {
            attempts++;
            int radius = ringRadius;
            int side = radius * 2 + 1;
            int perimeter = side * 4;
            int index = ringCursor++;
            if (ringCursor >= perimeter) {
                ringCursor = 0;
                ringRadius++;
                if (ringRadius > max) {
                    ringRadius = start;
                }
            }

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
            if (LodManager.contains(chunkX, chunkZ)) {
                continue;
            }
            batch.add(new ChunkPos(chunkX, chunkZ));
        }
        return batch;
    }

    private static LodChunk generate(ServerLevel level, ChunkGenerator generator,
                                     RandomState randomState, ChunkPos pos, int cellSize) {
        int cells = 16 / cellSize;
        short[] heights = new short[cells * cells];
        int[] colors = new int[cells * cells];
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        int seaLevel = generator.getSeaLevel();
        int chunkMinX = pos.getMinBlockX();
        int chunkMinZ = pos.getMinBlockZ();
        BiomeSource biomeSource = generator.getBiomeSource();
        Climate.Sampler sampler = randomState.sampler();

        for (int cz = 0; cz < cells; cz++) {
            for (int cx = 0; cx < cells; cx++) {
                int localX = Math.min(15, cx * cellSize + cellSize / 2);
                int localZ = Math.min(15, cz * cellSize + cellSize / 2);
                int worldX = chunkMinX + localX;
                int worldZ = chunkMinZ + localZ;
                int index = cz * cells + cx;

                int baseHeight = generator.getBaseHeight(worldX, worldZ,
                        Heightmap.Types.MOTION_BLOCKING, level, randomState);
                int top = baseHeight - 1;
                if (top < minY || top > maxY) {
                    heights[index] = LodChunk.EMPTY;
                    colors[index] = 0;
                    continue;
                }

                boolean water = top < seaLevel;
                int sampleY = water ? seaLevel : top;
                Holder<Biome> biomeHolder = biomeSource.getNoiseBiome(
                        QuartPos.fromBlock(worldX),
                        QuartPos.fromBlock(Math.max(minY, Math.min(maxY, sampleY))),
                        QuartPos.fromBlock(worldZ),
                        sampler);
                Biome biome = biomeHolder == null ? null : biomeHolder.value();
                int rgb;
                int height;
                if (water) {
                    height = Math.min(maxY, seaLevel);
                    rgb = biome == null ? 0x3F76E4 : biome.getWaterColor();
                    float depth = clamp((seaLevel - top) / 32.0f, 0.0f, 0.45f);
                    rgb = shade(rgb, 1.0f - depth);
                } else {
                    height = top;
                    rgb = biome == null ? 0x7FBF5F : biome.getGrassColor(worldX, worldZ);
                    float altitude = clamp((top - seaLevel) / 128.0f, 0.0f, 1.0f);
                    rgb = shade(rgb, 0.80f + 0.20f * altitude);
                }

                heights[index] = (short) height;
                colors[index] = 0xFF000000 | (rgb & 0xFFFFFF);
            }
        }
        return LodChunk.of(pos.x(), pos.z(), cellSize, cells, heights, colors);
    }

    private static int shade(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void reset() {
        generation++;
        inFlight = false;
        ringRadius = -1;
        ringCursor = 0;
        idleTicks = 0;
        loggedFirstAttempt = false;
    }
}
