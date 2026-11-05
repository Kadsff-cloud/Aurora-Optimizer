package dev.aurora.optimizer.client;

/** Contadores ligeros; se leen y escriben en el hilo del cliente. */
public final class AuroraStats {
    public static long chunksCaptured;
    public static long lodQuads;
    public static long particlesCulled;
    public static long trackingEmittersCulled;
    public static long entitiesCulled;
    public static long lastTrimMillis;
    public static long worldgenChunks;
    public static long worldgenSamples;
    public static long worldgenNanos;
    public static long levelsCleared;
    public static long entitiesCleared;
    public static long chunksDropped;

    private AuroraStats() {
    }

    public static void reset() {
        chunksCaptured = 0;
        lodQuads = 0;
        particlesCulled = 0;
        trackingEmittersCulled = 0;
        entitiesCulled = 0;
        worldgenChunks = 0;
        worldgenSamples = 0;
        worldgenNanos = 0;
        levelsCleared = 0;
        entitiesCleared = 0;
        chunksDropped = 0;
    }
}
