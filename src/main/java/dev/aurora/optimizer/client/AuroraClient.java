package dev.aurora.optimizer.client;

import dev.aurora.optimizer.AuroraCompat;
import dev.aurora.optimizer.addon.AuroraAddons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public final class AuroraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AuroraCompat.logDetection();
        AuroraAddons.loadClient();

        ClientTickEvents.END_CLIENT_TICK.register(LodManager::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(WorldgenLodGenerator::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(MemoryGuard::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(FrameBench::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(AuroraAddons::tickClient);
        LevelRenderEvents.COLLECT_SUBMITS.register(LodRenderer::onCollectSubmits);
        LevelRenderEvents.COLLECT_SUBMITS.register(AuroraAddons::collectSubmits);
        LevelRenderEvents.START_MAIN.register(Extras::onStartMain);
        ClientCommandRegistrationCallback.EVENT.register(AuroraCommands::register);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MemoryGuard.onShutdown());
    }
}
