package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.PistonSnap;
import com.farmfps.core.Stats;
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
 * A moving piston searches the entity cache for things to push on every tick. With hundreds of
 * entities in a farm that search alone was ~9% of the render thread in a profile. The server is
 * authoritative for entity positions, so far away from you the client skips its own prediction.
 * Pistons within 6 blocks of the player still push normally.
 */
@Mixin(PistonBlockEntity.class)
public abstract class PistonPushMixin {

    @Inject(
        method = "pushEntities(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;FLnet/minecraft/block/entity/PistonBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void farmfps$skipFarPush(World world, BlockPos pos, float f, PistonBlockEntity be, CallbackInfo ci) {
        if (!world.isClient() || !FarmConfig.INSTANCE.enabled || FarmConfig.INSTANCE.pistonMode == 0) {
            return;
        }
        if (!PistonSnap.shouldSnap(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
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
