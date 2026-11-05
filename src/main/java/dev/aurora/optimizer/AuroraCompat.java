package dev.aurora.optimizer;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Deteccion suave de mods de rendimiento. No son dependencias obligatorias:
 * Aurora funciona solo, pero evita duplicar trabajo cuando Sodium o Voxy ya
 * estan haciendo el suyo.
 */
public final class AuroraCompat {
    private AuroraCompat() {
    }

    public static boolean isLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static boolean isSodiumLoaded() {
        return isLoaded("sodium") || isLoaded("sodium-fabric");
    }

    public static boolean isVoxyLoaded() {
        return isLoaded("voxy");
    }

    public static boolean isIrisLoaded() {
        return isLoaded("iris");
    }

    public static boolean isSodiumExtraLoaded() {
        return isLoaded("sodium-extra");
    }

    // --- Bundle unificado 26.2 ---
    public static boolean isLithiumLoaded() {
        return isLoaded("lithium");
    }

    public static boolean isFerriteCoreLoaded() {
        return isLoaded("ferritecore");
    }

    public static boolean isMoreCullingLoaded() {
        return isLoaded("moreculling");
    }

    public static boolean isClothConfigLoaded() {
        return isLoaded("cloth-config");
    }

    public static boolean isRailOptimizationLoaded() {
        return isLoaded("railoptimization");
    }

    public static boolean isImmediatelyFastLoaded() {
        return isLoaded("immediatelyfast");
    }

    public static boolean isBadOptimizationsLoaded() {
        return isLoaded("badoptimizations");
    }

    public static boolean isClumpsLoaded() {
        return isLoaded("clumps");
    }

    public static boolean isCubesWithoutBordersLoaded() {
        return isLoaded("cwb");
    }

    /** Resumen corto para /aurora compat y los logs. */
    public static String bundleSummary() {
        return "Lithium=" + isLithiumLoaded()
                + " | FerriteCore=" + isFerriteCoreLoaded()
                + " | MoreCulling=" + isMoreCullingLoaded()
                + " | ClothConfig=" + isClothConfigLoaded()
                + " | RailOptimization=" + isRailOptimizationLoaded()
                + " | ImmediatelyFast=" + isImmediatelyFastLoaded()
                + " | BadOptimizations=" + isBadOptimizationsLoaded()
                + " | Clumps=" + isClumpsLoaded()
                + " | CubesWithoutBorders=" + isCubesWithoutBordersLoaded();
    }

    public static void logDetection() {
        AuroraConfig config = Aurora.config();
        if (!config.logCompat) {
            return;
        }
        Aurora.LOGGER.info("Compatibilidad detectada -> Sodium: {}, Sodium Extra: {}, Iris: {}, Voxy: {}",
                isSodiumLoaded(), isSodiumExtraLoaded(), isIrisLoaded(), isVoxyLoaded());
        Aurora.LOGGER.info("Bundle 26.2 -> {}", bundleSummary());
        if (isLithiumLoaded()) {
            Aurora.LOGGER.info("Lithium presente: optimiza ticking, IA, fisica y allocations del servidor integrado.");
        }
        if (isFerriteCoreLoaded()) {
            Aurora.LOGGER.info("FerriteCore presente: reduce memoria de blockstates, modelos y datos de bloque.");
        }
        if (isMoreCullingLoaded()) {
            Aurora.LOGGER.info("MoreCulling presente: culling de bloques/marcos; Aurora mantiene su culling de entidades por distancia.");
            if (!isClothConfigLoaded()) {
                Aurora.LOGGER.warn("MoreCulling esta presente sin Cloth Config; su configuracion puede fallar.");
            }
        }
        if (isRailOptimizationLoaded()) {
            Aurora.LOGGER.info("Rail Optimization presente: railes powered/activator se actualizan antes.");
        }
        if (isImmediatelyFastLoaded()) {
            Aurora.LOGGER.info("ImmediatelyFast presente: acelera el envio de buffers de HUD/entidades sin tocar el terreno de Sodium.");
        }
        if (isBadOptimizationsLoaded()) {
            Aurora.LOGGER.info("BadOptimizations presente: microoptimizaciones de cliente (texturas, luces y allocations).");
        }
        if (isClumpsLoaded()) {
            Aurora.LOGGER.info("Clumps presente: agrupa orbes de XP en el servidor integrado; complementa el culling de entidades de Aurora.");
        }
        if (isCubesWithoutBordersLoaded()) {
            Aurora.LOGGER.info("Cubes Without Borders presente: pantalla completa sin bordes; no modifica el render de terreno ni promete FPS.");
        }
        if (isVoxyLoaded() && config.lodActive()) {
            Aurora.LOGGER.warn("Voxy esta instalado pero lodMode=aurora; se dibujaran dos LOD. Usa lodMode=auto para ceder el control a Voxy.");
        }
        if (isSodiumLoaded()) {
            Aurora.LOGGER.info("Sodium detectado: en lodMode=auto Aurora cede el LOD y deja el terreno a Sodium; usa lodMode=aurora para forzar el LOD propio.");
        }
    }
}
