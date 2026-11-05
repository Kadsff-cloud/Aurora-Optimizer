package dev.aurora.optimizer.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache LRU acotada. Es el corazon del arreglo de fugas de RAM del LOD: nunca
 * crece sin limite, y cada entrada es una instantanea de datos primitivos.
 */
public final class LodCache {
    private final LinkedHashMap<Long, LodChunk> chunks = new LinkedHashMap<>(256, 0.75f, true);
    private List<LodChunk> snapshot = List.of();
    private boolean dirty = true;
    private int maxChunks;
    private long estimatedBytes;

    public LodCache(int maxChunks) {
        this.maxChunks = Math.max(1, maxChunks);
    }

    public synchronized void setMaxChunks(int maxChunks) {
        this.maxChunks = Math.max(1, maxChunks);
        trim();
    }

    public synchronized void put(LodChunk chunk) {
        long key = key(chunk.chunkX, chunk.chunkZ);
        LodChunk previous = chunks.put(key, chunk);
        if (previous != null) {
            estimatedBytes -= previous.estimatedBytes();
        }
        estimatedBytes += chunk.estimatedBytes();
        dirty = true;
        trim();
    }

    public synchronized LodChunk get(int chunkX, int chunkZ) {
        return chunks.get(key(chunkX, chunkZ));
    }

    public synchronized boolean contains(int chunkX, int chunkZ) {
        return chunks.containsKey(key(chunkX, chunkZ));
    }

    public synchronized int size() {
        return chunks.size();
    }

    public synchronized long estimatedBytes() {
        return estimatedBytes;
    }

    public synchronized void clear() {
        chunks.clear();
        snapshot = List.of();
        estimatedBytes = 0;
        dirty = true;
    }

    public synchronized List<LodChunk> snapshot() {
        if (dirty) {
            snapshot = List.copyOf(chunks.values());
            dirty = false;
        }
        return snapshot;
    }

    public synchronized void trim() {
        while (chunks.size() > maxChunks) {
            Map.Entry<Long, LodChunk> eldest = chunks.entrySet().iterator().next();
            estimatedBytes -= eldest.getValue().estimatedBytes();
            chunks.remove(eldest.getKey());
            dirty = true;
        }
    }

    public synchronized void trimTo(int target) {
        while (chunks.size() > target) {
            Map.Entry<Long, LodChunk> eldest = chunks.entrySet().iterator().next();
            estimatedBytes -= eldest.getValue().estimatedBytes();
            chunks.remove(eldest.getKey());
            dirty = true;
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
