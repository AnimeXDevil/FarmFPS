package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.Stats;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Hide moving blocks": the client finishes a piston move on its first tick, so the pushed block
 * goes straight from its old position to its new one and is never drawn in between. This does not
 * depend on how the renderer reads the moving block. Pistons within 3 blocks of you are left alone
 * so riding and being pushed still feel right. The server is authoritative either way.
 */
@Mixin(PistonBlockEntity.class)
public abstract class PistonFinishMixin {

    @Inject(
        method = "tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/PistonBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void farmfps$finishAtOnce(World world, BlockPos pos, BlockState state,
                                             PistonBlockEntity blockEntity, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!world.isClient() || !c.enabled || !c.hideMovingBlocks) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            double dx = player.getX() - (pos.getX() + 0.5);
            double dy = player.getY() - pos.getY();
            double dz = player.getZ() - (pos.getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz < 9.0) {
                return;
            }
        }
        Stats.pistonFinished++;
        blockEntity.finish();
        ci.cancel();
    }
}
