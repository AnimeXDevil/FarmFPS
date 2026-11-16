package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Farm mode: chunk meshing (vanilla and Sodium) only draws blocks whose render type is MODEL.
 * Reporting INVISIBLE for the hidden block types leaves them out of the mesh. Collision, state and
 * server logic are untouched; the blocks just are not drawn.
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class RenderTypeMixin {

    @Inject(method = "getRenderType()Lnet/minecraft/block/BlockRenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$invisible(CallbackInfoReturnable<BlockRenderType> cir) {
        if (!HideManager.blocksActive) {
            return;
        }
        Object self = this;
        BlockState state = (BlockState) self;
        Block block = state.getBlock();
        if (HideManager.isFilterHidden(block)) {
            cir.setReturnValue(BlockRenderType.INVISIBLE);
        } else if (HideManager.isRedstoneHidden(block)
            && !(FarmConfig.INSTANCE.solidFaceFix && state.isOpaque())) {
            // With the face fix on, full-size redstone blocks stay drawn so the faces of the blocks
            // next to them are not culled into see-through holes.
            cir.setReturnValue(BlockRenderType.INVISIBLE);
        }
    }
}
