package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tied to the particle hiding: inside a detected farm chunk the same sound may play only once
 * per tick; elsewhere identical sounds are limited to the configured number per tick.
 * In this Minecraft version SoundManager.play returns a PlayResult, not void.
 */
@Mixin(SoundManager.class)
public abstract class SoundThrottleMixin {

    @Inject(
        method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$throttle(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return;
        }
        boolean farm = c.hideFarmEffects
            && FarmDetector.isActive(((int) Math.floor(sound.getX())) >> 4, ((int) Math.floor(sound.getZ())) >> 4);
        if (!farm && !c.throttleSounds) {
            return;
        }
        int limit = farm ? 1 : c.maxSameSoundPerTick;
        if (!EffectCounters.allowSound(sound.getId().hashCode(), limit)) {
            Stats.soundsMuted++;
            cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        }
    }
}
