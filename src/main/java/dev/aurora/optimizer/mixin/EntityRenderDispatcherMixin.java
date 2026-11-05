package dev.aurora.optimizer.mixin;

import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraConfig;
import dev.aurora.optimizer.client.AuroraStats;
import dev.aurora.optimizer.client.HookProbe;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Culling por distancia para entidades que no son jefes ni jugadores.
 * Se aplica antes del frustum test, asi que ahorra extraccion y submit.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void aurora$onDispatcherCreated(CallbackInfo ci) {
        HookProbe.mark("entidades");
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void aurora$distanceCull(Entity entity, Frustum frustum, double x, double y, double z,
                                     CallbackInfoReturnable<Boolean> cir) {
        AuroraConfig config = Aurora.config();
        if (!config.entityCulling || entity instanceof Player) {
            return;
        }
        if (!config.cullBosses && (entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof Warden)) {
            return;
        }
        if (!config.cullNamedEntities && entity.hasCustomName()) {
            return;
        }

        double dx = entity.getX() - x;
        double dy = entity.getY() - y;
        double dz = entity.getZ() - z;
        double max = config.entityCullDistance;
        if (dx * dx + dy * dy + dz * dz > max * max) {
            AuroraStats.entitiesCulled++;
            cir.setReturnValue(false);
        }
    }
}
