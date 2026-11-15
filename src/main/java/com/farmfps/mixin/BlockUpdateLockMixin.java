package com.farmfps.mixin;

import com.farmfps.client.VisualLock;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Both BlockUpdateS2CPacket and ChunkDeltaUpdateS2CPacket end up in this one method, so a single
 * hook covers single-block and batched updates and keeps their order intact.
 */
@Mixin(ClientWorld.class)
public abstract class BlockUpdateLockMixin {

    @Inject(
        method = "handleBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$lock(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        if (VisualLock.intercept((ClientWorld) (Object) this, pos, state, flags)) {
            ci.cancel();
        }
    }
}
