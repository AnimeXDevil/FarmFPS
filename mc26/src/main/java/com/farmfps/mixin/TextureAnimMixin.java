package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Animated block textures (water, lava, fire...) are uploaded to the GPU less often near farms. */
@Mixin(TextureAtlas.class)
public abstract class TextureAnimMixin {
    private static int farmfps$counter;

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$throttle(CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return;
        }
        boolean farm = c.redstoneScope == 1 || FarmDetector.activeCount() > 0;
        int skip = farm ? c.textureAnimSkip : c.textureAnimSkipNormal;
        if (skip <= 0) {
            return;
        }
        if (++farmfps$counter > skip) {
            farmfps$counter = 0;
            return;
        }
        Stats.textureTicksSkipped++;
        ci.cancel();
    }
}
