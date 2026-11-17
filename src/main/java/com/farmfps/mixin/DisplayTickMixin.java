package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every tick vanilla picks hundreds of random blocks around you to spawn ambient particles
 * (torch smoke, drips, redstone sparks). Running that every second tick halves the cost
 * (about 2.6% of the render thread in a profile) with a barely visible drop in ambient particles.
 */
@Mixin(ClientWorld.class)
public abstract class DisplayTickMixin {
    private static boolean farmfps$skip;

    @Inject(method = "doRandomBlockDisplayTicks(III)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$halve(int centerX, int centerY, int centerZ, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !c.skipDisplayTicks) {
            return;
        }
        farmfps$skip = !farmfps$skip;
        if (farmfps$skip) {
            ci.cancel();
        }
    }
}
