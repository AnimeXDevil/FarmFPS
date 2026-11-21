package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.Stats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** "Hide moving blocks": the client finishes a piston move on its first tick (beyond 3 blocks from you). */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonFinishMixin {

    @Inject(
        method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void farmfps$finishAtOnce(Level world, BlockPos pos, BlockState state,
                                             PistonMovingBlockEntity blockEntity, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!world.isClientSide() || !c.enabled || !c.hideMovingBlocks) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            double dx = player.getX() - (pos.getX() + 0.5);
            double dy = player.getY() - pos.getY();
            double dz = player.getZ() - (pos.getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz < 9.0) {
                return;
            }
        }
        Stats.pistonFinished++;
        blockEntity.finalTick();
        ci.cancel();
    }
}
