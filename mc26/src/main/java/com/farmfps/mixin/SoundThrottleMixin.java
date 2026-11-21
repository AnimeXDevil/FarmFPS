package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Per-tick limit for identical sounds, and muting of redstone-style sounds. */
@Mixin(SoundManager.class)
public abstract class SoundThrottleMixin {

    @Inject(
        method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$throttle(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return;
        }
        String path = sound.getIdentifier().getPath();
        if (c.muteAllRedstoneSounds && EffectCounters.isRedstoneSound(path)) {
            Stats.soundsMuted++;
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        int cx = ((int) Math.floor(sound.getX())) >> 4;
        int cz = ((int) Math.floor(sound.getZ())) >> 4;
        boolean farm = c.hideFarmEffects && FarmDetector.isActive(cx, cz);
        if (!farm && !c.throttleSounds) {
            return;
        }
        if (c.muteRedstoneSounds && FarmDetector.shouldAffect(cx, cz) && EffectCounters.isRedstoneNoise(path)) {
            Stats.soundsMuted++;
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        int limit = farm ? 1 : c.maxSameSoundPerTick;
        if (!EffectCounters.allowSound(sound.getIdentifier().hashCode(), limit)) {
            Stats.soundsMuted++;
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
