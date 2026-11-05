package dev.aurora.optimizer.client;

import dev.aurora.optimizer.Aurora;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Benchmark ligero de FPS/frame time. No cambia nada del juego: solo muestrea
 * el estado actual para comparar ajustes o mods. Se ejecuta en el hilo cliente.
 */
public final class FrameBench {
    private static boolean running;
    private static int ticksLeft;
    private static int samples;
    private static double fpsSum;
    private static int fpsMin = Integer.MAX_VALUE;
    private static int fpsMax;

    private FrameBench() {
    }

    public static boolean start(int seconds) {
        if (running) {
            return false;
        }
        running = true;
        ticksLeft = Math.max(20, seconds * 20);
        samples = 0;
        fpsSum = 0;
        fpsMin = Integer.MAX_VALUE;
        fpsMax = 0;
        Minecraft client = Minecraft.getInstance();
        Aurora.LOGGER.info("Benchmark: inicio ({} s, renderDistance={}, {}x{}, vsync={})",
                seconds,
                client.options.getEffectiveRenderDistance(),
                client.getWindow().getWidth(),
                client.getWindow().getHeight(),
                client.options.enableVsync().get());
        return true;
    }

    public static void onClientTick(Minecraft client) {
        if (!running) {
            return;
        }

        int fps = client.getFps();
        if (fps > 0) {
            samples++;
            fpsSum += fps;
            fpsMin = Math.min(fpsMin, fps);
            fpsMax = Math.max(fpsMax, fps);
        }

        if (--ticksLeft <= 0) {
            finish(client);
        }
    }

    private static void finish(Minecraft client) {
        running = false;
        double avgFps = samples == 0 ? 0.0 : fpsSum / samples;
        // Derivado de la media de FPS: getFrameTimeNs() es una muestra
        // instantanea y puede no coincidir con la ventana de medida.
        double avgMs = avgFps > 0.0 ? 1000.0 / avgFps : 0.0;
        // El log se mantiene estable porque los scripts de benchmark lo parsean.
        String logMessage = String.format(Locale.ROOT,
                "Aurora bench: %.1f FPS medio (min %d, max %d) | %.2f ms/frame | %d muestras | heap %s | directo %s",
                avgFps,
                fpsMin == Integer.MAX_VALUE ? 0 : fpsMin,
                fpsMax,
                avgMs,
                samples,
                MemoryGuard.heapSummary(),
                MemoryGuard.directSummary());
        Aurora.LOGGER.info(logMessage);
        if (client.player != null) {
            client.player.sendSystemMessage(Component.translatable("aurora.bench.result",
                    String.format(Locale.ROOT, "%.1f", avgFps),
                    fpsMin == Integer.MAX_VALUE ? 0 : fpsMin,
                    fpsMax,
                    String.format(Locale.ROOT, "%.2f", avgMs),
                    samples,
                    MemoryGuard.heapSummary(),
                    MemoryGuard.directSummary()));
        }
    }

    public static boolean isRunning() {
        return running;
    }
}
