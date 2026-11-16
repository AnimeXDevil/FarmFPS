package com.farmfps.mixin;

import com.farmfps.client.VisualLock;
import com.farmfps.core.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the light recalculation for block changes flagged by BlockUpdateLockMixin. The flag is only
 * set while the client thread applies such a change, and the thread check keeps the integrated
 * server's own lighting untouched.
 */
@Mixin(LightingProvider.class)
public abstract class LightSuppressMixin {

    @Inject(method = "checkBlock(Lnet/minecraft/util/math/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void farmfps$skipLight(BlockPos pos, CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) {
            Stats.seenLightChecks++;
        }
        if (VisualLock.suppressLight && MinecraftClient.getInstance().isOnThread()) {
            Stats.lightSkipped++;
            ci.cancel();
        }
    }
}
