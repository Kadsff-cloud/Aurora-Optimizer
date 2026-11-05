package dev.aurora.optimizer.client;

import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraConfig;
import dev.aurora.optimizer.addon.AuroraAddons;
import net.minecraft.client.Minecraft;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Locale;

/**
 * Guardia de memoria: mantiene acotadas las caches propias, limpia las del
 * cliente al desconectar y publica estadisticas utiles para diagnosticar.
 */
public final class MemoryGuard {
    private static int ticks;

    private MemoryGuard() {
    }

    public static void onClientTick(Minecraft client) {
        AuroraConfig config = Aurora.config();
        if (!config.leakGuard) {
            return;
        }
        if (++ticks < config.trimIntervalTicks) {
            return;
        }
        ticks = 0;
        trim(false);
    }

    public static void trim(boolean force) {
        long start = System.currentTimeMillis();
        LodManager.trim();
        AuroraAddons.trim();

        Minecraft client = Minecraft.getInstance();
        if (force && client != null && client.particleEngine != null) {
            client.particleEngine.clearParticles();
        }

        AuroraStats.lastTrimMillis = System.currentTimeMillis() - start;
        if (Aurora.config().lodDebug) {
            Aurora.LOGGER.info("Limpieza de memoria: LOD {} chunks / {} | heap {} | directo {}",
                    LodManager.size(), formatBytes(LodManager.bytes()), heapSummary(), directSummary());
        }
    }

    public static void onDisconnect(Minecraft client) {
        AuroraConfig config = Aurora.config();
        if (!config.clearCachesOnDisconnect) {
            return;
        }
        int lodChunks = LodManager.size();
        if (client != null && client.level != null) {
            LevelLeakGuard.onDisconnect(client, client.level);
        }
        LodManager.clear();
        if (client != null && client.particleEngine != null) {
            client.particleEngine.clearParticles();
        }
        ticks = 0;
        Aurora.LOGGER.info("Aurora: mundo descargado; cache LOD liberada ({} chunks)", lodChunks);
    }

    public static void onShutdown() {
        int lodChunks = LodManager.size();
        LodManager.clear();
        Aurora.LOGGER.info("Aurora: apagado; cache LOD liberada ({} chunks)", lodChunks);
    }

    public static String heapSummary() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return formatBytes(heap.getUsed()) + " / " + formatBytes(heap.getMax());
    }

    public static String directSummary() {
        long used = 0;
        List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        for (BufferPoolMXBean pool : pools) {
            if (pool.getName().toLowerCase().contains("direct")) {
                used += pool.getMemoryUsed();
            }
        }
        return formatBytes(used);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB"};
        int unit = -1;
        while (value >= 1024.0 && unit < units.length - 1) {
            value /= 1024.0;
            unit++;
        }
        // Punto decimal estable: el chat se localiza, las cifras no cambian de formato.
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }
}
