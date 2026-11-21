package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hidden blocks cast no ambient-occlusion shadow on their neighbours. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AmbientOcclusionMixin {

    @Inject(
        method = "getShadeBrightness(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void farmfps$noShadow(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!HideManager.blocksActive) {
            return;
        }
        Object self = this;
        if (HideManager.isInvisible((BlockState) self)) {
            cir.setReturnValue(1.0F);
        }
    }
}
