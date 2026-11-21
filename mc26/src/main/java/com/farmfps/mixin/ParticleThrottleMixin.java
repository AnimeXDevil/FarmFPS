package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feeds the farm detector and hides particles: all of them inside a chunk detected as an active
 * farm (when "farm effects" is on), plus anything over the per-tick budget elsewhere.
 * Vanilla already returns null here when its own particle limit is hit, so callers handle null.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleThrottleMixin {

    @Inject(
        method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$throttle(ParticleOptions parameters, double x, double y, double z,
                                  double velocityX, double velocityY, double velocityZ,
                                  CallbackInfoReturnable<Particle> cir) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return;
        }
        int cx = ((int) Math.floor(x)) >> 4;
        int cz = ((int) Math.floor(z)) >> 4;
        FarmDetector.record(cx, cz, FarmDetector.WEIGHT_PARTICLE);
        if ((c.hideFarmEffects && FarmDetector.isActive(cx, cz))
            || (c.redstoneMode != 0 && c.redstoneScope == 1 && parameters instanceof DustParticleOptions)
            || (c.throttleParticles && !EffectCounters.allowParticle())) {
            Stats.particlesHidden++;
            cir.setReturnValue(null);
        }
    }
}
