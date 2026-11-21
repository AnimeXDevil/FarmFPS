package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Halves the random block display ticks (torch smoke, redstone dust sparks...). */
@Mixin(ClientLevel.class)
public abstract class DisplayTickMixin {
    private static boolean farmfps$skip;

    @Inject(method = "animateTick(III)V", at = @At("HEAD"), cancellable = true, require = 0)
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
