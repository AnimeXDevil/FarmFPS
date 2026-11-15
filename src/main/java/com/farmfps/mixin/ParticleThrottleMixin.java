package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Counts particles per chunk for the farm detector and drops particles once the per-tick budget is
 * spent. Vanilla already returns null here when its own particle limit is reached, so callers
 * handle a null result.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleThrottleMixin {

    @Inject(
        method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$throttle(ParticleEffect parameters, double x, double y, double z,
                                  double velocityX, double velocityY, double velocityZ,
                                  CallbackInfoReturnable<Particle> cir) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return;
        }
        FarmDetector.record(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4, FarmDetector.WEIGHT_PARTICLE);
        if (c.throttleParticles && !EffectCounters.allowParticle()) {
            cir.setReturnValue(null);
        }
    }
}
