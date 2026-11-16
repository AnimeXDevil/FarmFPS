package com.farmfps.mixin;

import com.farmfps.client.ClientTicker;
import com.farmfps.core.FarmConfig;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Smooth lighting (ambient occlusion) is switched off while a farm is active. Only chunk meshes
 * built during that time use flat lighting; the world is not re-meshed, so there is no hitch.
 */
@Mixin(MinecraftClient.class)
public abstract class FlatLightMixin {

    @Inject(method = "isAmbientOcclusionEnabled()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private static void farmfps$flat(CallbackInfoReturnable<Boolean> cir) {
        if (ClientTicker.flatLight && FarmConfig.INSTANCE.enabled && FarmConfig.INSTANCE.flatLightNearFarms) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
