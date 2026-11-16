package com.farmfps.mixin;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block-centred sounds (pistons, dispensers, note blocks, water) come from world events and cost
 * a lot even before they reach the sound manager. Inside the active scope the same sound may
 * start only once per tick (or the configured limit elsewhere), and redstone-style sounds can
 * be dropped outright by the sound-manager hook.
 */
@Mixin(World.class)
public abstract class SoundEventMixin {

    @Inject(
        method = "playSoundAtBlockCenterClient(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void farmfps$limit(BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch,
                               boolean useDistance, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !((World) (Object) this).isClient()) {
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
