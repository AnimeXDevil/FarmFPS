package com.farmfps.mixin;

import com.farmfps.client.VisualLock;
import com.farmfps.core.Stats;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skips the client light engine for redstone flips and piston moves flagged by VisualLock. */
@Mixin(LevelLightEngine.class)
public abstract class LightSuppressMixin {

    @Inject(method = "checkBlock(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$skipLight(BlockPos pos, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            Stats.seenLightChecks++;
            if (VisualLock.suppressLight) {
                Stats.lightSkipped++;
                ci.cancel();
            }
        }
    }
}
