package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a block that is drawn as invisible behave like redstone dust for face culling: its side
 * "culling face" is empty, so the neighbouring blocks draw their own face towards it instead of
 * leaving a see-through hole. Only the rendering-side culling shape is changed; light, collisions
 * and logic do not use it. Variant for versions where the method also takes the world and position.
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class CullFaceWorldMixin {

    @Inject(method = "getCullingFace(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$emptyCullingFace(BlockView world, BlockPos pos, Direction direction, CallbackInfoReturnable<VoxelShape> cir) {
        if (!HideManager.blocksActive) {
            return;
        }
        Object self = this;
        if (HideManager.isInvisible((BlockState) self)) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}
