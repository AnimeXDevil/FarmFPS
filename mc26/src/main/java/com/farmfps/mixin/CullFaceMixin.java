package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Neighbour face fix: a hidden block must not hide the faces of the blocks next to it. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class CullFaceMixin {

    @Inject(
        method = "getFaceOcclusionShape(Lnet/minecraft/core/Direction;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void farmfps$emptyFace(Direction direction, CallbackInfoReturnable<VoxelShape> cir) {
        if (!HideManager.blocksActive || !FarmConfig.INSTANCE.neighborFaceFix) {
            return;
        }
        Object self = this;
        if (HideManager.isInvisible((BlockState) self)) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(
        method = "getOcclusionShape()Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void farmfps$emptyOcclusion(CallbackInfoReturnable<VoxelShape> cir) {
        if (!HideManager.blocksActive || !FarmConfig.INSTANCE.neighborFaceFix) {
            return;
        }
        Object self = this;
        if (HideManager.isInvisible((BlockState) self)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
