package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hidden blocks report an INVISIBLE render shape so the chunk mesher skips their model. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class RenderTypeMixin {

    @Inject(method = "getRenderShape()Lnet/minecraft/world/level/block/RenderShape;", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$invisible(CallbackInfoReturnable<RenderShape> cir) {
        if (!HideManager.blocksActive) {
            return;
        }
        Object self = this;
        BlockState state = (BlockState) self;
        Block block = state.getBlock();
        if (HideManager.isFilterHidden(block)) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        } else if (HideManager.isRedstoneHidden(block)
            && !(FarmConfig.INSTANCE.solidFaceFix && state.isSolidRender())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
