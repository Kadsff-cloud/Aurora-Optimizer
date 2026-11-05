package dev.aurora.optimizer.mixin;

import dev.aurora.optimizer.client.HookProbe;
import dev.aurora.optimizer.client.LodManager;
import dev.aurora.optimizer.client.MemoryGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Engancha la captura del LOD al descarte de chunks y limpia las caches al
 * desconectar. Es el punto donde una cache mal hecha retendria el mundo entero.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void aurora$onLevelCreated(CallbackInfo ci) {
        HookProbe.mark("nivel");
        LodManager.clear();
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void aurora$captureBeforeUnload(LevelChunk chunk, CallbackInfo ci) {
        LodManager.onChunkUnload((ClientLevel) (Object) this, chunk);
    }

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void aurora$clearOnDisconnect(Component reason, CallbackInfo ci) {
        MemoryGuard.onDisconnect(Minecraft.getInstance());
    }
}
