package dev.aurora.optimizer.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Instantanea compacta de un chunk para el LOD lejano. No guarda el chunk ni
 * referencias al nivel: solo altura y color por celda, de modo que una cache
 * grande sigue ocupando pocos megabytes.
 */
public final class LodChunk {
    public static final short EMPTY = Short.MIN_VALUE;

    public final int chunkX;
    public final int chunkZ;
    public final int cellSize;
    public final int cells;
    public final short[] heights;
    public final int[] colors;
    public final long capturedAt;

    private LodChunk(int chunkX, int chunkZ, int cellSize, int cells, short[] heights, int[] colors) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.cellSize = cellSize;
        this.cells = cells;
        this.heights = heights;
        this.colors = colors;
        this.capturedAt = System.currentTimeMillis();
    }

    static LodChunk of(int chunkX, int chunkZ, int cellSize, int cells, short[] heights, int[] colors) {
        return new LodChunk(chunkX, chunkZ, cellSize, cells, heights, colors);
    }

    public static LodChunk capture(ClientLevel level, LevelChunk chunk, int cellSize) {
        int cells = 16 / cellSize;
        short[] heights = new short[cells * cells];
        int[] colors = new int[cells * cells];
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();

        for (int cz = 0; cz < cells; cz++) {
            for (int cx = 0; cx < cells; cx++) {
                int localX = Math.min(15, cx * cellSize + cellSize / 2);
                int localZ = Math.min(15, cz * cellSize + cellSize / 2);
                int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, localX, localZ) - 1;
                int index = cz * cells + cx;

                if (top < minY || top > maxY) {
                    heights[index] = EMPTY;
                    colors[index] = 0;
                    continue;
                }

                BlockPos pos = new BlockPos(chunkMinX + localX, top, chunkMinZ + localZ);
                BlockState state = chunk.getBlockState(pos);
                if (state.isAir()) {
                    heights[index] = EMPTY;
                    colors[index] = 0;
                    continue;
                }

                MapColor mapColor = state.getMapColor(level, pos);
                int rgb = mapColor == null ? 0x7F7F7F : mapColor.col;
                if (rgb == 0) {
                    rgb = 0x7F7F7F;
                }

                // Sombra suave por altura: da volumen sin necesitar iluminacion real.
                float heightFactor = 0.78f + 0.22f * Math.max(0.0f, Math.min(1.0f,
                        (top - minY) / (float) Math.max(1, maxY - minY)));
                int r = (int) (((rgb >> 16) & 0xFF) * heightFactor);
                int g = (int) (((rgb >> 8) & 0xFF) * heightFactor);
                int b = (int) ((rgb & 0xFF) * heightFactor);

                heights[index] = (short) top;
                colors[index] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        return new LodChunk(chunk.getPos().x(), chunk.getPos().z(), cellSize, cells, heights, colors);
    }

    public long estimatedBytes() {
        return 64L + heights.length * 2L + colors.length * 4L;
    }

    public boolean isEmpty() {
        for (short height : heights) {
            if (height != EMPTY) {
                return false;
            }
        }
        return true;
    }
}
