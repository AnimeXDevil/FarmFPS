package com.farmfps.mixin;

import com.farmfps.client.VisualLock;
import com.farmfps.core.Stats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the "no redraw" flag (and light suppression) to visual-only block changes on the client. */
@Mixin(Level.class)
public abstract class BlockUpdateLockMixin {

    @Inject(
        method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void farmfps$adjust(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
                                CallbackInfoReturnable<Boolean> cir) {
        if (VisualLock.reentry) {
            return;
        }
        Level self = (Level) (Object) this;
        if (!self.isClientSide()) {
            return;
        }
        Stats.seenBlockChanges++;
        int mask = VisualLock.decide(self, pos, state, flags);
        if (mask == 0) {
            return;
        }
        VisualLock.reentry = true;
        VisualLock.suppressLight = (mask & VisualLock.WANT_NO_LIGHT) != 0;
        try {
            int newFlags = (mask & VisualLock.WANT_NO_REDRAW) != 0 ? (flags | VisualLock.NO_REDRAW) : flags;
            cir.setReturnValue(self.setBlock(pos, state, newFlags, maxUpdateDepth));
        } finally {
            VisualLock.reentry = false;
            VisualLock.suppressLight = false;
        }
    }
}
