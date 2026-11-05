package dev.aurora.optimizer.mixin;

import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraConfig;
import dev.aurora.optimizer.client.AuroraStats;
import dev.aurora.optimizer.client.HookProbe;
import dev.aurora.optimizer.client.ParticleBudget;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

/**
 * Presupuesto global de particulas. Evita que explosiones, granjas o mods
 * mal comportados llenen el heap con objetos Particle.
 *
 * <p>En 26.2 {@code setLevel} ya llama a {@code clearParticles()}, que vacia
 * grupos, cola pendiente, emisores y contadores. El problema real no es el
 * cambio de nivel: es que {@code add} encola en {@code particlesToAdd} sin
 * mirar esa cola, y {@code createTrackingEmitter} no tiene tope. Este mixin
 * cubre ambos huecos.</p>
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    @Final
    private Map<ParticleRenderType, ParticleGroup<?>> particles;

    @Shadow
    @Final
    private Queue<Particle> particlesToAdd;

    @Shadow
    @Final
    private Queue<TrackingEmitter> trackingEmitters;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aurora$onEngineCreated(CallbackInfo ci) {
        HookProbe.mark("particulas");
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void aurora$limitParticles(Particle particle, CallbackInfo ci) {
        AuroraConfig config = Aurora.config();
        if (!config.reduceParticles || config.particleLimit <= 0) {
            return;
        }

        int grouped = 0;
        for (ParticleGroup<?> group : particles.values()) {
            grouped += group.size();
        }
        int used = ParticleBudget.used(grouped, particlesToAdd.size(), trackingEmitters.size());
        if (ParticleBudget.isFull(used, config.particleLimit)) {
            AuroraStats.particlesCulled++;
            if (config.lodDebug && AuroraStats.particlesCulled % 1000 == 0) {
                Aurora.LOGGER.info("Limite de particulas alcanzado ({}), descartadas {} (grupos={}, cola={}, emisores={})",
                        config.particleLimit, AuroraStats.particlesCulled, grouped,
                        particlesToAdd.size(), trackingEmitters.size());
            }
            ci.cancel();
        }
    }

    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
            at = @At("HEAD"), cancellable = true)
    private void aurora$limitTrackingEmitter(Entity entity, ParticleOptions options, CallbackInfo ci) {
        aurora$limitTrackingEmitter(ci);
    }

    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
            at = @At("HEAD"), cancellable = true)
    private void aurora$limitTrackingEmitter(Entity entity, ParticleOptions options, int lifetime, CallbackInfo ci) {
        aurora$limitTrackingEmitter(ci);
    }

    private void aurora$limitTrackingEmitter(CallbackInfo ci) {
        AuroraConfig config = Aurora.config();
        if (!config.reduceParticles || config.trackingEmitterLimit <= 0) {
            return;
        }
        if (ParticleBudget.isEmitterFull(trackingEmitters.size(), config.trackingEmitterLimit)) {
            AuroraStats.trackingEmittersCulled++;
            if (config.lodDebug && AuroraStats.trackingEmittersCulled % 100 == 0) {
                Aurora.LOGGER.info("Limite de emisores de seguimiento alcanzado ({}), descartados {}",
                        config.trackingEmitterLimit, AuroraStats.trackingEmittersCulled);
            }
            ci.cancel();
        }
    }
}
