package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mutes repeated identical sounds within one client tick (dispensers, pistons, hoppers, etc). */
@Mixin(SoundManager.class)
public abstract class SoundThrottleMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void farmfps$throttle(SoundInstance sound, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (c.enabled && c.throttleSounds && !EffectCounters.allowSound(sound.getId().hashCode())) {
            ci.cancel();
        }
    }
}
