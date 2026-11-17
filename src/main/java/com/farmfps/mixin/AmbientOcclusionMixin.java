package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A block drawn as invisible must not darken its neighbours. Smooth lighting (vanilla and Sodium)
 * shades a face by the ambient-occlusion value of the blocks around it; for an invisible block
 * this reports "fully bright", which removes the dark patches next to hidden pistons / observers.
 * Only the render-side shading is changed: opacity, light propagation and collisions are untouched.
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AmbientOcclusionMixin {

    @Inject(
        method = "getAmbientOcclusionLightLevel(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void farmfps$noShadow(BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!HideManager.blocksActive) {
            return;
        }
        Object self = this;
        if (HideManager.isInvisible((BlockState) self)) {
            cir.setReturnValue(1.0F);
        }
    }
}
