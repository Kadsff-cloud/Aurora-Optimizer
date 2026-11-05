package dev.aurora.optimizer.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraCompat;
import dev.aurora.optimizer.AuroraConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

/**
 * /aurora ...: control en caliente sin reiniciar el cliente. Pensado para
 * comparar ajustes y ver el estado de las caches.
 *
 * Todos los textos visibles pasan por claves de traduccion (assets/aurora/lang)
 * para que el mismo jar funcione en espanol e ingles. Los mensajes de log se
 * mantienen estables porque los scripts de benchmark los parsean.
 */
public final class AuroraCommands {
    private AuroraCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(ClientCommands.literal("aurora")
                .executes(ctx -> status(ctx.getSource()))
                .then(ClientCommands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(ClientCommands.literal("lod")
                        .then(ClientCommands.literal("on").executes(ctx -> setLod(ctx.getSource(), true, "aurora")))
                        .then(ClientCommands.literal("off").executes(ctx -> setLod(ctx.getSource(), false, "off")))
                        .then(ClientCommands.literal("auto").executes(ctx -> setLod(ctx.getSource(), true, "auto")))
                        .then(ClientCommands.literal("voxy").executes(ctx -> setLod(ctx.getSource(), true, "voxy")))
                        .then(ClientCommands.literal("worldgen")
                                .then(ClientCommands.literal("on").executes(ctx -> setWorldgen(ctx.getSource(), true)))
                                .then(ClientCommands.literal("off").executes(ctx -> setWorldgen(ctx.getSource(), false)))))
                .then(ClientCommands.literal("particles")
                        .then(ClientCommands.argument("limit", IntegerArgumentType.integer(0, 20000))
                                .executes(ctx -> setParticles(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "limit")))))
                .then(ClientCommands.literal("entities")
                        .then(ClientCommands.argument("distance", IntegerArgumentType.integer(16, 512))
                                .executes(ctx -> setEntities(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "distance")))))
                .then(ClientCommands.literal("trim").executes(ctx -> {
                    MemoryGuard.trim(true);
                    ctx.getSource().sendFeedback(Component.translatable(
                            "aurora.command.trim", LodManager.size()));
                    return 1;
                }))
                .then(ClientCommands.literal("reload").executes(ctx -> {
                    Aurora.reloadConfig();
                    LodManager.applyConfig();
                    ctx.getSource().sendFeedback(Component.translatable("aurora.command.reload"));
                    return 1;
                }))
                .then(ClientCommands.literal("compat").executes(ctx -> compat(ctx.getSource())))
                .then(ClientCommands.literal("bench")
                        .executes(ctx -> bench(ctx.getSource(), 10))
                        .then(ClientCommands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                .executes(ctx -> bench(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds")))))
                .then(ClientCommands.literal("help").executes(ctx -> help(ctx.getSource()))));
    }

    private static int status(FabricClientCommandSource source) {
        AuroraConfig config = Aurora.config();
        source.sendFeedback(Component.translatable("aurora.status.header", AuroraConfig.VERSION));
        source.sendFeedback(Component.translatable("aurora.status.memory",
                MemoryGuard.heapSummary(), MemoryGuard.directSummary()));
        source.sendFeedback(Component.translatable("aurora.status.lod",
                active(config.lodActive()), config.lodMode, LodManager.size(),
                MemoryGuard.formatBytes(LodManager.bytes())));
        Minecraft minecraft = Minecraft.getInstance();
        source.sendFeedback(Component.translatable("aurora.status.fps",
                minecraft.getFps(),
                String.format(java.util.Locale.ROOT, "%.2f", minecraft.getFrameTimeNs() / 1_000_000.0)));
        source.sendFeedback(Component.translatable("aurora.status.quads",
                AuroraStats.lodQuads, AuroraStats.chunksCaptured));
        source.sendFeedback(Component.translatable("aurora.status.particles",
                AuroraStats.particlesCulled, AuroraStats.trackingEmittersCulled,
                AuroraStats.entitiesCulled));
        source.sendFeedback(Component.translatable("aurora.status.worldgen",
                state(config.lodWorldgen), AuroraStats.worldgenChunks,
                AuroraStats.worldgenSamples, worldgenMillisPerChunk()));
        source.sendFeedback(Component.translatable("aurora.status.leaks",
                state(config.leakGuard), AuroraStats.levelsCleared,
                AuroraStats.entitiesCleared, AuroraStats.chunksDropped));
        source.sendFeedback(Component.translatable("aurora.status.hooks", HookProbe.summary()));
        source.sendFeedback(Component.translatable("aurora.status.compat",
                AuroraCompat.isSodiumLoaded(), AuroraCompat.isVoxyLoaded(),
                AuroraCompat.isSodiumExtraLoaded()));
        Aurora.LOGGER.info("Estado /aurora: heap={} | directo={} | LOD={} chunks/{} | quads={} | worldgen={} chunks/{} muestras | leaks: niveles={}, entidades={}, chunks={} | hooks={}",
                MemoryGuard.heapSummary(), MemoryGuard.directSummary(), LodManager.size(),
                MemoryGuard.formatBytes(LodManager.bytes()), AuroraStats.lodQuads,
                AuroraStats.worldgenChunks, AuroraStats.worldgenSamples,
                AuroraStats.levelsCleared, AuroraStats.entitiesCleared, AuroraStats.chunksDropped,
                HookProbe.summaryString());
        return 1;
    }

    private static Component state(boolean on) {
        return Component.translatable(on ? "aurora.state.on" : "aurora.state.off");
    }

    private static Component active(boolean active) {
        return Component.translatable(active ? "aurora.state.active" : "aurora.state.inactive");
    }

    private static String worldgenMillisPerChunk() {
        if (AuroraStats.worldgenChunks <= 0) {
            return "0.00";
        }
        double millis = AuroraStats.worldgenNanos / 1_000_000.0 / AuroraStats.worldgenChunks;
        return String.format(java.util.Locale.ROOT, "%.2f", millis);
    }

    private static int setLod(FabricClientCommandSource source, boolean enabled, String mode) {
        AuroraConfig config = Aurora.config();
        config.lodEnabled = enabled;
        config.lodMode = mode;
        config.sanitize();
        config.save();
        LodManager.applyConfig();
        source.sendFeedback(Component.translatable("aurora.command.lod.set",
                active(config.lodActive()), mode));
        return 1;
    }

    private static int setParticles(FabricClientCommandSource source, int limit) {
        AuroraConfig config = Aurora.config();
        config.particleLimit = limit;
        config.reduceParticles = limit > 0;
        config.sanitize();
        config.save();
        source.sendFeedback(Component.translatable("aurora.command.particles.set",
                config.particleLimit));
        return 1;
    }

    private static int setEntities(FabricClientCommandSource source, int distance) {
        AuroraConfig config = Aurora.config();
        config.entityCullDistance = distance;
        config.entityCulling = true;
        config.sanitize();
        config.save();
        source.sendFeedback(Component.translatable("aurora.command.entities.set",
                config.entityCullDistance));
        return 1;
    }

    private static int setWorldgen(FabricClientCommandSource source, boolean enabled) {
        AuroraConfig config = Aurora.config();
        config.lodWorldgen = enabled;
        config.save();
        if (!enabled) {
            WorldgenLodGenerator.reset();
        }
        source.sendFeedback(Component.translatable("aurora.command.worldgen.set",
                state(enabled)));
        return 1;
    }

    private static int compat(FabricClientCommandSource source) {
        source.sendFeedback(Component.translatable("aurora.compat.header"));
        source.sendFeedback(Component.translatable("aurora.compat.mods",
                AuroraCompat.isSodiumLoaded(), AuroraCompat.isSodiumExtraLoaded(),
                AuroraCompat.isIrisLoaded(), AuroraCompat.isVoxyLoaded()));
        source.sendFeedback(Component.translatable("aurora.compat.bundle",
                AuroraCompat.bundleSummary()));
        source.sendFeedback(Component.translatable("aurora.compat.bundle_detail"));
        source.sendFeedback(Component.translatable("aurora.compat.cwb"));
        source.sendFeedback(Component.translatable("aurora.compat.lod_auto"));
        source.sendFeedback(Component.translatable("aurora.compat.sodium_profile"));
        source.sendFeedback(Component.translatable("aurora.compat.worldgen"));
        return 1;
    }

    private static int bench(FabricClientCommandSource source, int seconds) {
        if (!FrameBench.start(seconds)) {
            source.sendFeedback(Component.translatable("aurora.command.bench.running"));
            return 0;
        }
        source.sendFeedback(Component.translatable("aurora.command.bench.started", seconds));
        return 1;
    }

    private static int help(FabricClientCommandSource source) {
        source.sendFeedback(Component.translatable("aurora.help.header"));
        source.sendFeedback(Component.translatable("aurora.help.status"));
        source.sendFeedback(Component.translatable("aurora.help.lod"));
        source.sendFeedback(Component.translatable("aurora.help.worldgen"));
        source.sendFeedback(Component.translatable("aurora.help.particles"));
        source.sendFeedback(Component.translatable("aurora.help.entities"));
        source.sendFeedback(Component.translatable("aurora.help.trim"));
        source.sendFeedback(Component.translatable("aurora.help.reload"));
        source.sendFeedback(Component.translatable("aurora.help.compat"));
        source.sendFeedback(Component.translatable("aurora.help.bench"));
        return 1;
    }
}
