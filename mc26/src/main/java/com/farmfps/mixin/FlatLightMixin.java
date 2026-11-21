package com.farmfps.mixin;

import com.farmfps.client.ClientTicker;
import com.farmfps.core.FarmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Near an active farm the "smooth lighting" option reads as off, so chunk meshes are built flat. */
@Mixin(OptionInstance.class)
public abstract class FlatLightMixin {

    @Inject(method = "get()Ljava/lang/Object;", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$flat(CallbackInfoReturnable<Object> cir) {
        if (!ClientTicker.flatLight || !FarmConfig.INSTANCE.enabled || !FarmConfig.INSTANCE.flatLightNearFarms) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null && (Object) this == mc.options.ambientOcclusion()) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
