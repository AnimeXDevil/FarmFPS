package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.PistonSnap;
import com.farmfps.core.Stats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Far from the player the client skips its own entity-push prediction for moving pistons. */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonPushMixin {

    @Inject(
        method = "moveCollidedEntities(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;FLnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void farmfps$skipFarPush(Level world, BlockPos pos, float f, PistonMovingBlockEntity be, CallbackInfo ci) {
        farmfps$maybeSkip(world, pos, ci);
    }

    @Inject(
        method = "moveStuckEntities(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;FLnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void farmfps$skipFarStuck(Level world, BlockPos pos, float f, PistonMovingBlockEntity be, CallbackInfo ci) {
        farmfps$maybeSkip(world, pos, ci);
    }

    private static void farmfps$maybeSkip(Level world, BlockPos pos, CallbackInfo ci) {
        if (!world.isClientSide() || !FarmConfig.INSTANCE.enabled || FarmConfig.INSTANCE.pistonMode == 0) {
            return;
        }
        if (!PistonSnap.shouldSnap(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            double dx = player.getX() - (pos.getX() + 0.5);
            double dy = player.getY() - pos.getY();
            double dz = player.getZ() - (pos.getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz < 36.0) {
                return;
            }
        }
        Stats.pistonPushSkipped++;
        ci.cancel();
    }
}
