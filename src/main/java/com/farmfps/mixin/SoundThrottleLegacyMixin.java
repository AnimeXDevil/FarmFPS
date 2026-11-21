package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tied to the particle hiding: inside a detected farm chunk the same sound may play only once
 * per tick; elsewhere identical sounds are limited to the configured number per tick.
 * Minecraft 1.21.5 and older: SoundManager.play returns void (newer versions return a PlayResult).
 */
@Mixin(SoundManager.class)
public abstract class SoundThrottleLegacyMixin {

    @Inject(
        method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$throttle(SoundInstance sound, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return;
        }
        if (c.muteAllRedstoneSounds && EffectCounters.isRedstoneSound(sound.getId().getPath())) {
            Stats.soundsMuted++;
            ci.cancel();
            return;
        }
        boolean farm = c.hideFarmEffects
            && FarmDetector.isActive(((int) Math.floor(sound.getX())) >> 4, ((int) Math.floor(sound.getZ())) >> 4);
        if (!farm && !c.throttleSounds) {
            return;
        }
        if (c.muteRedstoneSounds
            && FarmDetector.shouldAffect(((int) Math.floor(sound.getX())) >> 4, ((int) Math.floor(sound.getZ())) >> 4)
            && EffectCounters.isRedstoneNoise(sound.getId().getPath())) {
            Stats.soundsMuted++;
            ci.cancel();
            return;
        }
        int limit = farm ? 1 : c.maxSameSoundPerTick;
        if (!EffectCounters.allowSound(sound.getId().hashCode(), limit)) {
            Stats.soundsMuted++;
            ci.cancel();
        }
    }
}
