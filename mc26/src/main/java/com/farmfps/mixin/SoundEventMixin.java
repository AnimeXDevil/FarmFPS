package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block-centred sounds may start only once per tick inside farms (or the configured limit elsewhere). */
@Mixin(Level.class)
public abstract class SoundEventMixin {

    @Inject(
        method = "playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void farmfps$limit(BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch,
                               boolean useDistance, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !((Level) (Object) this).isClientSide()) {
            return;
        }
        boolean farm = c.hideFarmEffects && FarmDetector.isActive(pos.getX() >> 4, pos.getZ() >> 4);
        if (!farm && !c.throttleSounds) {
            return;
        }
        int limit = farm ? 1 : c.maxSameSoundPerTick;
        if (!EffectCounters.allowSound(sound.hashCode() * 31 + (pos.getX() >> 4) * 7 + (pos.getZ() >> 4), limit)) {
            Stats.soundsMuted++;
            ci.cancel();
        }
    }
}
