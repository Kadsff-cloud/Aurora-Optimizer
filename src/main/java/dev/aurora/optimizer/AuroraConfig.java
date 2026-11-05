package dev.aurora.optimizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuracion plana y tolerante a errores. Se guarda en config/aurora.json.
 * Los valores por defecto estan pensados para funcionar sin tocar nada.
 */
public final class AuroraConfig {
    public static final String VERSION = "0.1.0";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- Memoria / fugas ---
    public boolean leakGuard = true;
    public int trimIntervalTicks = 1200;
    public int maxTintCacheChunks = 512;
    public boolean clearCachesOnDisconnect = true;

    // --- Renderizado tipo Sodium ---
    public boolean entityCulling = true;
    public int entityCullDistance = 96;
    public boolean cullBosses = false;
    public boolean cullNamedEntities = false;
    public int particleLimit = 4000;
    public boolean reduceParticles = true;
    public int trackingEmitterLimit = 256;

    // --- Extras tipo Sodium Extra ---
    public boolean cloudHeightOverride = false;
    public float cloudHeight = 192.0f;
    public boolean disableClouds = false;
    public boolean fogOverride = false;
    public float fogStart = 0.75f;
    public float fogEnd = 1.0f;

    // --- LOD lejano (Voxy integrado / Aurora LOD) ---
    public boolean lodEnabled = true;
    public String lodMode = "auto"; // auto | aurora | voxy | off
    public int lodMaxChunks = 4096;
    public int lodCellSize = 2; // bloques por celda (1, 2, 4 u 8)
    public int lodStartDistanceChunks = 8;
    public int lodMaxDistanceChunks = 96;
    public int lodMaxQuads = 12000;
    public boolean lodFog = true;
    public boolean lodDebug = false;
    // Genera terreno por delante del radio cargado en singleplayer usando el
    // ChunkGenerator del servidor integrado. No carga chunks completos.
    // Opt-in: la medicion conservadora (radio 18, lote 4, 12k quads) costo
    // -20,3% frente a Sodium-on; no se activa por defecto.
    public boolean lodWorldgen = false;
    public int lodWorldgenBatch = 4;
    public int lodWorldgenRadiusChunks = 18;

    // --- Compatibilidad ---
    public boolean sodiumCompat = true;
    public boolean voxyCompat = true;
    public boolean logCompat = true;

    private transient Path path;

    public static AuroraConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("aurora.json");
        AuroraConfig config = new AuroraConfig();
        config.path = path;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                AuroraConfig loaded = GSON.fromJson(reader, AuroraConfig.class);
                if (loaded != null) {
                    loaded.path = path;
                    loaded.sanitize();
                    return loaded;
                }
            } catch (Exception e) {
                Aurora.LOGGER.warn("No se pudo leer {}; se usan valores por defecto", path, e);
            }
        }

        config.sanitize();
        config.save();
        return config;
    }

    public void save() {
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve("aurora.json");
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Aurora.LOGGER.warn("No se pudo guardar {}", path, e);
        }
    }

    public Path path() {
        return path;
    }

    public boolean lodActive() {
        boolean auto = "auto".equalsIgnoreCase(lodMode);
        boolean sodium = auto && AuroraCompat.isSodiumLoaded();
        boolean voxy = auto && AuroraCompat.isVoxyLoaded();
        return lodActive(sodium, voxy);
    }

    /**
     * Version sin FabricLoader para tests y para razonar sobre los modos.
     * En auto se cede a Voxy (dibuja su propio LOD) y a Sodium (politica de
     * coste: el LOD si se dibuja, pero anade unos pocos FPS y en escenas con
     * colinas puede quedar casi oculto).
     */
    boolean lodActive(boolean sodiumLoaded, boolean voxyLoaded) {
        if (!lodEnabled || "off".equalsIgnoreCase(lodMode)) {
            return false;
        }
        if ("voxy".equalsIgnoreCase(lodMode)) {
            return false;
        }
        if ("auto".equalsIgnoreCase(lodMode)) {
            if (voxyCompat && voxyLoaded) {
                return false;
            }
            if (sodiumCompat && sodiumLoaded) {
                return false;
            }
        }
        return true;
    }

    public int cellSize() {
        int size = lodCellSize;
        if (size <= 1) return 1;
        if (size <= 2) return 2;
        if (size <= 4) return 4;
        return 8;
    }

    public int cellsPerChunk() {
        return 16 / cellSize();
    }

    public void sanitize() {
        trimIntervalTicks = clamp(trimIntervalTicks, 100, 72000);
        maxTintCacheChunks = clamp(maxTintCacheChunks, 32, 8192);
        entityCullDistance = clamp(entityCullDistance, 16, 512);
        particleLimit = clamp(particleLimit, 0, 20000);
        trackingEmitterLimit = clamp(trackingEmitterLimit, 0, 4096);
        cloudHeight = clamp(cloudHeight, -64.0f, 512.0f);
        fogStart = clamp(fogStart, 0.0f, 1.0f);
        fogEnd = clamp(fogEnd, 0.01f, 1.0f);
        if (fogEnd <= fogStart) {
            fogEnd = Math.min(1.0f, fogStart + 0.01f);
        }
        lodMaxChunks = clamp(lodMaxChunks, 64, 32768);
        lodStartDistanceChunks = clamp(lodStartDistanceChunks, 2, 64);
        lodMaxDistanceChunks = clamp(lodMaxDistanceChunks, lodStartDistanceChunks + 1, 512);
        lodMaxQuads = clamp(lodMaxQuads, 1000, 500000);
        lodWorldgenBatch = clamp(lodWorldgenBatch, 1, 8);
        lodWorldgenRadiusChunks = clamp(lodWorldgenRadiusChunks,
                lodStartDistanceChunks + 1, lodMaxDistanceChunks);
        if (!"auto".equalsIgnoreCase(lodMode)
                && !"aurora".equalsIgnoreCase(lodMode)
                && !"voxy".equalsIgnoreCase(lodMode)
                && !"off".equalsIgnoreCase(lodMode)) {
            lodMode = "auto";
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
