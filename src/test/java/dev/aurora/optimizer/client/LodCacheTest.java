package dev.aurora.optimizer.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LodCacheTest {
    private static LodChunk chunk(int x, int z) {
        return LodChunk.of(x, z, 2, 1, new short[]{64}, new int[]{0xFF00FF00});
    }

    @Test
    void neverExceedsMaxChunks() {
        LodCache cache = new LodCache(2);
        cache.put(chunk(0, 0));
        cache.put(chunk(1, 0));
        cache.put(chunk(2, 0));

        assertEquals(2, cache.size());
        assertNull(cache.get(0, 0), "la entrada mas antigua debe salir");
        assertNotNull(cache.get(1, 0));
        assertNotNull(cache.get(2, 0));
    }

    @Test
    void accessOrderKeepsHotChunks() {
        LodCache cache = new LodCache(2);
        cache.put(chunk(0, 0));
        cache.put(chunk(1, 0));
        assertNotNull(cache.get(0, 0));
        cache.put(chunk(2, 0));

        assertNotNull(cache.get(0, 0), "el chunk consultado debe seguir");
        assertNull(cache.get(1, 0), "el chunk no consultado debe salir");
        assertNotNull(cache.get(2, 0));
    }

    @Test
    void byteEstimateTracksEvictions() {
        LodCache cache = new LodCache(1);
        cache.put(chunk(0, 0));
        long one = cache.estimatedBytes();
        assertTrue(one > 0);
        cache.put(chunk(1, 0));

        assertEquals(1, cache.size());
        assertEquals(one, cache.estimatedBytes(), "al reemplazar, el tamano no debe acumularse");
    }

    @Test
    void trimToShrinksImmediately() {
        LodCache cache = new LodCache(8);
        for (int i = 0; i < 8; i++) {
            cache.put(chunk(i, 0));
        }
        cache.trimTo(3);

        assertEquals(3, cache.size());
        assertEquals(3, cache.snapshot().size());
    }

    @Test
    void clearDropsEverything() {
        LodCache cache = new LodCache(8);
        cache.put(chunk(0, 0));
        cache.clear();

        assertEquals(0, cache.size());
        assertEquals(0, cache.estimatedBytes());
        assertTrue(cache.snapshot().isEmpty());
        assertFalse(cache.contains(0, 0));
    }
}
