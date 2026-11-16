package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feeds the farm detector and hides particles: all of them inside a chunk detected as an active
 * farm (when "farm effects" is on), plus anything over the per-tick budget elsewhere.
 * Vanilla already returns null here when its own particle limit is hit, so callers handle null.
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
        int cx = ((int) Math.floor(x)) >> 4;
        int cz = ((int) Math.floor(z)) >> 4;
        FarmDetector.record(cx, cz, FarmDetector.WEIGHT_PARTICLE);
        if ((c.hideFarmEffects && FarmDetector.isActive(cx, cz))
            || (c.redstoneMode != 0 && c.redstoneScope == 1 && parameters instanceof DustParticleEffect)
            || (c.throttleParticles && !EffectCounters.allowParticle())) {
            Stats.particlesHidden++;
            cir.setReturnValue(null);
        }
    }
}
