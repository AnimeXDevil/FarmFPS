package com.farmfps.mixin;

import com.farmfps.client.VisualLock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every client-side block change goes through World.setBlockState: server packets, batched chunk
 * deltas, and the client's own piston simulation. Hooking it here covers all of them.
 * When a change should skip its redraw, it is re-issued once with the no-redraw flag added;
 * the real state change still happens.
 */
@Mixin(World.class)
public abstract class BlockUpdateLockMixin {

    @Inject(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$adjust(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
                                CallbackInfoReturnable<Boolean> cir) {
        if (VisualLock.reentry) {
            return;
        }
        World self = (World) (Object) this;
        if (!self.isClient()) {
            return;
        }
        int mask = VisualLock.decide(self, pos, state, flags);
        if (mask == 0) {
            return;
        }
        VisualLock.reentry = true;
        VisualLock.suppressLight = (mask & VisualLock.WANT_NO_LIGHT) != 0;
        try {
            int newFlags = (mask & VisualLock.WANT_NO_REDRAW) != 0 ? (flags | VisualLock.NO_REDRAW) : flags;
            cir.setReturnValue(self.setBlockState(pos, state, newFlags, maxUpdateDepth));
        } finally {
            VisualLock.reentry = false;
            VisualLock.suppressLight = false;
        }
    }
}
